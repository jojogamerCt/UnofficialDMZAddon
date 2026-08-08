package org.unofficial.unofficialdmzaddon.space;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

public final class SpaceDimension {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID, "space");
    public static final ResourceLocation GALAXY_ID = ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID, "galaxy_space");
    public static final ResourceLocation UNIVERSE_7_ID = ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID, "universe_7_space");
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, ID);
    public static final ResourceKey<Level> GALAXY_KEY = ResourceKey.create(Registries.DIMENSION, GALAXY_ID);
    public static final ResourceKey<Level> UNIVERSE_7_KEY = ResourceKey.create(Registries.DIMENSION, UNIVERSE_7_ID);

    private SpaceDimension() {
    }

    public static boolean isSpace(ResourceKey<Level> dimension) {
        return dimension.equals(KEY) || dimension.equals(GALAXY_KEY) || dimension.equals(UNIVERSE_7_KEY);
    }

    public static boolean isSolarSpace(ResourceKey<Level> dimension) {
        return dimension.equals(KEY);
    }

    public static SpaceCelestialSystem.SpaceDomain domain(ResourceKey<Level> dimension) {
        if (dimension.equals(GALAXY_KEY)) return SpaceCelestialSystem.SpaceDomain.GALAXY;
        if (dimension.equals(UNIVERSE_7_KEY)) return SpaceCelestialSystem.SpaceDomain.UNIVERSE_7;
        return SpaceCelestialSystem.SpaceDomain.SOLAR_SYSTEM;
    }

    public static ResourceKey<Level> key(SpaceCelestialSystem.SpaceDomain domain) {
        return switch (domain) {
            case SOLAR_SYSTEM -> KEY;
            case GALAXY -> GALAXY_KEY;
            case UNIVERSE_7 -> UNIVERSE_7_KEY;
        };
    }
}
