package org.unofficial.unofficialdmzaddon.space;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Shared deterministic solar-system layout and collision math used by both logical sides. */
public final class SpacePlanetSystem {
    public static final long RESPAWN_TICKS = 20L * 60L * 5L;
    public static final long FADE_TICKS = 40L;
    public static final double REGION_SIZE = 640.0D;
    public static final double SOLAR_PLANE_Y = 160.0D;
    public static final double SUN_RADIUS = 16.0D;
    public static final ResourceLocation EXPLOSION_TEXTURE = texture("planet_explosion");
    public static final ResourceLocation SUN_TEXTURE = texture("sun");

    private static final double[] ORBIT_RADII = {96.0D, 168.0D, 240.0D};
    private static final double[] ORBIT_HEIGHTS = {0.0D, 12.0D, -10.0D};
    private static final double[] ORBIT_SPEEDS = {0.00045D, 0.00028D, 0.00018D};
    private static final double[] SPIN_SPEEDS = {0.015D, 0.011D, 0.008D};

    public static final List<SpacePlanetDefinition> PLANETS = List.of(
            planet("earth", "Earth", "minecraft:overworld", 8.5D),
            planet("namek", "Namek", "dragonminez:namek", 6.5D),
            planet("sacred_kai", "Sacred Kai's Planet", "dragonminez:sacredkaiplanet", 4.5D)
    );

    private SpacePlanetSystem() {
    }

    /** Planets occupy stable, separated orbital bands around the sector sun. */
    public static List<PlanetPlacement> layout(Vec3 playerPosition, double tickTime) {
        Vec3 sun = sunPosition(playerPosition);
        long regionX = floorRegion(playerPosition.x);
        long regionZ = floorRegion(playerPosition.z);
        // The system is shared in multiplayer. Progression remains per-player, but every client sees
        // the same planets in the same place instead of receiving a UUID-specific solar system.
        long seed = solarSeed(regionX, regionZ);
        Random random = new Random(seed);
        double baseAngle = random.nextDouble() * Math.PI * 2.0D;
        List<PlanetPlacement> result = new ArrayList<>(PLANETS.size());

        for (int index = 0; index < PLANETS.size(); index++) {
            double initialAngle = baseAngle + index * 2.18D + random.nextDouble() * 0.28D;
            double angle = initialAngle + tickTime * ORBIT_SPEEDS[Math.min(index, ORBIT_SPEEDS.length - 1)];
            double spin = tickTime * SPIN_SPEEDS[Math.min(index, SPIN_SPEEDS.length - 1)] + initialAngle;
            double orbit = ORBIT_RADII[Math.min(index, ORBIT_RADII.length - 1)];
            Vec3 position = new Vec3(
                    sun.x + Math.cos(angle) * orbit,
                    SOLAR_PLANE_Y + ORBIT_HEIGHTS[Math.min(index, ORBIT_HEIGHTS.length - 1)],
                    sun.z + Math.sin(angle) * orbit);
            result.add(new PlanetPlacement(index, PLANETS.get(index), position, orbit,
                    wrapRadians(angle), wrapRadians(spin)));
        }
        return result;
    }

    public static Vec3 sunPosition(Vec3 playerPosition) {
        long regionX = floorRegion(playerPosition.x);
        long regionZ = floorRegion(playerPosition.z);
        return new Vec3((regionX + 0.5D) * REGION_SIZE, SOLAR_PLANE_Y,
                (regionZ + 0.5D) * REGION_SIZE);
    }

    /** Nearby fixed stars provide parallax, making even slow movement obvious. */
    public static List<StarPlacement> starLayout(Vec3 playerPosition) {
        long regionX = floorRegion(playerPosition.x);
        long regionZ = floorRegion(playerPosition.z);
        Vec3 sun = sunPosition(playerPosition);
        Random random = new Random(solarSeed(regionX, regionZ) ^ 0x5DEECE66DL);
        List<StarPlacement> stars = new ArrayList<>(180);
        for (int i = 0; i < 180; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 24.0D + random.nextDouble() * 118.0D;
            double y = 105.0D + random.nextDouble() * 112.0D;
            double size = 0.10D + random.nextDouble() * 0.34D;
            int palette = random.nextInt(10);
            int color = palette < 6 ? 0xEAF6FF : palette < 8 ? 0x9CCBFF : palette == 8 ? 0xFFD89A : 0xD4B0FF;
            Vec3 position = new Vec3(sun.x + Math.cos(angle) * distance, y,
                    sun.z + Math.sin(angle) * distance);
            stars.add(new StarPlacement(position, size, color));
        }
        return stars;
    }

    public static boolean segmentIntersects(Vec3 start, Vec3 end, Vec3 center, double radius) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 1.0E-7D) return start.distanceToSqr(center) <= radius * radius;
        double t = Math.max(0.0D, Math.min(1.0D, center.subtract(start).dot(segment) / lengthSquared));
        return start.add(segment.scale(t)).distanceToSqr(center) <= radius * radius;
    }

    /** Segment versus axis-aligned cube test matching the rendered planet geometry. */
    public static boolean segmentIntersectsCube(Vec3 start, Vec3 end, Vec3 center, double halfExtent) {
        Vec3 direction = end.subtract(start);
        double minimum = 0.0D;
        double maximum = 1.0D;
        double[] origins = {start.x - center.x, start.y - center.y, start.z - center.z};
        double[] deltas = {direction.x, direction.y, direction.z};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(deltas[axis]) < 1.0E-8D) {
                if (origins[axis] < -halfExtent || origins[axis] > halfExtent) return false;
                continue;
            }
            double first = (-halfExtent - origins[axis]) / deltas[axis];
            double second = (halfExtent - origins[axis]) / deltas[axis];
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            minimum = Math.max(minimum, first);
            maximum = Math.min(maximum, second);
            if (minimum > maximum) return false;
        }
        return true;
    }

    /** Segment versus a cube spinning around its Y axis. */
    public static boolean segmentIntersectsRotatedCube(Vec3 start, Vec3 end, Vec3 center,
                                                       double halfExtent, double yawRadians) {
        return segmentIntersectsCube(rotateAroundY(start, center, -yawRadians),
                rotateAroundY(end, center, -yawRadians), center, halfExtent);
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

    private static long floorRegion(double coordinate) {
        return (long) Math.floor(coordinate / REGION_SIZE);
    }

    private static long solarSeed(long regionX, long regionZ) {
        return 0x44D5241474C4158L ^ regionX * 341873128712L ^ regionZ * 132897987541L;
    }

    private static Vec3 rotateAroundY(Vec3 point, Vec3 center, double angle) {
        double x = point.x - center.x;
        double z = point.z - center.z;
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        return new Vec3(center.x + x * cosine - z * sine, point.y,
                center.z + x * sine + z * cosine);
    }

    private static double wrapRadians(double angle) {
        double fullTurn = Math.PI * 2.0D;
        double wrapped = angle % fullTurn;
        return wrapped < 0.0D ? wrapped + fullTurn : wrapped;
    }

    private static SpacePlanetDefinition planet(String id, String name, String dimension, double radius) {
        return new SpacePlanetDefinition(id, name, ResourceLocation.parse(dimension), radius, texture(id));
    }

    private static ResourceLocation texture(String id) {
        return ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID,
                "textures/environment/planets/" + id + ".png");
    }

    public record PlanetPlacement(int index, SpacePlanetDefinition definition, Vec3 position, double orbitRadius,
                                  double orbitAngle, double spinAngle) {
    }

    public record StarPlacement(Vec3 position, double size, int color) {
    }
}
