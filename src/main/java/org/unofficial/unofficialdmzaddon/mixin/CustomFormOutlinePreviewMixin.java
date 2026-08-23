package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.layer.DMZSkinLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OutlineBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OutlineBufferSource.class, remap = false)
public abstract class CustomFormOutlinePreviewMixin {
    @Inject(method = "m_109928_", at = @At("TAIL"), remap = false)
    private void unofficialdmzaddon$renderLiveCustomFormOutline(CallbackInfo ci) {
        if (!DMZSkinLayer.PREVIEW_MODE) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.levelRenderer != null) minecraft.levelRenderer.doEntityOutline();
    }
}
