package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormDefinition;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormManager;

import java.util.List;
import java.util.ArrayList;

@Mixin(value = TransformationsHelper.class, remap = false)
public abstract class CustomFormOwnershipMixin {
    @Inject(method = "getUnlockedForms", at = @At("HEAD"), cancellable = true)
    private static void unofficialdmzaddon$hideForeignCustomForms(StatsData data, String race, String group,
                                                                   CallbackInfoReturnable<List<FormConfig.FormData>> cir) {
        if (group == null || !group.startsWith("customforms_")) return;
        if (data == null || data.getPlayer() == null || !CustomFormManager.ownsGroup(data.getPlayer().getUUID(), group)) {
            cir.setReturnValue(List.of());
            return;
        }
        FormConfig customGroup = ConfigManager.getFormGroup(race, group);
        cir.setReturnValue(customGroup == null || customGroup.getForms() == null
                ? List.of()
                : new ArrayList<>(customGroup.getForms().values()));
    }
}
