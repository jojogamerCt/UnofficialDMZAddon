from pathlib import Path

screen = Path('src/main/java/org/unofficial/unofficialdmzaddon/client/CustomFormsScreen.java')
s = screen.read_text()

s = s.replace(
    'import com.dragonminez.client.render.layer.DMZSkinLayer;\n',
    'import com.dragonminez.client.render.layer.DMZSkinLayer;\n'
    'import com.dragonminez.client.render.shader.TransformationMaskBufferSource;\n'
    'import com.dragonminez.client.render.shader.TransformationMaskRenderState;\n'
    'import com.dragonminez.mixin.client.PostChainAccessor;\n',
    1,
)
s = s.replace(
    'import net.minecraft.client.renderer.GameRenderer;\n'
    'import net.minecraft.client.renderer.OutlineBufferSource;\n'
    'import net.minecraft.client.renderer.entity.EntityRenderDispatcher;\n',
    'import net.minecraft.client.renderer.EffectInstance;\n'
    'import net.minecraft.client.renderer.GameRenderer;\n'
    'import net.minecraft.client.renderer.PostChain;\n'
    'import net.minecraft.client.renderer.PostPass;\n'
    'import net.minecraft.client.renderer.entity.EntityRenderDispatcher;\n',
    1,
)
s = s.replace('import java.util.LinkedHashMap;\n', 'import java.io.IOException;\nimport java.util.LinkedHashMap;\n', 1)

old_field = '    private double lastMouseY;\n'
new_field = (
    '    private double lastMouseY;\n'
    '    private PostChain outlinePreviewChain;\n'
    '    private int outlinePreviewWidth = -1;\n'
    '    private int outlinePreviewHeight = -1;\n'
)
if old_field not in s:
    raise SystemExit('CustomFormsScreen field anchor not found')
s = s.replace(old_field, new_field, 1)

old_render = '''            if (editing && page == 4 && preview.auraOutlineEnabled()
                    && UnofficialDMZConfig.CUSTOM_FORMS_ALLOW_AURA_OUTLINE.get()) {
                renderOutlinePreview(graphics, player, preview, centerX, baseY, scale, pose, camera);
            }
            InventoryScreen.renderEntityInInventory(graphics, centerX, baseY, scale, pose, camera, player);
            graphics.flush();
'''
new_render = '''            InventoryScreen.renderEntityInInventory(graphics, centerX, baseY, scale, pose, camera, player);
            graphics.flush();
            if (editing && page == 4 && preview.auraOutlineEnabled()
                    && UnofficialDMZConfig.CUSTOM_FORMS_ALLOW_AURA_OUTLINE.get()) {
                renderOutlinePreview(graphics, player, preview, centerX, baseY, scale, pose, camera, partialTick);
            }
'''
if old_render not in s:
    raise SystemExit('CustomFormsScreen preview render anchor not found')
s = s.replace(old_render, new_render, 1)

start_marker = "    /** Renders the preview model through Minecraft's real entity-outline buffer. */"
end_marker = '    private void resetGuiRenderState'
start = s.find(start_marker)
end = s.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit('CustomFormsScreen outline method anchors not found')

replacement = '''    /**
     * Renders the creator preview through DragonMineZ's transformation-mask post shader, the
     * same outline path used by Ikari and by saved custom forms in normal gameplay.
     */
    private void renderOutlinePreview(GuiGraphics graphics, Player player, CustomFormDefinition preview,
                                      int centerX, int baseY, int modelScale,
                                      Quaternionf pose, Quaternionf camera, float partialTick) {
        FormConfig.FormData formData = preview.toFormData();
        FormConfig.FormData.OutlineShaderConfig outline = formData.getOutlineShader();
        if (outline == null || !outline.isEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        PostChain chain = outlinePreviewChain(minecraft);
        if (chain == null) return;
        var maskTarget = chain.getTempTarget("entity_mask");
        if (maskTarget == null) return;

        float[] primary = ColorUtils.hexToRgb(outline.getPrimaryColor());
        float[] secondary = ColorUtils.hexToRgb(outline.getSecondaryColor());
        TransformationMaskBufferSource maskSource = new TransformationMaskBufferSource();
        maskSource.setIncludeOriginal(false);
        maskSource.setEntityColors(primary[0], primary[1], primary[2],
                secondary[0], secondary[1], secondary[2]);

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, baseY, 50.0D);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(modelScale, modelScale, -modelScale));
        graphics.pose().mulPose(new Quaternionf(pose));
        Lighting.setupForEntityInInventory();
        dispatcher.overrideCameraOrientation(new Quaternionf(camera).conjugate());
        dispatcher.setRenderShadow(false);
        try {
            dispatcher.render(player, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F,
                    graphics.pose(), maskSource, 0xF000F0);
        } finally {
            dispatcher.setRenderShadow(true);
            graphics.pose().popPose();
            Lighting.setupFor3DItems();
        }

        try {
            maskTarget.clear(Minecraft.ON_OSX);
            maskTarget.copyDepthFrom(minecraft.getMainRenderTarget());
            TransformationMaskRenderState.setCurrentTargets(maskTarget);
            maskSource.endMaskBatch();
        } finally {
            TransformationMaskRenderState.setCurrentTargets(null);
            minecraft.getMainRenderTarget().bindWrite(false);
        }

        configureOutlinePreviewUniforms(chain, outline, player.tickCount + partialTick);
        chain.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(false);
        resetRawRenderState();
    }

    private PostChain outlinePreviewChain(Minecraft minecraft) {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        if (outlinePreviewChain != null && (outlinePreviewWidth != width || outlinePreviewHeight != height)) {
            outlinePreviewChain.resize(width, height);
            outlinePreviewWidth = width;
            outlinePreviewHeight = height;
        }
        if (outlinePreviewChain != null) return outlinePreviewChain;
        try {
            outlinePreviewChain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(),
                    minecraft.getMainRenderTarget(),
                    ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/transformation_outline.json"));
            outlinePreviewChain.resize(width, height);
            outlinePreviewWidth = width;
            outlinePreviewHeight = height;
            return outlinePreviewChain;
        } catch (IOException | RuntimeException ignored) {
            closeOutlinePreviewChain();
            return null;
        }
    }

    private void configureOutlinePreviewUniforms(PostChain chain,
                                                 FormConfig.FormData.OutlineShaderConfig outline,
                                                 float animationTicks) {
        float animationTime = animationTicks / 20.0F;
        float blurRadius = Mth.clamp((float) outline.getOutlineThickness() * 3.0F, 2.0F, 14.0F);
        for (PostPass pass : ((PostChainAccessor) chain).dragonminez$getPasses()) {
            EffectInstance effect = pass.getEffect();
            String name = pass.getName();
            if ("dragonminez:transformation_unpack".equals(name)) {
                setUniform(effect, "AnimationTime", animationTime);
            } else if ("dragonminez:transformation_blur_h".equals(name)
                    || "dragonminez:transformation_blur_v".equals(name)) {
                setUniform(effect, "BloomRadius", blurRadius);
            } else if ("dragonminez:transformation_composite".equals(name)) {
                setUniform(effect, "BloomStrength", 0.95F);
                setUniform(effect, "GlowStrength", 1.35F);
            }
        }
    }

    private void setUniform(EffectInstance effect, String name, float value) {
        var uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private void closeOutlinePreviewChain() {
        if (outlinePreviewChain != null) {
            outlinePreviewChain.close();
            outlinePreviewChain = null;
        }
        outlinePreviewWidth = -1;
        outlinePreviewHeight = -1;
    }

'''
s = s[:start] + replacement + s[end:]

mouse_anchor = '''    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
'''
removed_block = '''    @Override
    public void removed() {
        closeOutlinePreviewChain();
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
'''
if mouse_anchor not in s:
    raise SystemExit('CustomFormsScreen mouse anchor not found')
s = s.replace(mouse_anchor, removed_block, 1)
screen.write_text(s)

mixins = Path('src/main/resources/unofficialdmzaddon.mixins.json')
m = mixins.read_text()
m = m.replace(',\n    "CustomFormOutlinePreviewMixin"\n', '\n')
mixins.write_text(m)

old_mixin = Path('src/main/java/org/unofficial/unofficialdmzaddon/mixin/CustomFormOutlinePreviewMixin.java')
if old_mixin.exists():
    old_mixin.unlink()

changelog = Path('changelog-v10.4.0.txt')
c = changelog.read_text()
old = "- Custom Forms Creator previews enabled outlines through Minecraft's real entity-outline pass with live outline-color changes."
new = "- Custom Forms Creator previews use DragonMineZ's native transformation outline shader, matching Ikari and saved custom forms without affecting the creator UI."
if old in c:
    c = c.replace(old, new)
elif new not in c:
    c = c.rstrip() + '\n' + new + '\n'
changelog.write_text(c)
