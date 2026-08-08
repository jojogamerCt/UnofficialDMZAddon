package org.unofficial.unofficialdmzaddon.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.space.SpaceDimension;

import java.lang.reflect.Method;

/** Optional, reflection-only integration with shader renderers. */
@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, value = Dist.CLIENT)
public final class SpaceRenderCompatibilityHandler {
    private static Method irisGetInstance;
    private static Method irisShaderPackInUse;
    private static boolean irisApiResolved;

    private SpaceRenderCompatibilityHandler() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.player.level().dimension().equals(SpaceDimension.KEY)) return;

        if (UnofficialDMZConfig.SPACE_RENDER_COMPATIBILITY.get() && shaderPackActive()) {
            SpaceSpecialEffects.renderCompatibilityPass(minecraft.level, event.getPoseStack(),
                    event.getCamera(), event.getPartialTick());
        }
    }

    private static boolean shaderPackActive() {
        if (!irisApiResolved) resolveIrisApi();
        if (irisGetInstance != null && irisShaderPackInUse != null) {
            try {
                Object api = irisGetInstance.invoke(null);
                return Boolean.TRUE.equals(irisShaderPackInUse.invoke(api));
            } catch (ReflectiveOperationException ignored) {
                // Fall through to conservative mod detection.
            }
        }
        return ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris") || optiFinePresent();
    }

    private static void resolveIrisApi() {
        irisApiResolved = true;
        for (String className : new String[]{"net.irisshaders.iris.api.v0.IrisApi",
                "net.coderbot.iris.api.v0.IrisApi"}) {
            try {
                Class<?> apiClass = Class.forName(className, false,
                        SpaceRenderCompatibilityHandler.class.getClassLoader());
                irisGetInstance = apiClass.getMethod("getInstance");
                irisShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
                return;
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            }
        }
    }

    private static boolean optiFinePresent() {
        try {
            Class.forName("net.optifine.shaders.Shaders", false,
                    SpaceRenderCompatibilityHandler.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

}
