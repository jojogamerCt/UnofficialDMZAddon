package org.unofficial.unofficialdmzaddon.technique;

import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
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
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.dmz.AddonTechniqueInstaller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative choreography for the addon's canon techniques. */
public final class CanonTechniqueHandler {
    private static final Map<UUID, HellzoneSequence> HELLZONES = new HashMap<>();
    private static final Map<UUID, StrikeSequence> STRIKES = new HashMap<>();

    public static boolean executeCustomKi(LivingEntity owner, Level level, KiAttackData attack, StatsData stats, float charge) {
        if (!UnofficialDMZConfig.CANON_TECHNIQUE_BEHAVIOR.get() || level.isClientSide || charge < .5F) return false;
        String id = attack.getId();
        if (!AddonTechniqueInstaller.TRI_BEAM.equals(id) && !AddonTechniqueInstaller.HELLZONE_GRENADE.equals(id)) return false;

        level.getEntitiesOfClass(AbstractKiProjectile.class, owner.getBoundingBox().inflate(18), ki -> ki.getOwner() == owner && !ki.isFiring())
                .forEach(Entity::discard);
        float realDamage = (float)(stats.getKiDamage() * attack.getDamageMultiplier()
                * attack.getConfiguredDamageMultiplier() * Mth.clamp(charge, .5F, 2F) * attack.getOutputMultiplier());
        if (AddonTechniqueInstaller.TRI_BEAM.equals(id)) {
            if (owner instanceof ServerPlayer player) play(player, "addon.tri_beam_fire");
            Vec3 origin = owner.getEyePosition().add(owner.getLookAngle().scale(.75D));
            TriBeamEntity beam = new TriBeamEntity(AddonEntities.TRI_BEAM.get(), level);
            beam.setup(owner, origin, owner.getLookAngle().normalize().scale(Math.max(1.8F, attack.getActualSpeed() * 1.55F)), realDamage);
            level.addFreshEntity(beam);
            level.playSound(null, owner.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.55F);
        } else {
            if (owner instanceof ServerPlayer player) play(player, "addon.hellzone_grenade_fire");
            launchHellzone(owner, (ServerLevel)level, attack, realDamage);
        }
        return true;
    }

    private static void launchHellzone(LivingEntity owner, ServerLevel level, KiAttackData attack, float damage) {
        HellzoneSequence previous = HELLZONES.remove(owner.getUUID());
        if (previous != null) previous.orbs.forEach(id -> discard(level, id));
        HitResult sight = owner.pick(48D, 1F, false);
        Vec3 center = sight.getType() == HitResult.Type.MISS
                ? owner.getEyePosition().add(owner.getLookAngle().scale(24D)) : sight.getLocation().add(0, 1.2D, 0);
        int count = UnofficialDMZConfig.HELLZONE_SPHERE_COUNT.get();
        int delay = UnofficialDMZConfig.HELLZONE_GATHER_DELAY_TICKS.get();
        double radius = UnofficialDMZConfig.HELLZONE_SCATTER_RADIUS.get();
        List<UUID> ids = new ArrayList<>();
        double golden = Math.PI * (3D - Math.sqrt(5D));
        for (int i = 0; i < count; i++) {
            double y = 1D - (2D * (i + .5D) / count);
            double radial = Math.sqrt(Math.max(0D, 1D - y * y));
            double angle = i * golden;
            Vec3 offset = new Vec3(Math.cos(angle) * radial * radius, y * radius * .72D, Math.sin(angle) * radial * radius);
            HellzoneOrbEntity orb = new HellzoneOrbEntity(AddonEntities.HELLZONE_ORB.get(), level);
            orb.setup(center.add(offset), center, delay);
            level.addFreshEntity(orb); ids.add(orb.getUUID());
        }
        int converge = Math.max(8, (int)Math.ceil(radius / 1.15D) + 4);
        HELLZONES.put(owner.getUUID(), new HellzoneSequence(owner.getUUID(), level.dimension().location().toString(), center, ids,
                level.getGameTime() + delay + converge, damage, attack.getColorInterior(), attack.getColorExterior(), attack.getColorOutline()));
        level.playSound(null, owner.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.35F);
    }

    public static boolean requestCustomStrike(ServerPlayer player, int preferredTargetId) {
        if (!UnofficialDMZConfig.CANON_TECHNIQUE_BEHAVIOR.get()) return false;
        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null || !stats.getStatus().isHasCreatedCharacter() || stats.getStatus().isStunned() || STRIKES.containsKey(player.getUUID())) return false;
        TechniqueData selected = stats.getTechniques().getSelectedTechnique();
        if (!(selected instanceof StrikeAttackData strike) || !isCustomStrike(strike.getId())) return false;
        if (stats.getSkills().getSkillLevel("kicontrol") <= 0 || stats.getResources().getPowerRelease() < 5 || !player.getMainHandItem().isEmpty()) return true;
        String cooldown = "TechniqueCooldown_" + strike.getId();
        if (stats.getCooldowns().hasCooldown(cooldown)) return true;
        double cost = strike.getCalculatedCost(stats);
        if (stats.getResources().getCurrentEnergy() < cost) return true;
        LivingEntity target = findTarget(player, preferredTargetId);
        if (target == null) return true;

        stats.getResources().removeEnergy((float)Math.ceil(cost));
        stats.getCooldowns().setCooldown(cooldown, strike.getActualCooldown());
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        faceAndPlace(player, target);
        float damage = (float)(stats.getStrikeDamage() * strike.getActualDamageMultiplier());
        EnergyBladeEntity blade = null;
        if (AddonTechniqueInstaller.SPIRIT_SWORD_RUSH.equals(strike.getId())) {
            blade = new EnergyBladeEntity(AddonEntities.ENERGY_BLADE.get(), player.level());
            blade.setup(player, strike.getDurationTicks()); player.level().addFreshEntity(blade);
        }
        play(player, strike.getAnimationId());
        STRIKES.put(player.getUUID(), new StrikeSequence(target.getUUID(), strike.getId(), strike.getDurationTicks(), 0, damage,
                blade == null ? null : blade.getUUID()));
        return true;
    }

    private static boolean isCustomStrike(String id) {
        return AddonTechniqueInstaller.JAN_KEN_FIST.equals(id) || AddonTechniqueInstaller.SPIRIT_SWORD_RUSH.equals(id)
                || AddonTechniqueInstaller.SADISTIC_18.equals(id);
    }

    private static LivingEntity findTarget(ServerPlayer player, int preferred) {
        double range = UnofficialDMZConfig.CUSTOM_STRIKE_TARGET_RANGE.get();
        Entity direct = preferred >= 0 ? TargetHelper.getEntityOrPart(player.level(), preferred) : null;
        direct = direct == null ? null : TargetHelper.resolveHittable(direct);
        if (direct instanceof LivingEntity living && living.isAlive() && TargetHelper.canAttack(player, living, range)
                && player.distanceToSqr(living) <= range * range) return living;
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), target -> target != player && target.isAlive()
                        && TargetHelper.canAttack(player, target, range)
                        && target.getEyePosition().subtract(player.getEyePosition()).normalize().dot(look) > .35D)
                .stream().min((a,b) -> Double.compare(player.distanceToSqr(a), player.distanceToSqr(b))).orElse(null);
    }

    private static void faceAndPlace(ServerPlayer player, LivingEntity target) {
        Vec3 away = player.position().subtract(target.position());
        if (away.lengthSqr() < .01D) away = player.getLookAngle().scale(-1);
        Vec3 position = target.position().add(away.normalize().scale(1.65D));
        player.teleportTo(position.x, position.y, position.z);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        player.setDeltaMovement(Vec3.ZERO);
    }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
        long time = event.getServer().overworld().getGameTime();
        HELLZONES.entrySet().removeIf(entry -> {
            HellzoneSequence sequence = entry.getValue();
            ServerLevel level = null;
            for (ServerLevel candidate : event.getServer().getAllLevels()) {
                if (candidate.dimension().location().toString().equals(sequence.dimension)) { level = candidate; break; }
            }
            if (level == null || level.getGameTime() < sequence.explodeAt) return false;
            explodeHellzone(level, sequence); return true;
        });
        STRIKES.entrySet().removeIf(entry -> processStrike(event.getServer().getPlayerList().getPlayer(entry.getKey()), entry.getValue()));
    }

    private static boolean processStrike(ServerPlayer attacker, StrikeSequence sequence) {
        if (attacker == null || !attacker.isAlive() || !(attacker.serverLevel().getEntity(sequence.target) instanceof LivingEntity target) || !target.isAlive()) {
            if (attacker != null) stop(attacker); return true;
        }
        int tick = ++sequence.tick;
        int[] hitTicks; float[] weights;
        if (AddonTechniqueInstaller.JAN_KEN_FIST.equals(sequence.id)) { hitTicks = new int[]{5,12,20}; weights = new float[]{.3F,.3F,.4F}; }
        else if (AddonTechniqueInstaller.SPIRIT_SWORD_RUSH.equals(sequence.id)) { hitTicks = new int[]{5,10,16,23,31}; weights = new float[]{.14F,.14F,.16F,.2F,.36F}; }
        else { hitTicks = new int[]{5,10,15,23,31}; weights = new float[]{.12F,.12F,.14F,.25F,.37F}; }
        for (int i=0;i<hitTicks.length;i++) if (tick == hitTicks[i]) strikeHit(attacker,target,sequence,i,weights[i]);
        if (tick >= sequence.duration) { stop(attacker); return true; }
        attacker.setDeltaMovement(Vec3.ZERO);
        return false;
    }

    private static void strikeHit(ServerPlayer attacker, LivingEntity target, StrikeSequence sequence, int index, float weight) {
        target.hurt(MainDamageTypes.strikeAttack(attacker.level(), attacker, sequence.id), sequence.damage * weight);
        TargetHelper.onSuccessfulAttack(attacker, target, TargetHelper.getRelation(attacker,target));
        ((ServerLevel)attacker.level()).sendParticles(index + 1 >= 3 ? ParticleTypes.CRIT : ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY()+target.getBbHeight()*.55D, target.getZ(), 8, .35,.35,.35,.08);
        attacker.level().playSound(null,target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,SoundSource.PLAYERS,1.0F,.8F+index*.08F);
        if (AddonTechniqueInstaller.SADISTIC_18.equals(sequence.id)) {
            if (index < 3) target.setDeltaMovement(0,.28D + index*.06D,0);
            else if (index == 3) target.setDeltaMovement(0,-1.45D,0);
            else { target.setDeltaMovement(target.position().subtract(attacker.position()).normalize().scale(1.1D)); spawnExplosion((ServerLevel)attacker.level(),target.position(),0xFFF18A,0xF6A72E,0xFFF7CE,2.2F); }
        } else if (AddonTechniqueInstaller.JAN_KEN_FIST.equals(sequence.id) && index == 2) {
            target.setDeltaMovement(target.position().subtract(attacker.position()).normalize().scale(.75D).add(0,.18D,0));
        } else target.setDeltaMovement(Vec3.ZERO);
    }

    private static void explodeHellzone(ServerLevel level, HellzoneSequence sequence) {
        sequence.orbs.forEach(id -> discard(level,id));
        spawnExplosion(level, sequence.center, sequence.main, sequence.border, sequence.outline, 4.2F);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(sequence.owner);
        if (owner != null) for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(sequence.center,sequence.center).inflate(4.5D),
                target -> target != owner && target.isAlive() && TargetHelper.canAttack(owner,target,8D))) {
            target.hurt(MainDamageTypes.kiblast(level, owner, owner), sequence.damage);
            TargetHelper.onSuccessfulAttack(owner,target,TargetHelper.getRelation(owner,target));
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,sequence.center.x,sequence.center.y,sequence.center.z,2,0,0,0,0);
        level.playSound(null,sequence.center.x,sequence.center.y,sequence.center.z,SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,2F,.85F);
    }

    private static void spawnExplosion(ServerLevel level, Vec3 center, int main, int border, int outline, float size) {
        KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(),level);
        visual.setPos(center.x,center.y,center.z); visual.setupExplosion(main,border,outline,size); level.addFreshEntity(visual);
    }
    private static void discard(ServerLevel level, UUID id) { Entity entity=level.getEntity(id); if(entity!=null) entity.discard(); }
    private static void play(ServerPlayer player,String animation) { NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(),TriggerAnimationS2C.AnimationType.KI_ANIMATION,0,-1,animation),player); }
    private static void stop(ServerPlayer player) { NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(),TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP,0,-1,""),player); }

    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        HELLZONES.remove(event.getEntity().getUUID()); STRIKES.remove(event.getEntity().getUUID());
    }

    private record HellzoneSequence(UUID owner,String dimension,Vec3 center,List<UUID> orbs,long explodeAt,float damage,int main,int border,int outline) {}
    private static final class StrikeSequence {
        final UUID target; final String id; final int duration; int tick; final float damage; final UUID blade;
        StrikeSequence(UUID target,String id,int duration,int tick,float damage,UUID blade){this.target=target;this.id=id;this.duration=duration;this.tick=tick;this.damage=damage;this.blade=blade;}
    }
}
