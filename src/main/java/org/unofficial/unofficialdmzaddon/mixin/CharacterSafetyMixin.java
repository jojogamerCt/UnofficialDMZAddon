package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.stats.Character;
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