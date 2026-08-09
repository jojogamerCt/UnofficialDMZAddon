package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.config.GeneralUserConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.unofficial.unofficialdmzaddon.client.HalalModeConfigAccess;
import org.unofficial.unofficialdmzaddon.client.GodAuraConfigAccess;

@Mixin(value = GeneralUserConfig.class, remap = false)
public class GeneralUserConfigMixin implements HalalModeConfigAccess, GodAuraConfigAccess {
    @Unique private Boolean unofficialdmzaddon$halalMode = false;
    @Unique private Boolean unofficialdmzaddon$constantGodFormAuras = true;

    @Override
    public boolean unofficialdmzaddon$isHalalMode() {
        if (unofficialdmzaddon$halalMode == null) unofficialdmzaddon$halalMode = false;
        return unofficialdmzaddon$halalMode;
    }

    @Override
    public void unofficialdmzaddon$setHalalMode(boolean enabled) {
        unofficialdmzaddon$halalMode = enabled;
    }

    @Override
    public boolean unofficialdmzaddon$constantGodFormAuras() {
        if (unofficialdmzaddon$constantGodFormAuras == null) unofficialdmzaddon$constantGodFormAuras = true;
        return unofficialdmzaddon$constantGodFormAuras;
    }

    @Override
    public void unofficialdmzaddon$setConstantGodFormAuras(boolean enabled) {
        unofficialdmzaddon$constantGodFormAuras = enabled;
    }
}
