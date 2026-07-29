package org.unofficial.unofficialdmzaddon.space;

import net.minecraft.resources.ResourceLocation;

/** A visible space body backed by a dimension that is currently implemented. */
public record SpacePlanetDefinition(String id, String name, ResourceLocation dimension, double radius,
                                    ResourceLocation texture) {
}