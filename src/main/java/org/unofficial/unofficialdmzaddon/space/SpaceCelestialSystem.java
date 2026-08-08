package org.unofficial.unofficialdmzaddon.space;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.util.List;

/**
 * The expandable deep-space hierarchy. The first release intentionally contains only one galaxy
 * and one universe; later versions can append definitions without changing travel or HUD code.
 */
public final class SpaceCelestialSystem {
    public static final Vec3 SOLAR_CENTER = new Vec3(320.0D, 160.0D, 320.0D);
    public static final Vec3 GALAXY_CENTER = SOLAR_CENTER;
    public static final Vec3 UNIVERSE_7_CENTER = SOLAR_CENTER;
    public static final double CELESTIAL_ENTRY_MARGIN = 16.0D;
    public static final double SOLAR_SYSTEM_EXIT_RADIUS = 390.0D;
    public static final double GALAXY_EXIT_RADIUS = 620.0D;

    public static final List<CelestialDefinition> CELESTIALS = List.of(
            celestial("solar_system_gateway", CelestialKind.SOLAR_SYSTEM,
                    SpaceDomain.GALAXY, SpaceDomain.SOLAR_SYSTEM,
                    GALAXY_CENTER.add(0.0D, 18.0D, -168.0D), 42.0D,
                    "solar_system"),
            celestial("universe_7_gateway", CelestialKind.UNIVERSE,
                    SpaceDomain.GALAXY, SpaceDomain.UNIVERSE_7,
                    GALAXY_CENTER.add(0.0D, 24.0D, 168.0D), 50.0D,
                    "universe_7"),
            celestial("north_galaxy_gateway", CelestialKind.GALAXY,
                    SpaceDomain.UNIVERSE_7, SpaceDomain.GALAXY,
                    UNIVERSE_7_CENTER.add(0.0D, 22.0D, -168.0D), 48.0D,
                    "galaxy_7")
    );

    private SpaceCelestialSystem() {
    }

    public static Vec3 center(ResourceKey<Level> dimension) {
        return center(SpaceDimension.domain(dimension));
    }

    public static Vec3 center(SpaceDomain domain) {
        return switch (domain) {
            case SOLAR_SYSTEM -> SOLAR_CENTER;
            case GALAXY -> GALAXY_CENTER;
            case UNIVERSE_7 -> UNIVERSE_7_CENTER;
        };
    }

    public static List<CelestialDefinition> visibleCelestials(ResourceKey<Level> dimension) {
        SpaceDomain domain = SpaceDimension.domain(dimension);
        return CELESTIALS.stream().filter(celestial -> celestial.sourceDomain() == domain).toList();
    }

    /**
     * Outward travel crosses the current domain's edge. Inward travel requires approaching the
     * rendered child system, so the hierarchy always reads Solar System -> Galaxy -> Universe
     * while leaving and Universe -> Galaxy -> Solar System while entering.
     */
    public static SpaceDomain travelTarget(ResourceKey<Level> dimension, Vec3 previousPosition,
                                           Vec3 currentPosition) {
        SpaceDomain source = SpaceDimension.domain(dimension);
        SpaceDomain inwardTarget = CELESTIALS.stream()
                .filter(celestial -> celestial.sourceDomain() == source)
                .filter(celestial -> segmentIntersects(previousPosition, currentPosition,
                        celestial.position(), celestial.radius() + CELESTIAL_ENTRY_MARGIN))
                .min(java.util.Comparator.comparingDouble(
                        celestial -> celestial.position().distanceToSqr(currentPosition)))
                .map(CelestialDefinition::targetDomain)
                .orElse(null);
        if (inwardTarget != null) return inwardTarget;

        return switch (source) {
            case SOLAR_SYSTEM -> isOutsideBoundary(currentPosition,
                    SOLAR_CENTER, SOLAR_SYSTEM_EXIT_RADIUS) ? SpaceDomain.GALAXY : null;
            case GALAXY -> isOutsideBoundary(currentPosition,
                    GALAXY_CENTER, GALAXY_EXIT_RADIUS) ? SpaceDomain.UNIVERSE_7 : null;
            case UNIVERSE_7 -> null;
        };
    }

    private static boolean isOutsideBoundary(Vec3 position, Vec3 center, double radius) {
        return position.distanceToSqr(center) > radius * radius;
    }

    private static boolean segmentIntersects(Vec3 start, Vec3 end, Vec3 center, double radius) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 1.0E-7D) return start.distanceToSqr(center) <= radius * radius;
        double progress = Math.max(0.0D, Math.min(1.0D,
                center.subtract(start).dot(segment) / lengthSquared));
        return start.add(segment.scale(progress)).distanceToSqr(center) <= radius * radius;
    }

    public static double spinAngle(CelestialDefinition celestial, double tickTime) {
        double speed = switch (celestial.kind()) {
            case SOLAR_SYSTEM -> 0.0D;
            case GALAXY -> 0.0012D;
            case UNIVERSE -> -0.00045D;
        };
        return tickTime * speed;
    }

    /** The Solar System breathes in and out instead of rotating like a galaxy. */
    public static float scalePulse(CelestialDefinition celestial, double tickTime) {
        if (celestial.kind() == CelestialKind.SOLAR_SYSTEM) {
            return 0.90F + (float) ((Math.sin(tickTime * 0.045D) + 1.0D) * 0.10D);
        }
        return 1.0F + (float) Math.sin(tickTime * 0.035D
                + celestial.position().z * 0.01D) * 0.025F;
    }

    private static CelestialDefinition celestial(String id, CelestialKind kind,
                                                  SpaceDomain sourceDomain, SpaceDomain targetDomain,
                                                  Vec3 position, double radius, String texture) {
        return celestial(id, kind, sourceDomain, targetDomain, position, radius,
                ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID,
                        "textures/environment/celestial/" + texture + ".png"));
    }

    private static CelestialDefinition celestial(String id, CelestialKind kind,
                                                  SpaceDomain sourceDomain, SpaceDomain targetDomain,
                                                  Vec3 position, double radius, ResourceLocation texture) {
        return new CelestialDefinition(id, kind, sourceDomain, targetDomain, position, radius, texture);
    }

    public enum CelestialKind {
        SOLAR_SYSTEM,
        GALAXY,
        UNIVERSE
    }

    public enum SpaceDomain {
        SOLAR_SYSTEM,
        GALAXY,
        UNIVERSE_7
    }

    public record CelestialDefinition(String id, CelestialKind kind,
                                      SpaceDomain sourceDomain, SpaceDomain targetDomain,
                                      Vec3 position, double radius, ResourceLocation texture) {
    }
}
