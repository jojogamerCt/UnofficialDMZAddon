package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.menuslots.SuperformMenuSlot;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.dmz.UltraInstinctDefinitions;

@Mixin(value = SuperformMenuSlot.class, remap = false)
public abstract class SuperformMenuSlotMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$allowUltraInstinctOnlyRender(StatsData statsData,
                                                                 CallbackInfoReturnable<ButtonInfo> cir) {
        if (!hasUltraInstinctOnlyAccess(statsData)) {
            return;
        }

        ActionMode currentMode = statsData.getStatus().getSelectedAction();
        String race = statsData.getCharacter().getRaceName();
        cir.setReturnValue(new ButtonInfo(
                Component.translatable("race.dragonminez." + race + ".group." + statsData.getCharacter().getSelectedFormGroup()).withStyle(ChatFormatting.BOLD),
                Component.translatable("race.dragonminez." + race + ".form." + statsData.getCharacter().getSelectedFormGroup() + "." + TransformationsHelper.getFirstFormGroup(statsData.getCharacter().getSelectedFormGroup(), race)),
                currentMode == ActionMode.FORM
        ));
    }

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$allowUltraInstinctOnlyHandle(StatsData statsData, CallbackInfo ci) {
        if (!hasUltraInstinctOnlyAccess(statsData)) {
            return;
        }

        boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.FORM;
        if (wasActive && statsData.getCharacter().hasActiveForm()) {
            if (TransformationsHelper.canDescend(statsData)) {
                NetworkHandler.sendToServer(new ExecuteActionC2S("descend"));
                playToggleSound(false);
            }
        } else if (!wasActive) {
            NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.FORM));
            playToggleSound(true);
        } else {
            NetworkHandler.sendToServer(new ExecuteActionC2S("cycle_form_group"));
            playToggleSound(true);
        }

        ci.cancel();
    }

    private static boolean hasUltraInstinctOnlyAccess(StatsData statsData) {
        boolean hasLegacyForms = statsData.getSkills().getSkillLevel("superform") >= 1
                || statsData.getSkills().getSkillLevel("legendaryforms") >= 1
                || statsData.getSkills().getSkillLevel("godform") >= 1
                || statsData.getSkills().getSkillLevel("androidforms") >= 1;
        boolean hasUltraInstinct = statsData.getSkills().getSkillLevel(UltraInstinctDefinitions.SKILL_ID) >= 1;
        return hasUltraInstinct && !hasLegacyForms;
    }

    private static void playToggleSound(boolean turnedOn) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (turnedOn) {
            minecraft.player.playSound(MainSounds.SWITCH_ON.get(), 1.0F, 1.0F);
        } else {
            minecraft.player.playSound(MainSounds.SWITCH_OFF.get(), 1.0F, 1.0F);
        }
    }
}
