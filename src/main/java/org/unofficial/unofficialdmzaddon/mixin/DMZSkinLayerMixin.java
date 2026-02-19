package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.common.util.lists.FrostDemonForms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = DMZSkinLayer.class, remap = false)
public abstract class DMZSkinLayerMixin {

    @ModifyVariable(method = "renderSpecializedRace", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private String unofficialdmzaddon$renderBlackAsFinalInSkinLayers(String formName) {
        if ("black".equalsIgnoreCase(formName)) {
            return FrostDemonForms.FINAL_FORM;
        }
        return formName;
    }

    @ModifyVariable(method = "renderFaceLayers", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private String unofficialdmzaddon$renderBlackAsFinalInFaceLayers(String formName) {
        if ("black".equalsIgnoreCase(formName)) {
            return FrostDemonForms.FINAL_FORM;
        }
        return formName;
    }
}
