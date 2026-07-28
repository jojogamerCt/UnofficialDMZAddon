package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.dmz.LegacyRaceCleanup;

import java.util.List;

@Mixin(value = ConfigManager.class, remap = false)
public abstract class RemovedRaceFilterMixin {
    @Inject(method = "getLoadedRaces", at = @At("RETURN"))
    private static void unofficialdmzaddon$filterRemovedRaces(CallbackInfoReturnable<List<String>> cir) {
        cir.getReturnValue().removeIf(LegacyRaceCleanup::isRemovedRace);
    }
}