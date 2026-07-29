package org.unofficial.unofficialdmzaddon.space;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

public final class SpaceDimension {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID, "space");
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, ID);

    private SpaceDimension() {
    }
}