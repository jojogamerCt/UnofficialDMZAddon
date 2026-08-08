package org.unofficial.unofficialdmzaddon.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.space.SpaceDimension;
import org.unofficial.unofficialdmzaddon.space.SpacePlanetSystem;

import java.util.Comparator;

/** Crosshair-targeted readout for the currently observed planet. */
@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpacePlanetHud {
    private static final int PANEL_WIDTH = 236;

    private SpacePlanetHud() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("space_planet_info", SpacePlanetHud::render);
    }

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui forgeGui, GuiGraphics graphics,
                               float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (!UnofficialDMZConfig.SPACE_PLANET_HUD.get() || player == null || minecraft.options.hideGui
                || minecraft.screen != null || !player.level().dimension().equals(SpaceDimension.KEY)) return;

        Camera camera = minecraft.gameRenderer.getMainCamera();
        SpacePlanetSystem.PlanetPlacement target = targetedPlanet(player, camera, partialTick);
        if (target == null) return;

        boolean unlocked = SpacePlanetClientState.isUnlocked(player, target);
        int width = Math.min(PANEL_WIDTH, screenWidth - 16);
        int x = screenWidth - width - 8;
        int y = Math.max(8, screenHeight / 10);
        int height = unlocked ? 132 : 72;
        Font font = minecraft.font;

        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xCC88B8FF);
        graphics.fill(x, y, x + width, y + height, 0xDC07101F);
        graphics.fill(x, y, x + 4, y + height, unlocked ? 0xFF55D6FF : 0xFFFFFFFF);

        String prefix = "planet.unofficialdmzaddon." + target.definition().id();
        Component title = unlocked ? Component.translatable(prefix + ".name")
                : Component.translatable("planet.unofficialdmzaddon.unknown");
        graphics.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), x + 10, y + 8,
                unlocked ? 0xFF8CE7FF : 0xFFFFFFFF, true);

        int lineY = y + 23;
        if (!unlocked) {
            drawLine(graphics, font, x, lineY, "hud.unofficialdmzaddon.planet.access",
                    Component.translatable("hud.unofficialdmzaddon.planet.locked").withStyle(ChatFormatting.RED));
            drawLine(graphics, font, x, lineY + 13, "hud.unofficialdmzaddon.planet.analysis",
                    Component.translatable("hud.unofficialdmzaddon.planet.unavailable"));
            graphics.drawString(font, Component.translatable("hud.unofficialdmzaddon.planet.unlock_hint"),
                    x + 10, lineY + 30, 0xFFB8C5D9, false);
            return;
        }

        double distance = camera.getPosition().distanceTo(target.position());
        drawLine(graphics, font, x, lineY, "hud.unofficialdmzaddon.planet.type",
                Component.translatable(prefix + ".type"));
        drawLine(graphics, font, x, lineY + 13, "hud.unofficialdmzaddon.planet.access",
                Component.translatable("hud.unofficialdmzaddon.planet.unlocked").withStyle(ChatFormatting.GREEN));
        drawLine(graphics, font, x, lineY + 26, "hud.unofficialdmzaddon.planet.distance",
                Component.literal(Math.round(distance) + " blocks"));
        drawLine(graphics, font, x, lineY + 39, "hud.unofficialdmzaddon.planet.orbit",
                Component.literal(Math.round(target.orbitRadius()) + " blocks"));
        drawLine(graphics, font, x, lineY + 52, "hud.unofficialdmzaddon.planet.atmosphere",
                Component.translatable(prefix + ".atmosphere"));
        drawLine(graphics, font, x, lineY + 65, "hud.unofficialdmzaddon.planet.environment",
                Component.translatable(prefix + ".environment"));
        drawLine(graphics, font, x, lineY + 78, "hud.unofficialdmzaddon.planet.destination",
                Component.literal(target.definition().dimension().toString()));
        graphics.drawString(font, Component.translatable("hud.unofficialdmzaddon.planet.travel_hint"),
                x + 10, lineY + 95, 0xFF9FC7E8, false);
    }

    private static void drawLine(GuiGraphics graphics, Font font, int x, int y, String labelKey,
                                 Component value) {
        Component line = Component.translatable(labelKey).append(Component.literal(": ")).append(value);
        graphics.drawString(font, line, x + 10, y, 0xFFE8F1FF, false);
    }

    private static SpacePlanetSystem.PlanetPlacement targetedPlanet(Player player, Camera camera,
                                                                    float partialTick) {
        Vec3 origin = camera.getPosition();
        Vector3f cameraLook = camera.getLookVector();
        Vec3 look = new Vec3(cameraLook.x(), cameraLook.y(), cameraLook.z()).normalize();
        double tickTime = player.level().getGameTime() + partialTick;
        return SpacePlanetSystem.layout(player.position(), tickTime).stream()
                .filter(placement -> intersectsView(origin, look, placement))
                .min(Comparator.comparingDouble(placement -> placement.position().distanceToSqr(origin)))
                .orElse(null);
    }

    private static boolean intersectsView(Vec3 origin, Vec3 look,
                                          SpacePlanetSystem.PlanetPlacement placement) {
        Vec3 offset = placement.position().subtract(origin);
        double forward = offset.dot(look);
        if (forward <= 0.0D || forward > 512.0D) return false;
        double perpendicularSquared = offset.lengthSqr() - forward * forward;
        double targetRadius = placement.definition().radius() * 1.45D;
        return perpendicularSquared <= targetRadius * targetRadius;
    }
}
