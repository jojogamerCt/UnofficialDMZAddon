from pathlib import Path

p = Path('src/main/java/org/unofficial/unofficialdmzaddon/client/CustomFormsScreen.java')
s = p.read_text()

if 'import com.mojang.blaze3d.pipeline.TextureTarget;' not in s:
    s = s.replace(
        'import com.mojang.blaze3d.platform.Lighting;\n',
        'import com.mojang.blaze3d.pipeline.TextureTarget;\nimport com.mojang.blaze3d.platform.Lighting;\n',
    )
if 'import com.mojang.blaze3d.vertex.BufferBuilder;' not in s:
    s = s.replace(
        'import com.mojang.blaze3d.systems.RenderSystem;\n',
        'import com.mojang.blaze3d.systems.RenderSystem;\n'
        'import com.mojang.blaze3d.vertex.BufferBuilder;\n'
        'import com.mojang.blaze3d.vertex.BufferUploader;\n'
        'import com.mojang.blaze3d.vertex.DefaultVertexFormat;\n'
        'import com.mojang.blaze3d.vertex.Tesselator;\n'
        'import com.mojang.blaze3d.vertex.VertexFormat;\n',
    )
if 'private TextureTarget outlinePreviewTarget;' not in s:
    s = s.replace(
        '    private PostChain outlinePreviewChain;\n    private int outlinePreviewWidth = -1;\n',
        '    private PostChain outlinePreviewChain;\n'
        '    private TextureTarget outlinePreviewTarget;\n'
        '    private int outlinePreviewWidth = -1;\n',
    )

start = s.index("    /**\n     * Renders the creator preview through DragonMineZ's transformation-mask post shader")
end = s.index('    private void resetGuiRenderState', start)
replacement = '''    /**
     * Renders the creator preview through DragonMineZ's transformation-mask post shader without
     * ever running that post-chain against Minecraft's real main framebuffer. The chain renders
     * into a private black target and only its additive RGB result is composited back into the GUI.
     * This preserves DMZ's Ikari/custom-form outline while keeping the creator UI and model intact.
     */
    private void renderOutlinePreview(GuiGraphics graphics, Player player, CustomFormDefinition preview,
                                      int centerX, int baseY, int modelScale,
                                      Quaternionf pose, Quaternionf camera, float partialTick) {
        FormConfig.FormData formData = preview.toFormData();
        FormConfig.FormData.OutlineShaderConfig outline = formData.getOutlineShader();
        if (outline == null || !outline.isEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        PostChain chain = outlinePreviewChain(minecraft);
        if (chain == null || outlinePreviewTarget == null) return;
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

        outlinePreviewTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        outlinePreviewTarget.clear(Minecraft.ON_OSX);
        try {
            maskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            maskTarget.clear(Minecraft.ON_OSX);
            maskTarget.copyDepthFrom(outlinePreviewTarget);
            TransformationMaskRenderState.setCurrentTargets(maskTarget);
            maskSource.endMaskBatch();
        } finally {
            TransformationMaskRenderState.setCurrentTargets(null);
        }

        configureOutlinePreviewUniforms(chain, outline, player.tickCount + partialTick);
        chain.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(false);
        compositeOutlinePreview(minecraft);
        resetRawRenderState();
    }

    private PostChain outlinePreviewChain(Minecraft minecraft) {
        int width = minecraft.getMainRenderTarget().width;
        int height = minecraft.getMainRenderTarget().height;
        if (outlinePreviewChain != null && outlinePreviewTarget != null
                && outlinePreviewWidth == width && outlinePreviewHeight == height) {
            return outlinePreviewChain;
        }
        closeOutlinePreviewChain();
        try {
            outlinePreviewTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            outlinePreviewTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            outlinePreviewChain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(),
                    outlinePreviewTarget,
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

    private void compositeOutlinePreview(Minecraft minecraft) {
        if (outlinePreviewTarget == null) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, outlinePreviewTarget.getColorTextureId());

        Matrix4f identity = new Matrix4f();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(identity, 0.0F, height, 0.0F).uv(0.0F, 0.0F).endVertex();
        builder.vertex(identity, width, height, 0.0F).uv(1.0F, 0.0F).endVertex();
        builder.vertex(identity, width, 0.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        builder.vertex(identity, 0.0F, 0.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
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
        if (outlinePreviewTarget != null) {
            outlinePreviewTarget.destroyBuffers();
            outlinePreviewTarget = null;
        }
        outlinePreviewWidth = -1;
        outlinePreviewHeight = -1;
    }

'''
s = s[:start] + replacement + s[end:]
p.write_text(s)

c = Path('changelog-v10.4.0.txt')
lines = c.read_text().splitlines()
lines = [line for line in lines if not line.startswith('- Custom Forms Creator outline previews') and not line.startswith('- Custom Forms Creator previews')]
insert_at = 6 if len(lines) >= 6 else len(lines)
lines.insert(insert_at, "- Custom Forms Creator outline previews run DragonMineZ's Ikari/custom-form transformation shader in a private off-screen framebuffer and add only its glow result back to the preview, leaving the player model and creator UI untouched.")
c.write_text('\n'.join(lines) + '\n')
