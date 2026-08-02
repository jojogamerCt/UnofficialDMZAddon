package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.FormMasteries;
import com.dragonminez.common.stats.extras.UsedForms;
import com.dragonminez.common.util.lists.StackForms;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Character.class, remap = false)
public abstract class CharacterSafetyMixin {

    @Shadow private String race;
    @Shadow private String gender;
    @Shadow private String characterClass;
    @Shadow private String selectedFormGroup;
    @Shadow private String activeFormGroup;
    @Shadow private String activeForm;
    @Shadow private String selectedForm;
    @Shadow private String selectedStackFormGroup;
    @Shadow private String activeStackFormGroup;
    @Shadow private String selectedStackForm;
    @Shadow private String activeStackForm;
    @Shadow @Final private FormMasteries formMasteries;
    @Shadow private UsedForms formsUsedBefore;
    @Shadow private String previousFormGroup;
    @Shadow private String previousForm;
    @Shadow @Final private FormMasteries stackFormMasteries;
    @Shadow private String bodyColor;
    @Shadow private String bodyColor2;
    @Shadow private String bodyColor3;
    @Shadow private String hairColor;
    @Shadow private String eye1Color;
    @Shadow private String eye2Color;
    @Shadow private String auraColor;
    @Shadow private Boolean armored;

    @Inject(method = "setSelectedFormGroup", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$guardSelectedFormGroup(String selectedFormGroup, CallbackInfo ci) {
        this.selectedFormGroup = selectedFormGroup == null ? "" : selectedFormGroup;
        ci.cancel();
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void unofficialdmzaddon$migrateDivineStackState(CompoundTag tag, CallbackInfo ci) {
        migrateMastery(StackForms.GROUP_ULTRAINSTINCT, "sign", "mastered", "autonomous", "true");
        migrateMastery(StackForms.GROUP_ULTRAEGO, "sign", "mastered");

        migrateRemovedDivineForms();

        if (isDivine(activeStackFormGroup)) {
            if (activeForm == null || activeForm.isEmpty()) {
                activeFormGroup = activeStackFormGroup;
                activeForm = activeStackForm;
            }
            activeStackFormGroup = "";
            activeStackForm = "";
        }
        if (isDivine(selectedStackFormGroup)) {
            if (activeForm == null || activeForm.isEmpty() || selectedStackFormGroup.equalsIgnoreCase(activeFormGroup)) {
                selectedFormGroup = selectedStackFormGroup;
                selectedForm = selectedStackForm;
            }
            selectedStackFormGroup = "";
            selectedStackForm = "";
        }

        migrateSaiyanGodForms();
    }

    private void migrateRemovedDivineForms() {
        double autonomousMastery = formMasteries.getMastery(StackForms.GROUP_ULTRAINSTINCT, "autonomous");
        double masteredMastery = formMasteries.getMastery(StackForms.GROUP_ULTRAINSTINCT, "mastered");
        if (autonomousMastery > masteredMastery) {
            formMasteries.setMastery(StackForms.GROUP_ULTRAINSTINCT, "mastered", autonomousMastery, 100.0);
        }
        double egoSignMastery = formMasteries.getMastery(StackForms.GROUP_ULTRAEGO, "sign");
        double egoMastery = formMasteries.getMastery(StackForms.GROUP_ULTRAEGO, "mastered");
        if (egoSignMastery > egoMastery) {
            formMasteries.setMastery(StackForms.GROUP_ULTRAEGO, "mastered", egoSignMastery, 100.0);
        }
        if ("autonomous".equalsIgnoreCase(selectedForm) && StackForms.GROUP_ULTRAINSTINCT.equalsIgnoreCase(selectedFormGroup)) selectedForm = "mastered";
        if ("autonomous".equalsIgnoreCase(activeForm) && StackForms.GROUP_ULTRAINSTINCT.equalsIgnoreCase(activeFormGroup)) activeForm = "mastered";
        if ("sign".equalsIgnoreCase(selectedForm) && StackForms.GROUP_ULTRAEGO.equalsIgnoreCase(selectedFormGroup)) selectedForm = "mastered";
        if ("sign".equalsIgnoreCase(activeForm) && StackForms.GROUP_ULTRAEGO.equalsIgnoreCase(activeFormGroup)) activeForm = "mastered";
    }
    private void migrateSaiyanGodForms() {
        for (String form : new String[] {"super_saiyan_god", "super_saiyan_blue", "super_saiyan_rose"}) {
            double oldMastery = formMasteries.getMastery("supersaiyan", form);
            double current = formMasteries.getMastery("godforms", form);
            if (oldMastery > current) formMasteries.setMastery("godforms", form, oldMastery, 100.0);
            if (formsUsedBefore != null && formsUsedBefore.getFormGroup("supersaiyan").contains(form)) {
                formsUsedBefore.putForm("godforms", form);
            }
        }
        if (isMovedGodForm(selectedForm) && "supersaiyan".equalsIgnoreCase(selectedFormGroup)) selectedFormGroup = "godforms";
        if (isMovedGodForm(activeForm) && "supersaiyan".equalsIgnoreCase(activeFormGroup)) activeFormGroup = "godforms";
        if (isMovedGodForm(previousForm) && "supersaiyan".equalsIgnoreCase(previousFormGroup)) previousFormGroup = "godforms";
    }

    private static boolean isMovedGodForm(String form) {
        return form != null && (form.equalsIgnoreCase("super_saiyan_god")
                || form.equalsIgnoreCase("super_saiyan_blue") || form.equalsIgnoreCase("super_saiyan_rose"));
    }
    private void migrateMastery(String group, String... forms) {
        for (String form : forms) {
            double oldMastery = stackFormMasteries.getMastery(group, form);
            double current = formMasteries.getMastery(group, form);
            if (oldMastery > current) formMasteries.setMastery(group, form, oldMastery, 100.0);
        }
    }

    private static boolean isDivine(String group) {
        return group != null && (StackForms.GROUP_ULTRAINSTINCT.equalsIgnoreCase(group)
                || StackForms.GROUP_ULTRAEGO.equalsIgnoreCase(group));
    }
    @Inject(method = "save", at = @At("HEAD"))
    private void unofficialdmzaddon$sanitizeNullStringsBeforeSave(CallbackInfoReturnable<?> cir) {
        race = nullTo(race, "human");
        gender = nullTo(gender, "male");
        characterClass = nullTo(characterClass, "warrior");
        selectedFormGroup = nullTo(selectedFormGroup, "");
        activeFormGroup = nullTo(activeFormGroup, "");
        activeForm = nullTo(activeForm, "");

        bodyColor = nullTo(bodyColor, "#F5D5A6");
        bodyColor2 = nullTo(bodyColor2, "#F5D5A6");
        bodyColor3 = nullTo(bodyColor3, "#F5D5A6");
        hairColor = nullTo(hairColor, "#000000");
        eye1Color = nullTo(eye1Color, "#0E1011");
        eye2Color = nullTo(eye2Color, "#0E1011");
        auraColor = nullTo(auraColor, "#FFFFFF");

        if (armored == null) {
            armored = false;
        }
    }

    private static String nullTo(String value, String fallback) {
        return value == null ? fallback : value;
    }
}