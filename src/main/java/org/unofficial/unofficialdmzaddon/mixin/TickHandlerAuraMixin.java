package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.TickHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.unofficial.unofficialdmzaddon.dmz.DivineAuraHelper;

@Mixin(value = TickHandler.class, remap = false)
public abstract class TickHandlerAuraMixin {
    @ModifyVariable(method = "updateAuraLight", at = @At("STORE"), ordinal = 0)
    private static boolean unofficialdmzaddon$forceGodAuraLight(boolean original,
            ServerPlayer player, StatsData data) {
        return original || DivineAuraHelper.hasPersistentAura(data);
    }
}
