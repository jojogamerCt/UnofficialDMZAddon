package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.dmz.SpecialRaceFormsDefinitions;

/**
 * Scales the Alien Full Power form's stat multipliers based on the player's
 * superform skill level. Level 1 uses the base multipliers; levels 2-4
 * progressively increase them.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class AlienFullPowerScalingMixin {

    @Inject(method = "getFormMultiplier", at = @At("RETURN"), cancellable = true)
    private void unofficialdmzaddon$scaleAlienFullPowerMultiplier(String statName,
                                                                   CallbackInfoReturnable<Double> cir) {
        StatsData self = (StatsData) (Object) this;

        String race = self.getCharacter().getRaceName();
        if (!SpecialRaceFormsDefinitions.ALIEN_RACE.equalsIgnoreCase(race)) {
            return;
        }

        String activeForm = self.getCharacter().getActiveForm();
        if (!SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER.equalsIgnoreCase(activeForm)) {
            return;
        }

        int level = self.getSkills().getSkillLevel("superform");
        if (level <= 1) {
            return;
        }

        double[] scaling = SpecialRaceFormsDefinitions.ALIEN_FULL_POWER_LEVEL_SCALING;
        int index = Math.min(level, scaling.length) - 1;
        double scale = scaling[index];

        cir.setReturnValue(cir.getReturnValue() * scale);
    }
}
