package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.layer.DMZSkinLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OutlineBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Composites the Custom Forms Creator mask with Minecraft's real entity-outline pass. */
@Mixin(OutlineBufferSource.class)
public abstract class CustomFormOutlinePreviewMixin {
    @Inject(method = "endOutlineBatch", at = @At("TAIL"))
    private void unofficialdmzaddon$renderLiveCustomFormOutline(CallbackInfo ci) {
        if (!DMZSkinLayer.PREVIEW_MODE) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.levelRenderer != null) minecraft.levelRenderer.doEntityOutline();
    }
}
