package org.unofficial.unofficialdmzaddon.dmz;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class UltraInstinctOmenCombatHandler {

    private static final int OMEN_DODGE_MESSAGE_CD = 30;
    private static final float BASE_DODGE_CHANCE = 0.20f;
    private static final float MAX_DODGE_CHANCE = 0.45f;
    private static final float BASE_STRIKE_CHANCE = 0.12f;
    private static final float MAX_STRIKE_CHANCE = 0.22f;
    private static final String DRAGONMINEZ_EVASION_SOUND = "dragonminez:evasion1";

    private final Map<UUID, Integer> dodgeMessageCooldown = new HashMap<>();
    private final SoundEvent fallbackEvasionSound = SoundEvents.ENDERMAN_TELEPORT;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0f) {
            return;
        }

        applyOmenStrikeBoost(event);
        if (event.isCanceled() || event.getAmount() <= 0.0f) {
            return;
        }

        applyOmenAutoDodge(event);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        dodgeMessageCooldown.remove(event.getEntity().getUUID());
    }

    private void applyOmenStrikeBoost(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim == attacker) {
            return;
        }

        DMZRuntimeAccess.getOmenState(attacker).ifPresent(state -> {
            double masteryRatio = masteryRatio(state.mastery());
            float procChance = lerp(BASE_STRIKE_CHANCE, MAX_STRIKE_CHANCE, masteryRatio);
            if (attacker.getRandom().nextFloat() > procChance) {
                return;
            }

            int kiCost = Math.max(1, (int) Math.ceil(state.maxEnergy() * 0.01));
            if (!state.consumeEnergy(kiCost)) {
                return;
            }

            float damageBoost = (float) lerp(1.12, 1.30, masteryRatio);
            event.setAmount(event.getAmount() * damageBoost);

            if (attacker.level() instanceof ServerLevel level) {
                level.sendParticles(
                        ParticleTypes.CRIT,
                        victim.getX(),
                        victim.getY() + (victim.getBbHeight() * 0.5),
                        victim.getZ(),
                        12,
                        0.2,
                        0.25,
                        0.2,
                        0.02
                );
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9f, 1.1f);
            }
        });
    }

    private void applyOmenAutoDodge(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker == victim) {
            return;
        }

        DMZRuntimeAccess.getOmenState(victim).ifPresent(state -> {
            double masteryRatio = masteryRatio(state.mastery());
            float dodgeChance = lerp(BASE_DODGE_CHANCE, MAX_DODGE_CHANCE, masteryRatio);
            if (victim.getRandom().nextFloat() > dodgeChance) {
                return;
            }

            int kiCost = Math.max(2, (int) Math.ceil(state.maxEnergy() * 0.02));
            if (!state.consumeEnergy(kiCost)) {
                return;
            }

            event.setCanceled(true);
            event.setAmount(0.0f);

            Vec3 direction = victim.position().subtract(attacker.position());
            if (direction.lengthSqr() < 1.0E-4) {
                direction = victim.getLookAngle().scale(-1.0);
            }
            direction = direction.normalize();

            double push = lerp(0.45, 0.70, masteryRatio);
            victim.push(direction.x * push, 0.12, direction.z * push);
            victim.hurtMarked = true;

            if (victim.level() instanceof ServerLevel level) {
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        victim.getX(),
                        victim.getY() + (victim.getBbHeight() * 0.5),
                        victim.getZ(),
                        16,
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
                        8,
                        0.15,
                        0.02,
                        0.15,
                        0.03
                );

                SoundEvent evasionSound = ForgeRegistries.SOUND_EVENTS.getValue(net.minecraft.resources.ResourceLocation.tryParse(DRAGONMINEZ_EVASION_SOUND));
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

            maybeSendDodgeMessage(victim);
        });
    }

    private void maybeSendDodgeMessage(ServerPlayer victim) {
        int nextAllowedTick = dodgeMessageCooldown.getOrDefault(victim.getUUID(), 0);
        if (victim.tickCount < nextAllowedTick) {
            return;
        }

        victim.displayClientMessage(Component.translatable("message.unofficialdmzaddon.ultra_instinct_omen.dodge"), true);
        dodgeMessageCooldown.put(victim.getUUID(), victim.tickCount + OMEN_DODGE_MESSAGE_CD);
    }

    private static float lerp(double min, double max, double ratio) {
        return (float) (min + (max - min) * ratio);
    }

    private static double masteryRatio(double mastery) {
        return Math.max(0.0, Math.min(1.0, mastery / 100.0));
    }
}
