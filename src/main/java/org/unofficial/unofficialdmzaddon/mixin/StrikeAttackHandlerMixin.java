package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.technique.CanonTechniqueHandler;

@Mixin(value = StrikeAttackHandler.class, remap = false)
public abstract class StrikeAttackHandlerMixin {
    @Inject(method = "requestStrike", at = @At("HEAD"), cancellable = true)
    private static void unofficial$canonStrike(ServerPlayer player, int targetId, CallbackInfo ci) {
        if (CanonTechniqueHandler.requestCustomStrike(player, targetId)) ci.cancel();
    }
}
