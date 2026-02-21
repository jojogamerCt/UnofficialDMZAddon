package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZRenderHand;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.dmz.SpecialRaceFormsDefinitions;

@Mixin(value = DMZRenderHand.class, remap = false)
public abstract class DMZRenderHandMixin {

    // ── Frost Demon Super Forms 2 (Black / Golden): render as Final Form ────────

    @ModifyVariable(method = "renderRaceLayers", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private String unofficialdmzaddon$renderSuperForms2AsFinalInFirstPerson(String formName) {
        if ("black".equalsIgnoreCase(formName) || "golden".equalsIgnoreCase(formName)) {
            return FrostDemonForms.FINAL_FORM;
        }
        return formName;
    }

    // ── Alien race first-person rendering ───────────────────────────────────

    private static final float[] FP_RED_1 = {1.00f, 0.55f, 0.55f};
    private static final float[] FP_RED_2 = {0.95f, 0.38f, 0.38f};
    private static final float[] FP_RED_3 = {0.85f, 0.22f, 0.22f};

    @Inject(method = "renderRaceLayers", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$renderAlienFirstPerson(
            PoseStack stack, MultiBufferSource buffer, int light, ModelPart arm,
            String race, String form, String gender, int bodyType,
            float[] b1, float[] b2, float[] b3, float[] h,
            CallbackInfo ci) {

        if (!SpecialRaceFormsDefinitions.ALIEN_RACE.equals(race)) {
            return;
        }

        String prefix = "textures/entity/races/alien/bodytype_" + bodyType + "_";

        boolean isFullPower = SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER
                .equalsIgnoreCase(form);

        float[] c1 = isFullPower ? FP_RED_1 : b1;
        float[] c2 = isFullPower ? FP_RED_2 : b2;
        float[] c3 = isFullPower ? FP_RED_3 : b3;

        unofficialdmzaddon$renderHandPart(stack, buffer, light, arm, prefix + "layer1.png", c1);
        unofficialdmzaddon$renderHandPart(stack, buffer, light, arm, prefix + "layer2.png", c2);
        unofficialdmzaddon$renderHandPart(stack, buffer, light, arm, prefix + "layer3.png", c3);

        ci.cancel();
    }

    // ── Helper: render a single hand texture layer ──────────────────────────

    @Unique
    private void unofficialdmzaddon$renderHandPart(
            PoseStack stack, MultiBufferSource buffer, int light,
            ModelPart part, String path, float[] rgb) {

        ResourceLocation loc = new ResourceLocation(Reference.MOD_ID, path);
        if (!Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) {
            return;
        }

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(loc));
        part.render(stack, vc, light, OverlayTexture.NO_OVERLAY,
                rgb[0], rgb[1], rgb[2], 1.0f);
    }
}
