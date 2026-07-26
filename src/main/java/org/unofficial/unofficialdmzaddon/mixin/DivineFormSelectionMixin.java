package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.StackForms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.dmz.SpecialRaceFormsDefinitions;

/** Keeps addon transformations mutually exclusive with other active transformation groups. */
@Mixin(value = TransformationsHelper.class, remap = false)
public abstract class DivineFormSelectionMixin {

    @Inject(method = "isSelectableForm", at = @At("HEAD"), cancellable = true)
    private static void unofficialdmzaddon$denyExclusiveSelectionWhileTransformed(
            StatsData data, String group, String form, CallbackInfoReturnable<Boolean> cir) {
        if (isExclusiveTarget(data, group, form) && isBlockedByCurrentTransformation(data, group)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getNextAvailableForm", at = @At("HEAD"), cancellable = true)
    private static void unofficialdmzaddon$denyExclusiveActivationWhileTransformed(
            StatsData data, CallbackInfoReturnable<FormConfig.FormData> cir) {
        if (data == null || data.getCharacter() == null) return;
        String targetGroup = TransformationsHelper.getTransformTargetGroup(data);
        FormConfig.FormData candidate = TransformationsHelper.getNextFormCandidate(data);
        if (candidate != null && isExclusiveTarget(data, targetGroup, candidate.getName())
                && isBlockedByCurrentTransformation(data, targetGroup)) {
            cir.setReturnValue(null);
        }
    }

    private static boolean isBlockedByCurrentTransformation(StatsData data, String targetGroup) {
        if (data == null || data.getCharacter() == null) return false;
        var character = data.getCharacter();
        if (character.hasActiveStackForm()) return true;
        return character.hasActiveForm() && !targetGroup.equalsIgnoreCase(character.getActiveFormGroup());
    }

    private static boolean isExclusiveTarget(StatsData data, String group, String form) {
        if (group == null || form == null) return false;
        if (StackForms.GROUP_ULTRAINSTINCT.equalsIgnoreCase(group)
                || StackForms.GROUP_ULTRAEGO.equalsIgnoreCase(group)) return true;
        if (data == null || data.getCharacter() == null) return false;

        String race = data.getCharacter().getRaceName();
        if (SpecialRaceFormsDefinitions.SAIYAN_RACE.equalsIgnoreCase(race)) {
            return SpecialRaceFormsDefinitions.SAIYAN_GROUP_BEAST.equalsIgnoreCase(group)
                    && SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST.equalsIgnoreCase(form);
        }
        if (SpecialRaceFormsDefinitions.NAMEKIAN_RACE.equalsIgnoreCase(race)) {
            return SpecialRaceFormsDefinitions.NAMEKIAN_GROUP_SUPERFORMS.equalsIgnoreCase(group)
                    && SpecialRaceFormsDefinitions.NAMEKIAN_FORM_ORANGE.equalsIgnoreCase(form);
        }
        if (SpecialRaceFormsDefinitions.FROST_DEMON_RACE.equalsIgnoreCase(race)) {
            return SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_SUPERFORMS2.equalsIgnoreCase(group)
                    && (SpecialRaceFormsDefinitions.FROST_DEMON_FORM_GOLDEN.equalsIgnoreCase(form)
                    || SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK.equalsIgnoreCase(form));
        }
        return SpecialRaceFormsDefinitions.ALIEN_RACE.equalsIgnoreCase(race)
                && SpecialRaceFormsDefinitions.ALIEN_GROUP_SUPERFORMS.equalsIgnoreCase(group)
                && SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER.equalsIgnoreCase(form);
    }
}