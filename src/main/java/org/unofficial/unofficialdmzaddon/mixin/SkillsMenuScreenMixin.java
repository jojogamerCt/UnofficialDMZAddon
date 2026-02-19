package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.character.SkillsMenuScreen;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.dmz.UltraInstinctDefinitions;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = SkillsMenuScreen.class, remap = false)
public abstract class SkillsMenuScreenMixin {

    @Shadow
    private StatsData statsData;

    @Inject(method = "getVisibleSkillNames", at = @At("RETURN"), cancellable = true)
    private void unofficialdmzaddon$addUltraInstinctFormsSection(CallbackInfoReturnable<List<String>> cir) {
        List<String> original = cir.getReturnValue();
        if (original == null) {
            return;
        }

        List<String> result = new ArrayList<>(original);
        boolean isFormsList = result.contains("superform")
                || result.contains("godform")
                || result.contains("legendaryforms")
                || result.contains("androidforms");

        if (isFormsList) {
            if (statsData != null
                    && statsData.getSkills().hasSkill(UltraInstinctDefinitions.SKILL_ID)
                    && !result.contains(UltraInstinctDefinitions.SKILL_ID)) {
                result.add(UltraInstinctDefinitions.SKILL_ID);
                result.sort(String::compareToIgnoreCase);
                cir.setReturnValue(result);
            }
            return;
        }

        if (result.removeIf(name -> UltraInstinctDefinitions.SKILL_ID.equalsIgnoreCase(name))) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "getUpgradeCost", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$resolveUltraInstinctUpgradeCost(String skillName,
                                                                    int currentLevel,
                                                                    CallbackInfoReturnable<Integer> cir) {
        if (!UltraInstinctDefinitions.SKILL_ID.equalsIgnoreCase(skillName)) {
            return;
        }

        int[] costs = UltraInstinctDefinitions.SKILL_TP_COSTS;
        if (currentLevel < 0 || currentLevel >= costs.length) {
            cir.setReturnValue(Integer.MAX_VALUE);
            return;
        }

        cir.setReturnValue(costs[currentLevel]);
    }
}
