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

public final class UltraInstinctOmenCombatHandler {

    private static final String DRAGONMINEZ_EVASION_SOUND = "dragonminez:evasion1";

    private final SoundEvent fallbackEvasionSound = SoundEvents.ENDERMAN_TELEPORT;

    /**
     * Intercepts the attack before any hurt logic runs (no hurt animation, no invulnerability
     * frames, no damage numbers).  This mirrors how DMZ cancels hits for Ki Barriers via
     * {@code LivingAttackEvent}.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(LivingAttackEvent event) {
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

            float dodgeChance = lerp(
                    lerp(0.20, 0.34, tierRatio),
                    lerp(0.45, 0.68, tierRatio),
                    masteryRatio
            );
            if (victim.getRandom().nextFloat() > dodgeChance) {
                return;
            }

            double kiCostRatio = lerp(0.020, 0.032, tierRatio);
            int kiCost = Math.max(2, (int) Math.ceil(state.maxEnergy() * kiCostRatio));
            if (!state.consumeEnergy(kiCost)) {
                return;
            }

            // Cancel the entire attack – no hurt animation, no invulnerability, no damage.
            event.setCanceled(true);

            Vec3 direction = victim.position().subtract(attacker.position());
            if (direction.lengthSqr() < 1.0E-4) {
                direction = victim.getLookAngle().scale(-1.0);
            }
            direction = direction.normalize();

            double push = lerp(
                    lerp(0.45, 0.70, tierRatio),
                    lerp(0.70, 0.95, tierRatio),
                    masteryRatio
            );
            victim.push(direction.x * push, 0.12, direction.z * push);
            victim.hurtMarked = true;

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

    private static float lerp(double min, double max, double ratio) {
        return (float) (min + (max - min) * ratio);
    }

    private static double masteryRatio(double mastery) {
        return Math.max(0.0, Math.min(1.0, mastery / 100.0));
    }

    private static double tierRatio(int tier) {
        return Math.max(0.0, Math.min(1.0, (tier - 1) / 3.0));
    }
}
