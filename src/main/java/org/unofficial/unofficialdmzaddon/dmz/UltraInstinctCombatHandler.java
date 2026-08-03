package org.unofficial.unofficialdmzaddon.dmz;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.unofficial.unofficialdmzaddon.network.AddonNetwork;

public final class UltraInstinctCombatHandler {

    private static final String DRAGONMINEZ_EVASION_SOUND = "dragonminez:evasion1";

    private final SoundEvent fallbackEvasionSound = SoundEvents.ENDERMAN_TELEPORT;

    /**
     * Intercepts the attack before any hurt logic runs (no hurt animation, no invulnerability
     * frames, no damage numbers).  This mirrors how DMZ cancels hits for Ki Barriers via
     * {@code LivingAttackEvent}.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (!org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.UI_DODGES_ENABLED.get()) return;
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker == victim) {
            return;
        }

        DMZRuntimeAccess.getUltraInstinctState(victim).ifPresent(state -> {
            double masteryRatio = masteryRatio(state.mastery());
            double tierRatio = tierRatio(state.tier());
            DodgeProfile profile = dodgeProfile(state.tier(), masteryRatio);

            if (victim.getRandom().nextFloat() >= profile.chance()) {
                return;
            }

            int kiCost = Math.max(1, (int) Math.ceil(state.maxEnergy() * profile.energyCostRatio()));
            if (!state.consumeEnergy(kiCost)) {
                return;
            }

            // LivingAttackEvent is the earliest cancellable damage event. Stopping it here
            // guarantees no health loss, hurt animation, invulnerability frames, or damage numbers.
            event.setCanceled(true);

            Vec3 direction = victim.position().subtract(attacker.position());
            if (direction.lengthSqr() < 1.0E-4) {
                direction = victim.getLookAngle().scale(-1.0);
            }
            direction = direction.normalize();

            // Set authoritative velocity instead of adding knockback. Player input can no longer
            // erase the dodge before the server synchronizes it to the client.
            Vec3 currentMovement = victim.getDeltaMovement();
            victim.setDeltaMovement(
                    direction.x * profile.distance(),
                    Math.max(currentMovement.y, profile.verticalLift()),
                    direction.z * profile.distance()
            );
            victim.hurtMarked = true;
            boolean leanRight = victim.getPersistentData().getBoolean("unofficialdmzaddon:ui_dodge_side");
            victim.getPersistentData().putBoolean("unofficialdmzaddon:ui_dodge_side", !leanRight);
            AddonNetwork.sendDodge(victim, leanRight);

            if (victim.level() instanceof ServerLevel level) {
                int endRodCount = (int) Math.round(16 + (10 * tierRatio));
                int cloudCount = (int) Math.round(8 + (6 * tierRatio));
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        victim.getX(),
                        victim.getY() + (victim.getBbHeight() * 0.5),
                        victim.getZ(),
                        endRodCount,
                        0.35,
                        0.45,
                        0.35,
                        0.01
                );
                level.sendParticles(
                        ParticleTypes.CLOUD,
                        victim.getX(),
                        victim.getY() + 0.1,
                        victim.getZ(),
                        cloudCount,
                        0.15,
                        0.02,
                        0.15,
                        0.03
                );

                SoundEvent evasionSound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.tryParse(DRAGONMINEZ_EVASION_SOUND));
                level.playSound(
                        null,
                        victim.getX(),
                        victim.getY(),
                        victim.getZ(),
                        evasionSound != null ? evasionSound : fallbackEvasionSound,
                        SoundSource.PLAYERS,
                        1.0f,
                        1.05f + victim.getRandom().nextFloat() * 0.15f
                );
            }

        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0f) {
            return;
        }

        applyUltraInstinctStrikeBoost(event);
    }

    private void applyUltraInstinctStrikeBoost(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim == attacker) {
            return;
        }

        DMZRuntimeAccess.getUltraInstinctState(attacker).ifPresent(state -> {
            double masteryRatio = masteryRatio(state.mastery());
            double tierRatio = tierRatio(state.tier());

            float procChance = lerp(
                    lerp(0.10, 0.20, tierRatio),
                    lerp(0.22, 0.35, tierRatio),
                    masteryRatio
            );
            if (attacker.getRandom().nextFloat() > procChance) {
                return;
            }

            float damageBoost = lerp(
                    lerp(1.12, 1.28, tierRatio),
                    lerp(1.30, 1.55, tierRatio),
                    masteryRatio
            );
            event.setAmount(event.getAmount() * damageBoost);

            if (attacker.level() instanceof ServerLevel level) {
                int critParticles = (int) Math.round(12 + (8 * tierRatio));
                level.sendParticles(
                        ParticleTypes.CRIT,
                        victim.getX(),
                        victim.getY() + (victim.getBbHeight() * 0.5),
                        victim.getZ(),
                        critParticles,
                        0.2,
                        0.25,
                        0.2,
                        0.02
                );
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9f, 1.1f);
            }
        });
    }

    private static DodgeProfile dodgeProfile(int tier, double masteryRatio) {
        double chanceAtZeroMastery;
        double chanceAtFullMastery;
        double distanceAtZeroMastery;
        double distanceAtFullMastery;
        double verticalLift;
        double energyCostAtZeroMastery;
        double energyCostAtFullMastery;

        switch (Math.max(1, Math.min(4, tier))) {
            case 1 -> {
                chanceAtZeroMastery = 0.50;
                chanceAtFullMastery = 0.70;
                distanceAtZeroMastery = 0.80;
                distanceAtFullMastery = 1.05;
                verticalLift = 0.12;
                energyCostAtZeroMastery = 0.012;
                energyCostAtFullMastery = 0.008;
            }
            case 2 -> {
                chanceAtZeroMastery = 0.65;
                chanceAtFullMastery = 0.82;
                distanceAtZeroMastery = 1.00;
                distanceAtFullMastery = 1.30;
                verticalLift = 0.15;
                energyCostAtZeroMastery = 0.010;
                energyCostAtFullMastery = 0.006;
            }
            default -> {
                chanceAtZeroMastery = 0.88;
                chanceAtFullMastery = 0.97;
                distanceAtZeroMastery = 1.50;
                distanceAtFullMastery = 1.90;
                verticalLift = 0.22;
                energyCostAtZeroMastery = 0.0075;
                energyCostAtFullMastery = 0.004;
            }
        }

        return new DodgeProfile(
                lerp(chanceAtZeroMastery, chanceAtFullMastery, masteryRatio),
                lerp(distanceAtZeroMastery, distanceAtFullMastery, masteryRatio),
                verticalLift,
                lerp(energyCostAtZeroMastery, energyCostAtFullMastery, masteryRatio)
        );
    }

    private record DodgeProfile(float chance, float distance, double verticalLift, float energyCostRatio) {
    }

    private static float lerp(double min, double max, double ratio) {
        return (float) (min + (max - min) * ratio);
    }

    private static double masteryRatio(double mastery) {
        return Math.max(0.0, Math.min(1.0, mastery / 100.0));
    }

    private static double tierRatio(int tier) {
        return Math.max(0.0, Math.min(1.0, (tier - 1) / 2.0));
    }
}
