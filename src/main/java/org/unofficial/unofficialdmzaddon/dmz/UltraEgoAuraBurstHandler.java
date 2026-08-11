package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.network.AddonNetwork;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Ultra Ego's hit-reactive aura and transformation-scoped damage power growth. */
public final class UltraEgoAuraBurstHandler {
    private static final String DAMAGE_POWER_EFFECT = "unofficialdmzaddon_ultraego_damage_power";
    private final Set<UUID> activeUltraEgoPlayers = new HashSet<>();

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F || event.getEntity().level().isClientSide
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!DivineAuraHelper.isUltraEgo(data)) return;
            if (activeUltraEgoPlayers.add(player.getUUID())) data.getEffects().removeEffect(DAMAGE_POWER_EFFECT);
            AddonNetwork.sendUltraEgoBurst(player);
            if (!UnofficialDMZConfig.SPECIAL_FORM_BUFFS.get()) return;

            double cap = UnofficialDMZConfig.ULTRA_EGO_DAMAGE_POWER_CAP.get();
            double gain = UnofficialDMZConfig.ULTRA_EGO_DAMAGE_POWER_GAIN.get()
                    * event.getAmount() / Math.max(1.0F, player.getMaxHealth());
            double currentPower = data.getEffects().hasEffect(DAMAGE_POWER_EFFECT)
                    ? data.getEffects().getEffectPower(DAMAGE_POWER_EFFECT) : 1.0D;
            double nextBonus = Math.min(cap,
                    Math.max(0.0D, currentPower - 1.0D) + Math.max(0.0D, gain));
            data.getEffects().addEffect(DAMAGE_POWER_EFFECT, 1.0D + nextBonus, -1);
            sync(player);
        });
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) return;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            boolean active = player.isAlive() && data.getStatus().isAlive()
                    && DivineAuraHelper.isUltraEgo(data);
            if (active) {
                // A missing runtime marker means this is a fresh activation or server session.
                // Remove any persisted bonus before damage can begin building it again.
                if (activeUltraEgoPlayers.add(player.getUUID())
                        && data.getEffects().hasEffect(DAMAGE_POWER_EFFECT)) {
                    data.getEffects().removeEffect(DAMAGE_POWER_EFFECT);
                    sync(player);
                }
            } else {
                reset(player, data.getEffects().hasEffect(DAMAGE_POWER_EFFECT));
            }
        });
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) reset(player, true);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) reset(player, false);
    }

    private void reset(ServerPlayer player, boolean sendSync) {
        activeUltraEgoPlayers.remove(player.getUUID());
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!data.getEffects().hasEffect(DAMAGE_POWER_EFFECT)) return;
            data.getEffects().removeEffect(DAMAGE_POWER_EFFECT);
            if (sendSync) sync(player);
        });
    }

    private static void sync(ServerPlayer player) {
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }
}
