package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.model.DMZPlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.util.Arrays;

@Mixin(value = DMZPlayerModel.class, remap = false)
public abstract class DMZPlayerModelAnimationMixin {
    private static final ResourceLocation ADDON_ANIMATIONS =
            new ResourceLocation(UnofficialDMZAddon.MODID, "animations/player/canon_techniques.animation.json");

    @Inject(method = "getAnimationResourceFallbacks", at = @At("RETURN"), cancellable = true)
    private void unofficial$appendAnimations(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation[]> cir) {
        ResourceLocation[] existing = cir.getReturnValue();
        if (Arrays.asList(existing).contains(ADDON_ANIMATIONS)) return;
        ResourceLocation[] combined = Arrays.copyOf(existing, existing.length + 1);
        combined[existing.length] = ADDON_ANIMATIONS;
        cir.setReturnValue(combined);
    }
}
