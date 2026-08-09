package org.unofficial.unofficialdmzaddon.client;

import net.minecraft.resources.ResourceLocation;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

public final class CustomFormsIcon {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnofficialDMZAddon.MODID, "textures/gui/custom_forms_menu.png");
    private CustomFormsIcon() {}

    public static ResourceLocation texture() { return TEXTURE; }
}
