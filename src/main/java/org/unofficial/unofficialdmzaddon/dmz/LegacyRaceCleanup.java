package org.unofficial.unofficialdmzaddon.dmz;

import net.minecraftforge.fml.loading.FMLPaths;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class LegacyRaceCleanup {
    private static final Set<String> REMOVED_RACES = Set.of(
            "beerusrace", "beerus_race", "offworlder", "offworlderrace"
    );

    private LegacyRaceCleanup() {
    }

    public static boolean isRemovedRace(String race) {
        if (race == null) return false;
        String normalized = race.toLowerCase(Locale.ROOT);
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) normalized = normalized.substring(namespaceSeparator + 1);
        return REMOVED_RACES.contains(normalized);
    }

    public static void removeLegacyConfigs() {
        Path racesDirectory = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("races");
        for (String race : REMOVED_RACES) deleteDirectory(racesDirectory.resolve(race));
    }

    private static void deleteDirectory(Path directory) {
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new LegacyCleanupException(exception);
                }
            });
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Removed obsolete race config: {}", directory);
        } catch (IOException | LegacyCleanupException exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not remove obsolete race config '{}': {}", directory, cause.getMessage());
        }
    }

    private static final class LegacyCleanupException extends RuntimeException {
        private LegacyCleanupException(Throwable cause) {
            super(cause);
        }
    }
}