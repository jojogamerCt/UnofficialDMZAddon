package org.unofficial.unofficialdmzaddon.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;
import org.unofficial.unofficialdmzaddon.space.SpacePlanetSystem;

@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpaceSpecialEffects extends DimensionSpecialEffects {
    private static final int SKY_LONGITUDE_SEGMENTS = 64;
    private static final int SKY_LATITUDE_SEGMENTS = 32;
    private static final int PLANET_LONGITUDE_SEGMENTS = 32;
    private static final int PLANET_LATITUDE_SEGMENTS = 16;
    private static final float SKY_RADIUS = 100.0F;
    public static final ResourceLocation EFFECTS = ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID, "space_effects");
    private static final ResourceLocation SKY = ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID, "textures/environment/anime_space.png");

    private SpaceSpecialEffects() {
        super(Float.NaN, false, SkyType.NONE, true, true);
    }

    @SubscribeEvent
    public static void register(RegisterDimensionSpecialEffectsEvent event) {
        event.register(EFFECTS, new SpaceSpecialEffects());
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
        return new Vec3(0.004D, 0.006D, 0.018D);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        renderSpaceSky(poseStack);
        renderPlanets(level, poseStack, camera, partialTick);
        return true;
    }

    private static void renderSpaceSky(PoseStack poseStack) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SKY);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        drawSphere(poseStack.last().pose(), Vec3.ZERO, SKY_RADIUS,
                SKY_LONGITUDE_SEGMENTS, SKY_LATITUDE_SEGMENTS);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderPlanets(ClientLevel level, PoseStack poseStack, Camera camera, float partialTick) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        long gameTime = level.getGameTime();
        Vec3 cameraPosition = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        for (SpacePlanetSystem.PlanetPlacement placement : SpacePlanetSystem.layout(player.getUUID(), player.position())) {
            long destroyedAt = SpacePlanetClientState.destroyedAt(placement.index());
            float visibility = SpacePlanetSystem.visibility(gameTime, destroyedAt);
            Vec3 relative = placement.position().subtract(cameraPosition);

            if (visibility > 0.001F) {
                RenderSystem.setShaderTexture(0, placement.definition().texture());
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, visibility);
                drawSphere(poseStack.last().pose(), relative, (float) placement.definition().radius(),
                        PLANET_LONGITUDE_SEGMENTS, PLANET_LATITUDE_SEGMENTS);
            }

            long age = destroyedAt < 0L ? Long.MAX_VALUE : gameTime - destroyedAt;
            if (age >= 0L && age < 30L) {
                float progress = age / 30.0F;
                float explosionAlpha = 1.0F - progress;
                float explosionRadius = (float) placement.definition().radius() * (1.05F + progress * 1.4F);
                RenderSystem.setShaderTexture(0, SpacePlanetSystem.EXPLOSION_TEXTURE);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, explosionAlpha);
                drawSphere(poseStack.last().pose(), relative, explosionRadius,
                        PLANET_LONGITUDE_SEGMENTS, PLANET_LATITUDE_SEGMENTS);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static void drawSphere(Matrix4f matrix, Vec3 center, float radius, int longitudeSegments,
                                   int latitudeSegments) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (int latitude = 0; latitude < latitudeSegments; latitude++) {
            float v0 = (float) latitude / latitudeSegments;
            float v1 = (float) (latitude + 1) / latitudeSegments;
            double phi0 = Math.PI * (v0 - 0.5D);
            double phi1 = Math.PI * (v1 - 0.5D);

            for (int longitude = 0; longitude < longitudeSegments; longitude++) {
                float u0 = (float) longitude / longitudeSegments;
                float u1 = (float) (longitude + 1) / longitudeSegments;
                double theta0 = Math.PI * 2.0D * u0;
                double theta1 = Math.PI * 2.0D * u1;
                vertex(buffer, matrix, center, radius, theta0, phi0, u0, v0);
                vertex(buffer, matrix, center, radius, theta0, phi1, u0, v1);
                vertex(buffer, matrix, center, radius, theta1, phi1, u1, v1);
                vertex(buffer, matrix, center, radius, theta1, phi0, u1, v0);
            }
        }
        tesselator.end();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 center, float radius,
                               double theta, double phi, float u, float v) {
        float horizontal = (float) (Math.cos(phi) * radius);
        float x = (float) center.x + (float) (Math.sin(theta) * horizontal);
        float y = (float) center.y + (float) (Math.sin(phi) * radius);
        float z = (float) center.z + (float) (Math.cos(theta) * horizontal);
        buffer.vertex(matrix, x, y, z).uv(u, v).endVertex();
    }
}