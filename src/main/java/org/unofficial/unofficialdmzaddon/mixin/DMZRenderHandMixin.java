package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.DMZRenderHand;
import com.dragonminez.common.util.lists.FrostDemonForms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = DMZRenderHand.class, remap = false)
public abstract class DMZRenderHandMixin {

    @ModifyVariable(method = "renderRaceLayers", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private String unofficialdmzaddon$renderBlackAsFinalInFirstPerson(String formName) {
        if ("black".equalsIgnoreCase(formName)) {
            return FrostDemonForms.FINAL_FORM;
        }
        return formName;
    }
}
