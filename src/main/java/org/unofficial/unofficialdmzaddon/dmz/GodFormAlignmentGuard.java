package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Final server-side guard for commands or third-party packets that bypass the normal selection path. */
public final class GodFormAlignmentGuard {
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) return;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!data.getCharacter().hasActiveForm()) return;
            String group = data.getCharacter().getActiveFormGroup();
            String form = data.getCharacter().getActiveForm();
            if (!GodFormAlignment.isAllowed(data, group, form)) {
                data.getCharacter().clearActiveForm(player);
            }
        });
    }
}
