package org.unofficial.unofficialdmzaddon.mixin;

import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.client.HalalModeText;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {
    @Inject(
            method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void unofficialdmzaddon$applyHalalMode(
            String key,
            String fallback,
            CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(HalalModeText.apply(cir.getReturnValue()));
    }
}
