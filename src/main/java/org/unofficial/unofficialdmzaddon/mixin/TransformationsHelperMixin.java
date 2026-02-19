package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.dmz.UltraInstinctDefinitions;

@Mixin(value = TransformationsHelper.class, remap = false)
public abstract class TransformationsHelperMixin {

    @Inject(method = "getSkillLevelForType", at = @At("HEAD"), cancellable = true)
    private static void unofficialdmzaddon$resolveUltraInstinctSkillLevel(StatsData statsData,
                                                                          String formType,
                                                                          CallbackInfoReturnable<Integer> cir) {
        if (!UltraInstinctDefinitions.FORM_TYPE.equalsIgnoreCase(formType)) {
            return;
        }
        cir.setReturnValue(statsData.getSkills().getSkillLevel(UltraInstinctDefinitions.SKILL_ID));
    }
}
