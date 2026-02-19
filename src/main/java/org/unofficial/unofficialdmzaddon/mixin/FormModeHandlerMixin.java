package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.actionmode.FormModeHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.dmz.UltraInstinctDefinitions;

@Mixin(value = FormModeHandler.class, remap = false)
public abstract class FormModeHandlerMixin {

    @Inject(method = "handleActionCharge", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$applyUltraInstinctChargeScaling(ServerPlayer player,
                                                                    StatsData data,
                                                                    CallbackInfoReturnable<Integer> cir) {
        FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableForm(data);
        if (nextForm == null) {
            return;
        }

        String group = data.getCharacter().hasActiveForm()
                ? data.getCharacter().getActiveFormGroup()
                : data.getCharacter().getSelectedFormGroup();

        FormConfig groupConfig = ConfigManager.getFormGroup(data.getCharacter().getRaceName(), group);
        if (groupConfig == null || !UltraInstinctDefinitions.FORM_TYPE.equalsIgnoreCase(groupConfig.getFormType())) {
            return;
        }

        int skillLvl = data.getSkills().getSkillLevel(UltraInstinctDefinitions.SKILL_ID);
        cir.setReturnValue(6 * Math.max(1, skillLvl));
    }
}
