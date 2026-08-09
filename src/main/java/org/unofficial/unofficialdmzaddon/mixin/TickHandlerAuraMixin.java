package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.TickHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = TickHandler.class, remap = false)
public abstract class TickHandlerAuraMixin {
    @ModifyVariable(method = "updateAuraLight", at = @At("STORE"), ordinal = 0)
    private static boolean unofficialdmzaddon$forceGodAuraLight(boolean original,
            ServerPlayer player, StatsData data) {
        // Client aura preferences are player-local. Do not force a server light merely
        // because a God form is active; native charging and Aura Status still illuminate.
        return data.getStatus().isAuraActive() || data.getStatus().isPermanentAura();
    }
}
