package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Registers the Alien race in the DMZ config system at startup.
 *
 * <p>Alien characteristics:
 * <ul>
 *   <li>No hair customisation (bald by default, hairType preset 0 = no hair)</li>
 *   <li>No gender distinction</li>
 *   <li>3 body types (0, 1, 2)</li>
 *   <li>1 eye type (0) – DMZ's bump-to-1 cycles back to type 0 since no type 1 texture exists</li>
 *   <li>1 nose type (0)</li>
 *   <li>1 mouth type (0)</li>
 * </ul>
 *
 * <p>Texture paths expected under {@code assets/dragonminez/textures/entity/races/alien/}:
 * <ul>
 *   <li>Body: {@code bodytype_{0|1|2}_layer{1|2|3}.png} → 9 textures</li>
 *   <li>Eyes: {@code faces/alien_eye_0_{0|1|2|3}.png} → 4 textures (layers only, 1 eye type)</li>
 *   <li>Nose: {@code faces/alien_nose_0.png} → 1 texture</li>
 *   <li>Mouth: {@code faces/alien_mouth_0.png} → 1 texture</li>
 * </ul>
 * Total: <b>14 textures to create in Photoshop.</b>
 */
public final class AlienRaceInstaller {

    static final String ALIEN_RACE = SpecialRaceFormsDefinitions.ALIEN_RACE;
    static final String ALIEN_DEFAULT_EYE_COLOR = "#FFFFFF";

    private static final int[] ALIEN_SUPERFORM_DEFAULT_COSTS = new int[]{20000, 80000, 120000, 160000};

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AlienRaceInstaller() {
    }

    public static void install() {
        boolean characterOk = ensureAlienCharacterConfig();
        boolean statsOk     = ensureAlienStatsConfig();
        boolean runtimeOk   = injectAlienCharacterIntoRuntime();

        if (characterOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Alien race character.json written.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not write Alien race character.json.");
        }

        if (statsOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Alien race stats.json written.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not write Alien race stats.json.");
        }

        if (runtimeOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Alien race injected into DMZ runtime registry.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not inject Alien race into DMZ runtime registry.");
        }
    }

    // ── character.json ────────────────────────────────────────────────────────

    private static boolean ensureAlienCharacterConfig() {
        Path file = raceDir().resolve("character.json");
        try {
            Files.createDirectories(file.getParent());

            // Always overwrite so DMZ's default config (canUseHair=true) cannot persist.
            JsonObject root = buildCharacterJson();
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            return true;
        } catch (Exception e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Failed writing Alien character.json: {}", e.getMessage());
            return false;
        }
    }

    private static JsonObject buildCharacterJson() {
        JsonObject root = new JsonObject();

        root.addProperty("canUseHair",       false);
        root.addProperty("defaultHairType",  0);     // preset 0 = no hair (bald)
        root.addProperty("hasGender",        false);
        root.addProperty("useVanillaSkin",   false);
        root.addProperty("customModel",      "");

        root.addProperty("defaultBodyType",  0);
        root.addProperty("defaultEyesType",  0);
        root.addProperty("defaultNoseType",  0);
        root.addProperty("defaultMouthType", 0);
        root.addProperty("defaultTattooType",0);

        root.addProperty("defaultBodyColor",  "#D4CECC");  // Jiren's pale grey skin (DBS)
        root.addProperty("defaultBodyColor2", "#B0AAAA");
        root.addProperty("defaultBodyColor3", "#8C8888");
        root.addProperty("defaultHairColor",  "#000000");
        root.addProperty("defaultEye1Color",  ALIEN_DEFAULT_EYE_COLOR);  // lighter black
        root.addProperty("defaultEye2Color",  ALIEN_DEFAULT_EYE_COLOR);  // lighter black

        // Superform TP costs – same count as Namekian (4 levels).
        JsonArray costs = new JsonArray();
        for (int cost : ALIEN_SUPERFORM_DEFAULT_COSTS) {
            costs.add(cost);
        }
        root.add("superformTpCost", costs);

        return root;
    }

    // ── stats.json ────────────────────────────────────────────────────────────

    private static boolean ensureAlienStatsConfig() {
        Path file = raceDir().resolve("stats.json");
        try {
            Files.createDirectories(file.getParent());

            if (Files.exists(file)) {
                return true;
            }

            JsonObject root = buildStatsJson();
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            return true;
        } catch (Exception e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Failed writing Alien stats.json: {}", e.getMessage());
            return false;
        }
    }

    private static JsonObject buildStatsJson() {
        JsonObject root = new JsonObject();

        // Base multipliers – balanced for a physically dominant race.
        root.addProperty("strBase",   1.20);
        root.addProperty("skpBase",   1.00);
        root.addProperty("stmBase",   1.10);
        root.addProperty("defBase",   1.30);
        root.addProperty("vitBase",   1.15);
        root.addProperty("pwrBase",   1.10);
        root.addProperty("eneBase",   0.90);
        root.addProperty("speedBase", 1.05);

        root.addProperty("baseHealth",  20.0);
        root.addProperty("baseEnergy",  2000.0);
        root.addProperty("baseStamina", 100.0);

        return root;
    }

    // ── Runtime injection ─────────────────────────────────────────────────────

    /**
     * Applies the alien character config to the DMZ runtime so the race is
     * immediately usable without a world restart if the config files are new.
     */
    private static boolean injectAlienCharacterIntoRuntime() {
        try {
            RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(ALIEN_RACE);
            if (raceConfig == null) {
                // DMZ may load the race later via loadAllRaces(); that is fine.
                return true;
            }

            // Enforce the no-hair / no-gender / correct-colour constraints at
            // runtime in case an older character.json was loaded with wrong defaults.
            raceConfig.setCanUseHair(false);
            raceConfig.setDefaultHairType(0);     // preset 0 = no hair (bald)
            raceConfig.setHasGender(false);
            raceConfig.setDefaultEye1Color(ALIEN_DEFAULT_EYE_COLOR);
            raceConfig.setDefaultEye2Color(ALIEN_DEFAULT_EYE_COLOR);
            raceConfig.setDefaultBodyColor("#D4CECC");

            int[] costs = raceConfig.getSuperformTpCost();
            if (costs == null || costs.length < ALIEN_SUPERFORM_DEFAULT_COSTS.length) {
                raceConfig.setSuperformTpCost(ALIEN_SUPERFORM_DEFAULT_COSTS);
            }

            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Alien runtime injection failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Path raceDir() {
        return FMLPaths.CONFIGDIR.get()
                .resolve("dragonminez")
                .resolve("races")
                .resolve(ALIEN_RACE);
    }
}
