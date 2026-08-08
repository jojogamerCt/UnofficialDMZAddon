package org.unofficial.unofficialdmzaddon.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.space.SpaceDimension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional, reflection-only integration with shader and LOD renderers. */
@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, value = Dist.CLIENT)
public final class SpaceRenderCompatibilityHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceRenderCompatibilityHandler.class);
    private static boolean distantHorizonsOverride;
    private static boolean warnedDistantHorizons;
    private static Method irisGetInstance;
    private static Method irisShaderPackInUse;
    private static boolean irisApiResolved;

    private SpaceRenderCompatibilityHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean inSpace = minecraft.player != null
                && minecraft.player.level().dimension().equals(SpaceDimension.KEY);
        updateDistantHorizonsOverride(inSpace && UnofficialDMZConfig.SPACE_LOD_COMPATIBILITY.get());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.player.level().dimension().equals(SpaceDimension.KEY)) return;

        boolean shaderFallback = UnofficialDMZConfig.SPACE_RENDER_COMPATIBILITY.get() && shaderPackActive();
        boolean lodFallback = UnofficialDMZConfig.SPACE_LOD_COMPATIBILITY.get()
                && (ModList.get().isLoaded("distanthorizons") || ModList.get().isLoaded("voxy"));
        if (shaderFallback || lodFallback) {
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

    private static void updateDistantHorizonsOverride(boolean shouldDisable) {
        if (!ModList.get().isLoaded("distanthorizons") || shouldDisable == distantHorizonsOverride) return;
        try {
            Class<?> delayed = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed");
            Field configsField = delayed.getField("configs");
            Object configs = configsField.get(null);
            if (configs == null) return; // DH exposes this field only after its delayed initialization event.
            Object graphics = configs.getClass().getMethod("graphics").invoke(configs);
            Object renderingEnabled = graphics.getClass().getMethod("renderingEnabled").invoke(graphics);
            if (shouldDisable) {
                Method setter = findBooleanSetter(renderingEnabled.getClass(), "setValue", "setApiValue");
                setter.invoke(renderingEnabled, Boolean.FALSE);
            } else {
                findNoArgumentMethod(renderingEnabled.getClass(), "clearValue", "clearApiValue")
                        .invoke(renderingEnabled);
            }
            distantHorizonsOverride = shouldDisable;
        } catch (ReflectiveOperationException exception) {
            if (!warnedDistantHorizons) {
                warnedDistantHorizons = true;
                LOGGER.warn("Could not apply Distant Horizons Space rendering override; using the late render fallback", exception);
            }
        }
    }

    private static Method findBooleanSetter(Class<?> type, String... names) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            for (String name : names) {
                if (method.getName().equals(name) && method.getParameterCount() == 1) return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + ".setValue");
    }

    private static Method findNoArgumentMethod(Class<?> type, String... names) throws NoSuchMethodException {
        for (String name : names) {
            try {
                return type.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(type.getName() + ".clearValue");
    }
}
