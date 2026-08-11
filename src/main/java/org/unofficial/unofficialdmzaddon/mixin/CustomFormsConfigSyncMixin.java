package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.client.CustomFormsClientState;
import org.unofficial.unofficialdmzaddon.dmz.AddonTechniqueInstaller;

@Mixin(value = ConfigManager.class, remap = false)
public abstract class CustomFormsConfigSyncMixin {
    @Inject(method = {"applySpecificSyncedConfig", "applySyncedServerConfig"}, at = @At("TAIL"))
    private static void unofficialdmzaddon$restoreCustomFormsAfterDmzSync(CallbackInfo ci) {
        CustomFormsClientState.reinstallAll();
        AddonTechniqueInstaller.reinstallAfterConfigSync();
    }
}
