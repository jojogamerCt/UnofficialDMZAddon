package org.unofficial.unofficialdmzaddon.mixin;

import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.client.HalalModeText;
import org.unofficial.unofficialdmzaddon.client.CustomFormsClientState;

@Mixin(Language.class)
public abstract class LanguageMixin {
    @Inject(method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void unofficialdmzaddon$applyHalalMode(String key, CallbackInfoReturnable<String> cir) {
        String customName = CustomFormsClientState.translatedName(key);
        cir.setReturnValue(HalalModeText.apply(customName != null ? customName : cir.getReturnValue()));
    }
}
