package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Alien Adaptive Physiology: repeated exposure to one damage type builds temporary resistance. */
public final class AlienRacialPassiveHandler {
    private static final long ADAPTATION_WINDOW_TICKS = 120L;
    private static final int MAX_ADAPTATION = 5;
    private static final float RESISTANCE_PER_STACK = 0.04F;
    private final Map<UUID, Adaptation> adaptations = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onHurt(LivingHurtEvent event) {
        if (!org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.ALIEN_RACIAL_PASSIVE.get()
                || event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!SpecialRaceFormsDefinitions.ALIEN_RACE.equalsIgnoreCase(data.getCharacter().getRaceName())) {
                adaptations.remove(player.getUUID());
                return;
            }
            String damageType = event.getSource().getMsgId();
            long now = player.serverLevel().getGameTime();
            Adaptation previous = adaptations.get(player.getUUID());
            int stacks = previous != null && previous.damageType().equals(damageType)
                    && now <= previous.expiresAt() ? Math.min(MAX_ADAPTATION, previous.stacks() + 1) : 1;
            float reduction = Math.min(0.20F, (stacks - 1) * RESISTANCE_PER_STACK);
            if (reduction > 0.0F) event.setAmount(event.getAmount() * (1.0F - reduction));
            adaptations.put(player.getUUID(), new Adaptation(damageType, stacks, now + ADAPTATION_WINDOW_TICKS));
        });
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        adaptations.remove(event.getEntity().getUUID());
    }

    private record Adaptation(String damageType, int stacks, long expiresAt) {}
}