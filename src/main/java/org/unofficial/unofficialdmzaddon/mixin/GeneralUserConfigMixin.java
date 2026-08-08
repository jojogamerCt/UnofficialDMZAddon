package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.config.GeneralUserConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.unofficial.unofficialdmzaddon.client.HalalModeConfigAccess;

@Mixin(value = GeneralUserConfig.class, remap = false)
public class GeneralUserConfigMixin implements HalalModeConfigAccess {
    @Unique private Boolean unofficialdmzaddon$halalMode = false;

    @Override
    public boolean unofficialdmzaddon$isHalalMode() {
        if (unofficialdmzaddon$halalMode == null) unofficialdmzaddon$halalMode = false;
        return unofficialdmzaddon$halalMode;
    }

    @Override
    public void unofficialdmzaddon$setHalalMode(boolean enabled) {
        unofficialdmzaddon$halalMode = enabled;
    }
}
