package org.unofficial.unofficialdmzaddon.space;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Shared deterministic layout and collision math used by both logical sides. */
public final class SpacePlanetSystem {
    public static final long RESPAWN_TICKS = 20L * 60L * 5L;
    public static final long FADE_TICKS = 40L;
    public static final double REGION_SIZE = 128.0D;
    public static final ResourceLocation EXPLOSION_TEXTURE = texture("planet_explosion");

    public static final List<SpacePlanetDefinition> PLANETS = List.of(
            planet("earth", "Earth", "minecraft:overworld", 12.0D),
            planet("namek", "Namek", "dragonminez:namek", 9.0D),
            planet("otherworld", "Otherworld", "dragonminez:otherworld", 10.5D),
            planet("sacred_kai", "Sacred Kai's Planet", "dragonminez:sacredkaiplanet", 6.0D),
            planet("time_chamber", "Time Chamber", "dragonminez:time_chamber", 7.5D)
    );

    private SpacePlanetSystem() {
    }

    public static List<PlanetPlacement> layout(UUID playerId, Vec3 playerPosition) {
        long regionX = floorRegion(playerPosition.x);
        long regionZ = floorRegion(playerPosition.z);
        double centerX = (regionX + 0.5D) * REGION_SIZE;
        double centerZ = (regionZ + 0.5D) * REGION_SIZE;
        long seed = playerId.getMostSignificantBits() ^ Long.rotateLeft(playerId.getLeastSignificantBits(), 19)
                ^ regionX * 341873128712L ^ regionZ * 132897987541L;
        Random random = new Random(seed);
        List<PlanetPlacement> result = new ArrayList<>(PLANETS.size());

        for (int index = 0; index < PLANETS.size(); index++) {
            SpacePlanetDefinition definition = PLANETS.get(index);
            Vec3 position = null;
            for (int attempt = 0; attempt < 64; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = 38.0D + random.nextDouble() * 24.0D;
                double y = 144.0D + random.nextDouble() * 34.0D;
                Vec3 candidate = new Vec3(centerX + Math.cos(angle) * distance, y,
                        centerZ + Math.sin(angle) * distance);
                if (isSeparated(candidate, definition.radius(), result)) {
                    position = candidate;
                    break;
                }
            }
            if (position == null) {
                double angle = index * (Math.PI * 2.0D / PLANETS.size());
                position = new Vec3(centerX + Math.cos(angle) * 58.0D, 148.0D + index * 5.0D,
                        centerZ + Math.sin(angle) * 58.0D);
            }
            result.add(new PlanetPlacement(index, definition, position));
        }
        return result;
    }

    public static boolean segmentIntersects(Vec3 start, Vec3 end, Vec3 center, double radius) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 1.0E-7D) {
            return start.distanceToSqr(center) <= radius * radius;
        }
        double t = Math.max(0.0D, Math.min(1.0D, center.subtract(start).dot(segment) / lengthSquared));
        return start.add(segment.scale(t)).distanceToSqr(center) <= radius * radius;
    }

    public static float visibility(long currentTick, long destroyedAt) {
        if (destroyedAt < 0L) return 1.0F;
        long age = currentTick - destroyedAt;
        if (age < FADE_TICKS) return Math.max(0.0F, 1.0F - age / (float) FADE_TICKS);
        long fadeInStart = RESPAWN_TICKS - FADE_TICKS;
        if (age >= fadeInStart) return Math.min(1.0F, (age - fadeInStart) / (float) FADE_TICKS);
        return 0.0F;
    }

    public static boolean isDestroyed(long currentTick, long destroyedAt) {
        return destroyedAt >= 0L && currentTick - destroyedAt < RESPAWN_TICKS;
    }

    private static boolean isSeparated(Vec3 candidate, double radius, List<PlanetPlacement> placements) {
        for (PlanetPlacement placed : placements) {
            double minimum = radius + placed.definition().radius() + 18.0D;
            if (candidate.distanceToSqr(placed.position()) < minimum * minimum) return false;
        }
        return true;
    }

    private static long floorRegion(double coordinate) {
        return (long) Math.floor(coordinate / REGION_SIZE);
    }

    private static SpacePlanetDefinition planet(String id, String name, String dimension, double radius) {
        return new SpacePlanetDefinition(id, name, ResourceLocation.parse(dimension), radius, texture(id));
    }

    private static ResourceLocation texture(String id) {
        return ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID,
                "textures/environment/planets/" + id + ".png");
    }

    public record PlanetPlacement(int index, SpacePlanetDefinition definition, Vec3 position) {
    }
}