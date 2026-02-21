package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.dmz.SpecialRaceFormsDefinitions;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

@SuppressWarnings({"unchecked", "rawtypes"})
@Mixin(value = DMZSkinLayer.class, remap = false)
public abstract class DMZSkinLayerMixin extends GeoRenderLayer {

    // GeoRenderLayer requires a constructor; Mixin ignores constructors.
    protected DMZSkinLayerMixin(GeoRenderer renderer) {
        super(renderer);
    }

    // ── Frost Demon Super Forms 2 (Black / Golden): render as Final Form ──────

    @ModifyVariable(method = "renderSpecializedRace", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private String unofficialdmzaddon$renderSuperForms2AsFinalInSkinLayers(String formName) {
        if ("black".equalsIgnoreCase(formName) || "golden".equalsIgnoreCase(formName)) {
            return FrostDemonForms.FINAL_FORM;
        }
        return formName;
    }

    @ModifyVariable(method = "renderFaceLayers", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private String unofficialdmzaddon$renderSuperForms2AsFinalInFaceLayers(String formName) {
        if ("black".equalsIgnoreCase(formName) || "golden".equalsIgnoreCase(formName)) {
            return FrostDemonForms.FINAL_FORM;
        }
        return formName;
    }

    // ── Alien race body rendering ─────────────────────────────────────────────

    private static final ThreadLocal<Boolean> ALIEN_REDIRECT_ACTIVE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @ModifyVariable(method = "renderBody", at = @At(value = "STORE", ordinal = 0), ordinal = 0, argsOnly = false)
    private String unofficialdmzaddon$redirectAlienRaceNameInBody(String raceName) {
        if (SpecialRaceFormsDefinitions.ALIEN_RACE.equals(raceName)) {
            ALIEN_REDIRECT_ACTIVE.set(Boolean.TRUE);
            return "namekian";
        }
        return raceName;
    }

    @ModifyVariable(method = "renderSpecializedRace", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String unofficialdmzaddon$restoreAlienRaceInSpecializedRender(String race) {
        if (ALIEN_REDIRECT_ACTIVE.get()) {
            ALIEN_REDIRECT_ACTIVE.set(Boolean.FALSE);
            return SpecialRaceFormsDefinitions.ALIEN_RACE;
        }
        return race;
    }

    // ── Alien Full Power: render base texture with red tint (Jiren FP) ────────

    private static final float[] FP_RED_1 = {1.00f, 0.55f, 0.55f};
    private static final float[] FP_RED_2 = {0.95f, 0.38f, 0.38f};
    private static final float[] FP_RED_3 = {0.85f, 0.22f, 0.22f};

    @Inject(
            method = "renderSpecializedRace",
            at = @At("HEAD"),
            cancellable = true
    )
    private void unofficialdmzaddon$renderAlienFullPower(
            BakedGeoModel model, PoseStack poseStack, AbstractClientPlayer animatable, MultiBufferSource bufferSource,
            String race, String form, int bodyType, boolean hasForm,
            float[] b1, float[] b2, float[] b3, float[] h,
            float pt, int pl, int po,
            CallbackInfo ci) {

        if (!SpecialRaceFormsDefinitions.ALIEN_RACE.equals(race)) return;
        if (!SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER.equalsIgnoreCase(form)) return;

        String prefix = "textures/entity/races/alien/bodytype_" + bodyType + "_";

        unofficialdmzaddon$renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", FP_RED_1, pt, pl, po);
        unofficialdmzaddon$renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", FP_RED_2, pt, pl, po);
        unofficialdmzaddon$renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer3.png", FP_RED_3, pt, pl, po);

        ci.cancel();
    }

    // ── Helper: render a single colored texture layer ─────────────────────────

    @Unique
    private void unofficialdmzaddon$renderColoredLayer(
            BakedGeoModel model,
            PoseStack poseStack,
            Object animatable,
            MultiBufferSource bufferSource,
            String path,
            float[] rgb,
            float partialTick,
            int packedLight,
            int packedOverlay) {

        ResourceLocation loc = new ResourceLocation(Reference.MOD_ID, path);
        if (!Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) {
            return;
        }

        RenderType renderType = RenderType.entityCutoutNoCull(loc);
        poseStack.pushPose();
        GeoRenderer renderer = getRenderer();
        renderer.reRender(model, poseStack, bufferSource, (GeoAnimatable) animatable, renderType,
                bufferSource.getBuffer(renderType), partialTick, packedLight, packedOverlay,
                rgb[0], rgb[1], rgb[2], 1.0f);
        poseStack.popPose();
    }
}
