package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.client.CustomFormsIcon;
import org.unofficial.unofficialdmzaddon.client.CustomFormsScreen;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

@Mixin(value = BaseMenuScreen.class, remap = false)
public abstract class BaseMenuCustomFormsMixin extends ScaledScreen {
    protected BaseMenuCustomFormsMixin() { super(Component.empty()); }
    @Shadow protected abstract void switchMenu(Screen nextScreen);

    @Inject(method = "initNavigationButtons", at = @At("TAIL"))
    private void unofficialdmzaddon$customFormsButton(CallbackInfo ci) {
        if (!UnofficialDMZConfig.CUSTOM_FORMS_ENABLED.get()) return;
        int centerX = getUiWidth() / 2;
        addRenderableWidget(new CustomTextureButton.Builder()
                .position(centerX + 130, getUiHeight() - 30)
                .size(20, 20)
                .texture(CustomFormsIcon.texture())
                .textureSize(20, 20)
                .textureCoords(0, 0, 20, 0)
                .message(Component.translatable("gui.unofficialdmzaddon.custom_forms"))
                .onPress(button -> switchMenu(new CustomFormsScreen()))
                .sound(MainSounds.UI_MENU_SWITCH.get())
                .build());
    }
}
