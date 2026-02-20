package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.client.util.TextureCounter;
import com.dragonminez.common.network.C2S.StatsSyncC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.Character;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.dmz.SpecialRaceFormsDefinitions;

@Mixin(value = CharacterCustomizationScreen.class, remap = false)
public abstract class CharacterCustomizationScreenMixin {

    @Shadow
    @Final
    private Character character;

    @Shadow
    private void refreshButtons() {
    }

    @Inject(method = "changeEyes", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$fixAlienEyesTypeWrap(int delta, CallbackInfo ci) {
        if (!SpecialRaceFormsDefinitions.ALIEN_RACE.equals(character.getRace())) {
            return;
        }

        int maxEyes = TextureCounter.getMaxEyesTypes(character.getRace());
        int currentEyes = character.getEyesType();
        int newEyes;

        if (maxEyes <= 0) {
            newEyes = 0;
        } else {
            newEyes = currentEyes + delta;
            if (newEyes < 0) {
                newEyes = maxEyes;
            } else if (newEyes > maxEyes) {
                newEyes = 0;
            }
        }

        if (currentEyes != newEyes) {
            character.setEyesType(newEyes);
            NetworkHandler.sendToServer(new StatsSyncC2S(character));
        }
        refreshButtons();
        ci.cancel();
    }

    @Inject(method = "changeNose", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$fixAlienNoseTypeWrap(int delta, CallbackInfo ci) {
        if (!SpecialRaceFormsDefinitions.ALIEN_RACE.equals(character.getRace())) {
            return;
        }

        int maxNose = TextureCounter.getMaxNoseTypes(character.getRace());
        int currentNose = character.getNoseType();
        int newNose;

        if (maxNose <= 0) {
            newNose = 0;
        } else {
            newNose = currentNose + delta;
            if (newNose < 0) {
                newNose = maxNose;
            } else if (newNose > maxNose) {
                newNose = 0;
            }
        }

        if (currentNose != newNose) {
            character.setNoseType(newNose);
            NetworkHandler.sendToServer(new StatsSyncC2S(character));
        }
        refreshButtons();
        ci.cancel();
    }
}
