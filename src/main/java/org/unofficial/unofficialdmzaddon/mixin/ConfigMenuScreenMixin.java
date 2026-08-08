package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.character.ConfigMenuScreen;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;
import org.unofficial.unofficialdmzaddon.client.HalalModeConfigAccess;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = ConfigMenuScreen.class, remap = false)
public abstract class ConfigMenuScreenMixin {
    @Shadow @Final private List<Object> configOptions;
    @Shadow private int scrollOffset;
    @Unique private int unofficialdmzaddon$halalOptionIndex = -1;

    @Inject(method = "initializeConfigOptions", at = @At("TAIL"))
    private void unofficialdmzaddon$addHalalMode(CallbackInfo ci) {
        try {
            Class<?> typeClass = Class.forName("com.dragonminez.client.gui.character.ConfigMenuScreen$ConfigType");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object booleanType = Enum.valueOf((Class<? extends Enum>) typeClass, "BOOLEAN");
            Class<?> optionClass = Class.forName("com.dragonminez.client.gui.character.ConfigMenuScreen$ConfigOption");
            Constructor<?> constructor = optionClass.getDeclaredConstructor(
                    String.class, typeClass, float.class, float.class, float.class, Consumer.class);
            constructor.setAccessible(true);
            HalalModeConfigAccess access = (HalalModeConfigAccess) (Object) ConfigManager.getUserConfig();
            Consumer<Float> setter = value -> access.unofficialdmzaddon$setHalalMode(value > 0.0F);
            Object option = constructor.newInstance("config.halalMode", booleanType,
                    access.unofficialdmzaddon$isHalalMode() ? 1.0F : 0.0F, 0.0F, 1.0F, setter);
            // Keep the camera action last, just like DragonMineZ does for its own options.
            unofficialdmzaddon$halalOptionIndex = Math.max(0, configOptions.size() - 1);
            configOptions.add(unofficialdmzaddon$halalOptionIndex, option);
        } catch (ReflectiveOperationException | ClassCastException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not add Halal Mode to DMZ settings: {}", e.getMessage());
        }
    }

    @Inject(method = "renderConfigsList", at = @At("TAIL"))
    private void unofficialdmzaddon$renderHalalSubtitle(GuiGraphics graphics, int panelX, int panelY,
                                                         int mouseX, int mouseY, CallbackInfo ci) {
        if (unofficialdmzaddon$halalOptionIndex < scrollOffset
                || unofficialdmzaddon$halalOptionIndex >= scrollOffset + 7) return;
        int itemY = panelY + 35 + (unofficialdmzaddon$halalOptionIndex - scrollOffset) * 20;
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 0.5F);
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable("gui.dragonminez.config.halalMode.subtitle"),
                (panelX + 15) * 2, (itemY + 13) * 2, 0xFFB8B8B8, true);
        graphics.pose().popPose();
    }
}
