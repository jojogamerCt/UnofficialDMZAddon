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

    /**
     * Custom forms are purchased when they are created, not through a synthetic DMZ skill.
     * DMZ's charging preview asks this method directly, so owner forms must remain available
     * even while an older player's stats are still being migrated to the display-only skill.
     */
    @Inject(method = "getNextAvailableForm", at = @At("RETURN"), cancellable = true)
    private static void unofficialdmzaddon$allowOwnedCustomTransform(StatsData data,
                                                                      CallbackInfoReturnable<FormConfig.FormData> cir) {
        if (cir.getReturnValue() != null || data == null || data.getPlayer() == null) return;
        String group = data.getCharacter().getSelectedFormGroup();
        if (!CustomFormManager.ownsGroup(data.getPlayer().getUUID(), group)) return;
        FormConfig customGroup = ConfigManager.getFormGroup(data.getCharacter().getRaceName(), group);
        if (customGroup == null) return;
        FormConfig.FormData selected = customGroup.getForm(data.getCharacter().getSelectedForm());
        if (selected != null) cir.setReturnValue(selected);
    }
}
