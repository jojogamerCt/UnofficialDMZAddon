package org.unofficial.unofficialdmzaddon.technique;

import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.unofficial.unofficialdmzaddon.dmz.AddonTechniqueInstaller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CanonTechniqueHandler {
    private static final int HELLZONE_ORB_COUNT = 14;
    private static final int HELLZONE_SPAWN_INTERVAL = 2;
    private static final double HELLZONE_SCATTER_RADIUS = 6.0D;
    private static final double HELLZONE_STAGE_SPEED = 0.95D;
    private static final int HELLZONE_CONVERGE_TICKS = 10;
    private static final int HELLZONE_MAIN = 0xFFE55C;
    private static final int HELLZONE_BORDER = 0xF5B52E;
    private static final int HELLZONE_OUTLINE = 0xFFF4B0;
    private static final double TRI_RANGE = 96.0D;
    private static final double TRI_TRAVEL_PER_TICK = 6.0D;
    private static final Map<UUID, HellzoneSequence> HELLZONES = new HashMap<>();
    private static final Map<UUID, TriBeamSequence> TRI_BEAMS = new HashMap<>();

    public static boolean executeCustomKi(LivingEntity owner, Level level, KiAttackData attack, StatsData stats, float charge) {
        if (level.isClientSide) return false;
        String id = attack.getId();
        if (AddonTechniqueInstaller.HELLZONE_GRENADE.equals(id)) {
            if (charge < 0.5F) {
                beginHellzoneCharge(owner, (ServerLevel) level);
                return false;
            }
            discardNativeChargingProjectile(owner, level);
            float damage = resolveDamage(stats, attack, charge);
            if (owner instanceof ServerPlayer player) play(player, "addon.hellzone_grenade_fire");
            releaseHellzone(owner, (ServerLevel) level, attack, damage);
            return true;
        }
        if (AddonTechniqueInstaller.TRI_BEAM.equals(id)) {
            if (charge < 0.5F) return false;
            discardNativeChargingProjectile(owner, level);
            float damage = resolveDamage(stats, attack, charge);
            if (owner instanceof ServerPlayer player) play(player, "addon.tri_beam_fire");
            beginTriBeam(owner, (ServerLevel) level, attack, damage);
            return true;
        }
        return false;
    }

    private static float resolveDamage(StatsData stats, KiAttackData attack, float charge) {
        return (float) (stats.getKiDamage() * attack.getDamageMultiplier()
                * attack.getConfiguredDamageMultiplier() * Mth.clamp(charge, 0.5F, 2.0F)
                * attack.getOutputMultiplier());
    }

    private static void discardNativeChargingProjectile(LivingEntity owner, Level level) {
        level.getEntitiesOfClass(AbstractKiProjectile.class, owner.getBoundingBox().inflate(30.0D), projectile ->
                        projectile.getOwner() == owner && !projectile.isFiring())
                .forEach(Entity::discard);
    }

    private static void beginTriBeam(LivingEntity owner, ServerLevel level, KiAttackData attack, float damage) {
        TriBeamSequence old = TRI_BEAMS.remove(owner.getUUID());
        if (old != null) old.finished = true;
        Vec3 direction = owner.getLookAngle().normalize();
        Vec3 origin = owner.getEyePosition().add(direction.scale(0.70D));
        HitResult blockSight = owner.pick(TRI_RANGE, 1.0F, false);
        Vec3 blockEnd = blockSight.getType() == HitResult.Type.MISS ? origin.add(direction.scale(TRI_RANGE)) : blockSight.getLocation();
        double maxDistance = Math.max(0.25D, origin.distanceTo(blockEnd));
        TRI_BEAMS.put(owner.getUUID(), new TriBeamSequence(owner.getUUID(), level, origin, direction, maxDistance,
                damage, attack.getColorInterior(), attack.getColorExterior(), attack.getColorOutline()));
        level.playSound(null, owner.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.55F);
    }

    private static void tickTriBeam(ServerPlayer player, TriBeamSequence sequence) {
        if (sequence.finished) return;
        double previousDistance = sequence.distance;
        double nextDistance = Math.min(sequence.maxDistance, previousDistance + TRI_TRAVEL_PER_TICK);
        TriBeamHit hit = findTriBeamHit(player, sequence, previousDistance, nextDistance);
        if (hit != null) {
            nextDistance = hit.distance;
            sequence.directTarget = hit.target.getUUID();
        }
        sequence.distance = nextDistance;
        Vec3 head = sequence.origin.add(sequence.direction.scale(nextDistance));
        renderTriangularBeam(sequence.level, sequence.origin, head, sequence.direction, sequence.maxDistance);
        if (hit != null || nextDistance >= sequence.maxDistance - 0.01D) {
            sequence.impact = head;
            explodeTriBeam(sequence, player);
            sequence.finished = true;
            TRI_BEAMS.remove(player.getUUID());
        }
    }

    private static TriBeamHit findTriBeamHit(ServerPlayer owner, TriBeamSequence sequence, double segmentStart, double segmentEnd) {
        Vec3 start = sequence.origin.add(sequence.direction.scale(segmentStart));
        Vec3 end = sequence.origin.add(sequence.direction.scale(segmentEnd));
        AABB search = new AABB(start, end).inflate(beamHitRadius(sequence.maxDistance, segmentEnd) + 2.5D);
        LivingEntity bestTarget = null;
        double bestDistance = segmentEnd + 1.0D;
        List<LivingEntity> targets = sequence.level.getEntitiesOfClass(LivingEntity.class, search,
                target -> target != owner && target.isAlive() && TargetHelper.canAttack(owner, target, TRI_RANGE));
        for (LivingEntity target : targets) {
            double halfWidth = Math.max(0.15D, target.getBbWidth() * 0.5D);
            double height = Math.max(0.2D, target.getBbHeight());
            Vec3 base = target.position();
            double candidateDistance = Double.POSITIVE_INFINITY;
            for (int i = 0; i <= 6; i++) {
                Vec3 sample = base.add(0.0D, height * (i / 6.0D), 0.0D);
                double along = sample.subtract(sequence.origin).dot(sequence.direction);
                double clampedAlong = clamp(along, segmentStart, segmentEnd);
                Vec3 closest = sequence.origin.add(sequence.direction.scale(clampedAlong));
                double allowed = beamHitRadius(sequence.maxDistance, clampedAlong) + halfWidth;
                if (sample.distanceToSqr(closest) <= allowed * allowed) {
                    candidateDistance = Math.min(candidateDistance, Math.max(segmentStart, clampedAlong - halfWidth));
                }
            }
            if (candidateDistance < bestDistance) {
                bestDistance = candidateDistance;
                bestTarget = target;
            }
        }
        return bestTarget == null ? null : new TriBeamHit(bestTarget, bestDistance);
    }

    private static double beamHitRadius(double maxDistance, double distance) {
        double t = clamp(distance / Math.max(1.0D, maxDistance), 0.0D, 1.0D);
        return 0.35D + 1.50D * t;
    }

    private static void renderTriangularBeam(ServerLevel level, Vec3 origin, Vec3 head, Vec3 direction, double maxDistance) {
        double length = origin.distanceTo(head);
        if (length < 0.05D) return;
        Vec3 helper = Math.abs(direction.y) < 0.92D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 u = direction.cross(helper).normalize();
        Vec3 v = direction.cross(u).normalize();
        int samples = Math.max(2, Math.min(48, (int) Math.ceil(length / 1.25D)));
        for (int s = 0; s <= samples; s++) {
            double travelled = length * (s / (double) samples);
            double globalT = clamp(travelled / Math.max(1.0D, maxDistance), 0.0D, 1.0D);
            Vec3 center = origin.add(direction.scale(travelled));
            double radius = 0.18D + 1.35D * globalT;
            for (int corner = 0; corner < 3; corner++) {
                double angle = -Math.PI * 0.5D + corner * Math.PI * 2.0D / 3.0D;
                Vec3 vertex = center.add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius));
                level.sendParticles(MainParticles.KI_TRAIL.get(), vertex.x, vertex.y, vertex.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            level.sendParticles(MainParticles.KI_SHEDDING.get(), center.x, center.y, center.z,
                    1, radius * 0.05D, radius * 0.05D, radius * 0.05D, 0.0D);
            if ((s & 1) == 0) level.sendParticles(MainParticles.KI_LIGHTNING.get(), center.x, center.y, center.z,
                    1, radius * 0.04D, radius * 0.04D, radius * 0.04D, 0.0D);
        }
    }

    private static void explodeTriBeam(TriBeamSequence sequence, ServerPlayer owner) {
        ServerLevel level = sequence.level;
        Entity direct = sequence.directTarget != null ? level.getEntity(sequence.directTarget) : null;
        if (direct instanceof LivingEntity living && living.isAlive() && TargetHelper.canAttack(owner, living, TRI_RANGE)) {
            if (living.hurt(MainDamageTypes.kiblast(level, owner, owner), sequence.damage)) {
                TargetHelper.onSuccessfulAttack(owner, living, TargetHelper.getRelation(owner, living));
            }
        }
        float splashDamage = sequence.damage * 0.35F;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(sequence.impact, sequence.impact).inflate(3.25D), target ->
                        target != owner && target != direct && target.isAlive() && TargetHelper.canAttack(owner, target, 5.0D))) {
            if (target.hurt(MainDamageTypes.kiblast(level, owner, owner), splashDamage)) {
                TargetHelper.onSuccessfulAttack(owner, target, TargetHelper.getRelation(owner, target));
            }
        }
        spawnExplosion(level, sequence.impact, sequence.main, sequence.border, sequence.outline, 3.1F);
        level.sendParticles(MainParticles.KI_EXPLOSION_FLASH.get(), sequence.impact.x, sequence.impact.y, sequence.impact.z,
                3, 0.18D, 0.18D, 0.18D, 0.0D);
        level.sendParticles(MainParticles.KI_EXPLOSION_SPLASH.get(), sequence.impact.x, sequence.impact.y, sequence.impact.z,
                14, 0.85D, 0.85D, 0.85D, 0.03D);
        level.sendParticles(MainParticles.KI_SHEDDING.get(), sequence.impact.x, sequence.impact.y, sequence.impact.z,
                18, 0.75D, 0.75D, 0.75D, 0.04D);
        level.playSound(null, sequence.impact.x, sequence.impact.y, sequence.impact.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6F, 1.10F);
    }

    private static void beginHellzoneCharge(LivingEntity owner, ServerLevel level) {
        HellzoneSequence previous = HELLZONES.remove(owner.getUUID());
        if (previous != null) discardOrbs(previous);
        HitResult sight = owner.pick(48.0D, 1.0F, false);
        Vec3 center = sight.getType() == HitResult.Type.MISS
                ? owner.getEyePosition().add(owner.getLookAngle().scale(24.0D))
                : sight.getLocation().add(0.0D, 1.0D, 0.0D);
        HELLZONES.put(owner.getUUID(), new HellzoneSequence(owner.getUUID(), level, center, level.getGameTime()));
    }

    private static void spawnNextHellzoneOrb(ServerPlayer player, HellzoneSequence sequence) {
        if (sequence.spawned >= HELLZONE_ORB_COUNT) return;
        int i = sequence.spawned++;
        Vec3 staging = stagingPoint(sequence.center, i, HELLZONE_ORB_COUNT);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        if (side.lengthSqr() > 1.0E-6D) side = side.normalize().scale((i & 1) == 0 ? 0.42D : -0.42D);
        else side = Vec3.ZERO;
        Vec3 start = player.getEyePosition().add(look.scale(0.55D)).add(side).add(0.0D, -0.35D, 0.0D);
        KiBlastEntity orb = createHellzoneOrb(player, sequence.level, start);
        sequence.orbs.add(new HellzoneOrbState(orb.getUUID(), staging));
        sequence.level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 0.55F, 1.45F + (i % 3) * 0.08F);
    }

    private static KiBlastEntity createHellzoneOrb(ServerPlayer player, ServerLevel level, Vec3 position) {
        KiBlastEntity orb = new KiBlastEntity(level, player);
        orb.setOwner(player);
        orb.setKiRenderType(0);
        orb.setSize(0.62F);
        orb.setKiSpeed(0.0F);
        orb.setKiDamage(0.0F);
        orb.setColors(HELLZONE_MAIN, HELLZONE_BORDER, HELLZONE_OUTLINE);
        orb.setCastTime(0);
        orb.setMaxLife(99999);
        orb.setArmorPenetration(0);
        orb.setHeal(false);
        orb.setFiring(true);
        orb.setPos(position.x, position.y, position.z);
        orb.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(orb);
        return orb;
    }

    private static Vec3 stagingPoint(Vec3 center, int index, int count) {
        double goldenAngle = Math.PI * (3.0D - Math.sqrt(5.0D));
        double y = 1.0D - (2.0D * (index + 0.5D) / count);
        double radial = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
        double angle = index * goldenAngle;
        return center.add(Math.cos(angle) * radial * HELLZONE_SCATTER_RADIUS,
                y * HELLZONE_SCATTER_RADIUS * 0.72D,
                Math.sin(angle) * radial * HELLZONE_SCATTER_RADIUS);
    }

    private static void releaseHellzone(LivingEntity owner, ServerLevel level, KiAttackData attack, float damage) {
        HellzoneSequence sequence = HELLZONES.get(owner.getUUID());
        if (sequence == null || sequence.level != level) {
            beginHellzoneCharge(owner, level);
            sequence = HELLZONES.get(owner.getUUID());
        }
        if (sequence == null || !(owner instanceof ServerPlayer player)) return;
        while (sequence.spawned < HELLZONE_ORB_COUNT) spawnNextHellzoneOrb(player, sequence);
        sequence.gathering = true;
        sequence.gatherStartedAt = level.getGameTime();
        sequence.explodeAt = sequence.gatherStartedAt + HELLZONE_CONVERGE_TICKS;
        sequence.damage = damage;
        sequence.main = attack.getColorInterior();
        sequence.border = attack.getColorExterior();
        sequence.outline = attack.getColorOutline();
        level.playSound(null, sequence.center.x, sequence.center.y, sequence.center.z,
                SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.3F, 1.5F);
    }

    private static void tickHellzone(ServerPlayer player, HellzoneSequence sequence) {
        if (sequence.gathering) {
            double remainingTicks = Math.max(1.0D, sequence.explodeAt - sequence.level.getGameTime());
            for (HellzoneOrbState state : sequence.orbs) {
                Entity entity = sequence.level.getEntity(state.entityId);
                if (entity instanceof KiBlastEntity orb) {
                    Vec3 current = orb.position();
                    Vec3 delta = sequence.center.subtract(current);
                    orb.setPos(current.add(delta.scale(1.0D / remainingTicks)));
                    orb.setDeltaMovement(Vec3.ZERO);
                    orb.setKiDamage(0.0F);
                }
            }
            if (sequence.level.getGameTime() >= sequence.explodeAt) {
                explodeHellzone(sequence);
                HELLZONES.remove(player.getUUID());
            }
            return;
        }
        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        boolean charging = stats != null
                && AddonTechniqueInstaller.HELLZONE_GRENADE.equals(stats.getTechniques().getChargingTechniqueId())
                && stats.getTechniques().isTechniqueCharging();
        if (!charging) {
            discardOrbs(sequence);
            HELLZONES.remove(player.getUUID());
            return;
        }
        long elapsed = sequence.level.getGameTime() - sequence.startedAt;
        int allowed = Math.min(HELLZONE_ORB_COUNT, 1 + (int) (elapsed / HELLZONE_SPAWN_INTERVAL));
        while (sequence.spawned < allowed) spawnNextHellzoneOrb(player, sequence);
        for (HellzoneOrbState state : sequence.orbs) {
            Entity entity = sequence.level.getEntity(state.entityId);
            if (!(entity instanceof KiBlastEntity orb)) continue;
            Vec3 current = orb.position();
            Vec3 delta = state.staging.subtract(current);
            if (delta.lengthSqr() <= HELLZONE_STAGE_SPEED * HELLZONE_STAGE_SPEED) orb.setPos(state.staging);
            else orb.setPos(current.add(delta.normalize().scale(HELLZONE_STAGE_SPEED)));
            orb.setDeltaMovement(Vec3.ZERO);
            orb.setKiDamage(0.0F);
        }
    }

    private static void explodeHellzone(HellzoneSequence sequence) {
        discardOrbs(sequence);
        ServerLevel level = sequence.level;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(sequence.owner);
        spawnExplosion(level, sequence.center, sequence.main, sequence.border, sequence.outline, 4.5F);
        if (owner != null) {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(sequence.center, sequence.center).inflate(4.75D),
                    target -> target != owner && target.isAlive() && TargetHelper.canAttack(owner, target, 8.0D))) {
                if (target.hurt(MainDamageTypes.kiblast(level, owner, owner), sequence.damage)) {
                    TargetHelper.onSuccessfulAttack(owner, target, TargetHelper.getRelation(owner, target));
                }
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, sequence.center.x, sequence.center.y, sequence.center.z,
                2, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, sequence.center.x, sequence.center.y, sequence.center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.85F);
    }

    private static void spawnExplosion(ServerLevel level, Vec3 center, int main, int border, int outline, float size) {
        KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), level);
        visual.setPos(center.x, center.y, center.z);
        visual.setupExplosion(main, border, outline, size);
        level.addFreshEntity(visual);
    }

    private static void discardOrbs(HellzoneSequence sequence) {
        for (HellzoneOrbState state : sequence.orbs) {
            Entity entity = sequence.level.getEntity(state.entityId);
            if (entity != null) entity.discard();
        }
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        HellzoneSequence hellzone = HELLZONES.get(player.getUUID());
        if (hellzone != null) tickHellzone(player, hellzone);
        TriBeamSequence triBeam = TRI_BEAMS.get(player.getUUID());
        if (triBeam != null) tickTriBeam(player, triBeam);
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        HellzoneSequence hellzone = HELLZONES.remove(id);
        if (hellzone != null) discardOrbs(hellzone);
        TriBeamSequence tri = TRI_BEAMS.remove(id);
        if (tri != null) tri.finished = true;
    }

    private static void play(ServerPlayer player, String animation) {
        NetworkHandler.sendToTrackingEntityAndSelf(
                new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, animation), player);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class HellzoneSequence {
        final UUID owner;
        final ServerLevel level;
        final Vec3 center;
        final long startedAt;
        final List<HellzoneOrbState> orbs = new ArrayList<>();
        int spawned;
        boolean gathering;
        long gatherStartedAt;
        long explodeAt;
        float damage;
        int main;
        int border;
        int outline;
        HellzoneSequence(UUID owner, ServerLevel level, Vec3 center, long startedAt) {
            this.owner = owner;
            this.level = level;
            this.center = center;
            this.startedAt = startedAt;
        }
    }

    private static final class HellzoneOrbState {
        final UUID entityId;
        final Vec3 staging;
        HellzoneOrbState(UUID entityId, Vec3 staging) {
            this.entityId = entityId;
            this.staging = staging;
        }
    }

    private static final class TriBeamSequence {
        final UUID owner;
        final ServerLevel level;
        final Vec3 origin;
        final Vec3 direction;
        final double maxDistance;
        final float damage;
        final int main;
        final int border;
        final int outline;
        double distance;
        Vec3 impact;
        UUID directTarget;
        boolean finished;
        TriBeamSequence(UUID owner, ServerLevel level, Vec3 origin, Vec3 direction, double maxDistance,
                        float damage, int main, int border, int outline) {
            this.owner = owner;
            this.level = level;
            this.origin = origin;
            this.direction = direction;
            this.maxDistance = maxDistance;
            this.damage = damage;
            this.main = main;
            this.border = border;
            this.outline = outline;
            this.impact = origin.add(direction.scale(maxDistance));
        }
    }

    private record TriBeamHit(LivingEntity target, double distance) {}
}
