from pathlib import Path

path = Path("src/main/java/org/unofficial/unofficialdmzaddon/client/CustomFormsScreen.java")
text = path.read_text()

if "import net.minecraft.client.renderer.OutlineBufferSource;" not in text:
    replacements = [
        (
            "import com.mojang.blaze3d.systems.RenderSystem;\n",
            "import com.mojang.blaze3d.platform.Lighting;\nimport com.mojang.blaze3d.systems.RenderSystem;\n",
        ),
        (
            "import net.minecraft.client.renderer.GameRenderer;\n",
            "import net.minecraft.client.renderer.GameRenderer;\nimport net.minecraft.client.renderer.OutlineBufferSource;\nimport net.minecraft.client.renderer.entity.EntityRenderDispatcher;\n",
        ),
        (
            "import org.joml.Quaternionf;\n",
            "import org.joml.Quaternionf;\nimport org.joml.Matrix4f;\n",
        ),
        (
            "renderOutlinePreview(graphics, player, preview, centerX, baseY, scale, partialTick);",
            "renderOutlinePreview(graphics, player, preview, centerX, baseY, scale, pose, camera);",
        ),
    ]
    for old, new in replacements:
        count = text.count(old)
        if count != 1:
            raise RuntimeError(f"Expected exactly one occurrence of {old!r}, found {count}")
        text = text.replace(old, new, 1)

    start = text.index("    /**\n     * The world outline is a post-process applied to the main framebuffer")
    end = text.index("    private void resetGuiRenderState", start)
    method = '''    /** Renders the preview model through Minecraft's real entity-outline buffer. */
    private void renderOutlinePreview(GuiGraphics graphics, Player player, CustomFormDefinition preview,
                                      int centerX, int baseY, int modelScale,
                                      Quaternionf pose, Quaternionf camera) {
        float[] rgb = ColorUtils.hexToRgb(preview.auraOutlineColor());
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        OutlineBufferSource outlines = minecraft.renderBuffers().outlineBufferSource();

        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, baseY, 50.0D);
        graphics.pose().mulPoseMatrix(new Matrix4f().scaling(modelScale, modelScale, -modelScale));
        graphics.pose().mulPose(new Quaternionf(pose));
        Lighting.setupForEntityInInventory();
        dispatcher.overrideCameraOrientation(new Quaternionf(camera).conjugate());
        dispatcher.setRenderShadow(false);
        outlines.setColor(
                Mth.clamp(Math.round(rgb[0] * 255.0F), 0, 255),
                Mth.clamp(Math.round(rgb[1] * 255.0F), 0, 255),
                Mth.clamp(Math.round(rgb[2] * 255.0F), 0, 255),
                255);
        try {
            dispatcher.render(player, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F,
                    graphics.pose(), outlines, 0xF000F0);
            outlines.endOutlineBatch();
        } finally {
            dispatcher.setRenderShadow(true);
            graphics.pose().popPose();
            Lighting.setupFor3DItems();
        }
    }

'''
    text = text[:start] + method + text[end:]
    path.write_text(text)
