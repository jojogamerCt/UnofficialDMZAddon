package org.unofficial.unofficialdmzaddon.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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
    private static boolean skyFilterConfigured;

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
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
                                double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }


    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        renderSpaceSky(poseStack);
        renderSolarSystem(level, poseStack, camera);
        renderPlanets(level, poseStack, camera, partialTick);
        renderMotionStreaks(level, poseStack, camera);
        return true;
    }

    private static void renderSpaceSky(PoseStack poseStack) {
        if (!skyFilterConfigured) {
            Minecraft.getInstance().getTextureManager().getTexture(SKY).setFilter(true, true);
            skyFilterConfigured = true;
        }
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


    private static void renderSolarSystem(ClientLevel level, PoseStack poseStack, Camera camera) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        Matrix4f matrix = poseStack.last().pose();
        Vec3 cameraPosition = camera.getPosition();
        Vec3 sunRelative = SpacePlanetSystem.sunPosition(player.position()).subtract(cameraPosition);

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // A soft outer glow is drawn without writing depth, then the solid cubic sun anchors the system.
        RenderSystem.depthMask(false);
        drawColoredCube(matrix, sunRelative, (float) SpacePlanetSystem.SUN_RADIUS + 8.0F, 255, 116, 18, 42);
        drawColoredCube(matrix, sunRelative, (float) SpacePlanetSystem.SUN_RADIUS + 3.5F, 255, 194, 72, 110);
        RenderSystem.depthMask(true);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SpacePlanetSystem.SUN_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 0.92F, 1.0F);
        drawCube(matrix, sunRelative, (float) SpacePlanetSystem.SUN_RADIUS);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        drawOrbitRings(matrix, player, cameraPosition);
        drawNearbyStars(matrix, player, cameraPosition);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static void drawOrbitRings(Matrix4f matrix, Player player, Vec3 cameraPosition) {
        Vec3 sun = SpacePlanetSystem.sunPosition(player.position());
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        for (SpacePlanetSystem.PlanetPlacement planet : SpacePlanetSystem.layout(player.getUUID(), player.position())) {
            double radius = planet.orbitRadius();
            for (int i = 0; i < 96; i++) {
                double a0 = Math.PI * 2.0D * i / 96.0D;
                double a1 = Math.PI * 2.0D * (i + 1) / 96.0D;
                Vec3 p0 = new Vec3(sun.x + Math.cos(a0) * radius, SpacePlanetSystem.SOLAR_PLANE_Y, sun.z + Math.sin(a0) * radius).subtract(cameraPosition);
                Vec3 p1 = new Vec3(sun.x + Math.cos(a1) * radius, SpacePlanetSystem.SOLAR_PLANE_Y, sun.z + Math.sin(a1) * radius).subtract(cameraPosition);
                buffer.vertex(matrix, (float) p0.x, (float) p0.y, (float) p0.z).color(90, 130, 205, 45).endVertex();
                buffer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(90, 130, 205, 45).endVertex();
            }
        }
        Tesselator.getInstance().end();
    }

    private static void drawNearbyStars(Matrix4f matrix, Player player, Vec3 cameraPosition) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (SpacePlanetSystem.StarPlacement star : SpacePlanetSystem.starLayout(player.getUUID(), player.position())) {
            Vec3 relative = star.position().subtract(cameraPosition);
            int rgb = star.color();
            appendColoredCube(buffer, matrix, relative, (float) star.size(),
                    (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 235);
        }
        Tesselator.getInstance().end();
    }

    private static void drawColoredCube(Matrix4f matrix, Vec3 center, float halfSize,
                                        int red, int green, int blue, int alpha) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        appendColoredCube(buffer, matrix, center, halfSize, red, green, blue, alpha);
        Tesselator.getInstance().end();
    }

    private static void appendColoredCube(BufferBuilder buffer, Matrix4f matrix, Vec3 center, float halfSize,
                                          int red, int green, int blue, int alpha) {
        float x0=(float)center.x-halfSize, x1=(float)center.x+halfSize;
        float y0=(float)center.y-halfSize, y1=(float)center.y+halfSize;
        float z0=(float)center.z-halfSize, z1=(float)center.z+halfSize;
        coloredQuad(buffer,matrix,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,red,green,blue,alpha);
        coloredQuad(buffer,matrix,x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0,red,green,blue,alpha);
        coloredQuad(buffer,matrix,x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1,red,green,blue,alpha);
        coloredQuad(buffer,matrix,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,red,green,blue,alpha);
        coloredQuad(buffer,matrix,x0,y1,z1,x1,y1,z1,x1,y1,z0,x0,y1,z0,red,green,blue,alpha);
        coloredQuad(buffer,matrix,x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1,red,green,blue,alpha);
    }

    private static void coloredQuad(BufferBuilder buffer, Matrix4f matrix,
                                    float ax,float ay,float az,float bx,float by,float bz,
                                    float cx,float cy,float cz,float dx,float dy,float dz,
                                    int red,int green,int blue,int alpha) {
        buffer.vertex(matrix,ax,ay,az).color(red,green,blue,alpha).endVertex();
        buffer.vertex(matrix,bx,by,bz).color(red,green,blue,alpha).endVertex();
        buffer.vertex(matrix,cx,cy,cz).color(red,green,blue,alpha).endVertex();
        buffer.vertex(matrix,dx,dy,dz).color(red,green,blue,alpha).endVertex();
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
            Vec3 relative = placement.position().subtract(cameraPosition);
            boolean unlocked = SpacePlanetClientState.isUnlocked(player, placement);
            float visibility = unlocked ? SpacePlanetSystem.visibility(gameTime, destroyedAt) : 1.0F;

            if (visibility > 0.001F) {
                if (unlocked) {
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderTexture(0, placement.definition().texture());
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, visibility);
                    drawCube(poseStack.last().pose(), relative, (float) placement.definition().radius());
                } else {
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    drawColoredCube(poseStack.last().pose(), relative,
                            (float) placement.definition().radius(), 0, 0, 0,
                            Math.max(0, Math.min(255, Math.round(visibility * 255.0F))));
                    drawLockedPlanetMarker(poseStack, camera, relative,
                            (float) placement.definition().radius(), visibility);
                }
            }

            long age = destroyedAt < 0L ? Long.MAX_VALUE : gameTime - destroyedAt;
            if (unlocked && age >= 0L && age < 30L) {
                float progress = age / 30.0F;
                float explosionAlpha = 1.0F - progress;
                float explosionRadius = (float) placement.definition().radius() * (1.05F + progress * 1.4F);
                drawCenteredExplosion(poseStack.last().pose(), relative, explosionRadius, explosionAlpha);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static void drawLockedPlanetMarker(PoseStack poseStack, Camera camera, Vec3 center,
                                                float planetRadius, float visibility) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float scale = Math.max(0.35F, planetRadius * 0.18F);

        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-scale, -scale, scale);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        int alpha = Math.max(0, Math.min(255, Math.round(visibility * 255.0F)));
        int color = alpha << 24 | 0x00FFFFFF;
        font.drawInBatch("?", -font.width("?") / 2.0F, -font.lineHeight / 2.0F, color, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        buffers.endBatch();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        poseStack.popPose();
    }



    private static void drawCenteredExplosion(Matrix4f matrix, Vec3 planetCenter, float radius, float alpha) {
        // Concentric cubic shells all share the exact planet-center transform, avoiding a texture hotspot
        // that made the old equirectangular explosion appear to start beside the planet.
        int outerAlpha = Math.max(0, Math.min(255, Math.round(alpha * 85.0F)));
        int middleAlpha = Math.max(0, Math.min(255, Math.round(alpha * 155.0F)));
        int coreAlpha = Math.max(0, Math.min(255, Math.round(alpha * 235.0F)));
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.depthMask(false);
        drawColoredCube(matrix, planetCenter, radius, 255, 72, 18, outerAlpha);
        drawColoredCube(matrix, planetCenter, radius * 0.72F, 255, 155, 28, middleAlpha);
        drawColoredCube(matrix, planetCenter, radius * 0.42F, 255, 242, 185, coreAlpha);
        RenderSystem.depthMask(true);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }
    private static void renderMotionStreaks(ClientLevel level, PoseStack poseStack, Camera camera) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        Vec3 velocity = player.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.006D) return;

        Vec3 direction = velocity.normalize();
        float length = (float) Math.min(24.0D, 4.0D + speed * 34.0D);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(1.5F + (float) Math.min(2.5D, speed));

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        long phase = level.getGameTime() / 2L;
        for (int i = 0; i < 72; i++) {
            long seed = i * 341873128712L + phase * 132897987541L;
            double x = (((seed >>> 8) & 255L) / 255.0D - 0.5D) * 70.0D;
            double y = (((seed >>> 24) & 255L) / 255.0D - 0.5D) * 45.0D;
            double z = (((seed >>> 40) & 255L) / 255.0D - 0.5D) * 70.0D;
            Vec3 start = new Vec3(x, y, z);
            Vec3 end = start.subtract(direction.scale(length));
            buffer.vertex(matrix, (float) start.x, (float) start.y, (float) start.z).color(145, 205, 255, 180).endVertex();
            buffer.vertex(matrix, (float) end.x, (float) end.y, (float) end.z).color(75, 100, 210, 0).endVertex();
        }
        Tesselator.getInstance().end();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static void drawCube(Matrix4f matrix, Vec3 center, float halfSize) {
        float x0 = (float) center.x - halfSize, x1 = (float) center.x + halfSize;
        float y0 = (float) center.y - halfSize, y1 = (float) center.y + halfSize;
        float z0 = (float) center.z - halfSize, z1 = (float) center.z + halfSize;
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quad(buffer, matrix, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1);
        quad(buffer, matrix, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0);
        quad(buffer, matrix, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1);
        quad(buffer, matrix, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0);
        quad(buffer, matrix, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0);
        quad(buffer, matrix, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1);
        Tesselator.getInstance().end();
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix,
                             float ax,float ay,float az, float bx,float by,float bz,
                             float cx,float cy,float cz, float dx,float dy,float dz) {
        buffer.vertex(matrix, ax,ay,az).uv(0,1).endVertex();
        buffer.vertex(matrix, bx,by,bz).uv(1,1).endVertex();
        buffer.vertex(matrix, cx,cy,cz).uv(1,0).endVertex();
        buffer.vertex(matrix, dx,dy,dz).uv(0,0).endVertex();
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
