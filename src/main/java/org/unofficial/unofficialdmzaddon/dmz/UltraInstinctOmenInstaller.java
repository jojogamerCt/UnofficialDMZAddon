package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UltraInstinctOmenInstaller {

    private static final String GROUP_ULTRA_INSTINCT = UltraInstinctDefinitions.GROUP_NAME;
    private static final String GROUP_LEGACY_SUPER_SAIYAN = "supersaiyan";
    private static final String FORM_TYPE_SUPER = "super";

    private static final int[] SAIYAN_SUPERFORM_DEFAULT_COSTS = new int[]{20000, 40000, 60000, 80000, 100000, 120000, 140000, 160000};
    private static final int[] NAMEKIAN_SUPERFORM_DEFAULT_COSTS = new int[]{20000, 80000, 120000, 160000};
    private static final int[] FROST_DEMON_SUPERFORM_DEFAULT_COSTS = new int[]{20000, 80000, 120000, 160000, 200000, 240000};
    private static final int[] ALIEN_SUPERFORM_DEFAULT_COSTS = new int[]{20000, 80000, 120000, 160000};

    private static final List<String> UI_FORM_KEYS = List.of(
            UltraInstinctDefinitions.LEGACY_FORM_OMEN,
            UltraInstinctDefinitions.FORM_SIGN,
            UltraInstinctDefinitions.FORM_MASTERED,
            UltraInstinctDefinitions.FORM_AUTONOMOUS,
            UltraInstinctDefinitions.FORM_TRUE
    );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private UltraInstinctOmenInstaller() {
    }

    public static void install() {
        boolean uiRuntimeOk = injectUltraInstinctIntoRuntimeFormRegistry();
        boolean uiFileOk = persistUltraInstinctFormFile();
        boolean uiCleanupOk = cleanupLegacySuperSaiyanUltraInstinct();

        boolean specialRuntimeOk = injectSpecialRaceFormsIntoRuntimeFormRegistry();
        boolean specialFilesOk = persistSpecialRaceFormFiles();
        boolean raceCapacityOk = ensureSpecialRaceProgressionCapacities();

        if (uiRuntimeOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Installed progressive Ultra Instinct forms in DMZ runtime registry.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not inject Ultra Instinct forms in DMZ runtime registry.");
        }

        if (uiFileOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Persisted Ultra Instinct forms at dragonminez/races/saiyan/forms/ultrainstinct.json.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not persist Ultra Instinct form file.");
        }

        if (uiCleanupOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Removed legacy Ultra Instinct entries from supersaiyan group.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not clean legacy Ultra Instinct entries from supersaiyan group.");
        }

        if (specialRuntimeOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Installed Beast, Orange, Black and Full Power forms into runtime form registries.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not install one or more special race forms in runtime.");
        }

        if (specialFilesOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Persisted Beast, Orange, Black and Full Power form config files.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not persist one or more special race form config files.");
        }

        if (raceCapacityOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Verified transformation level capacities for special race forms.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not ensure transformation level capacities for one or more races.");
        }
    }

    private static boolean injectUltraInstinctIntoRuntimeFormRegistry() {
        try {
            Map<String, FormConfig> raceForms = ConfigManager.getAllFormsForRace(SpecialRaceFormsDefinitions.SAIYAN_RACE);
            if (raceForms == null) {
                return false;
            }

            FormConfig ultraInstinct = raceForms.computeIfAbsent(GROUP_ULTRA_INSTINCT, key -> {
                FormConfig cfg = new FormConfig();
                cfg.setGroupName(GROUP_ULTRA_INSTINCT);
                cfg.setFormType(UltraInstinctDefinitions.FORM_TYPE);
                cfg.setForms(new LinkedHashMap<>());
                return cfg;
            });

            Map<String, FormConfig.FormData> newOrderedForms = new LinkedHashMap<>();
            upsertUltraInstinctForm(newOrderedForms, ultraInstinct, UltraInstinctDefinitions.FORM_SIGN, 1,
                    "#161922", "#D6E7FF", "#E8F9FF", "#F5FFFF",
                    3.35, 3.65, 1.35, 2.70, 1.20, 3.90, 1.35, 1.45,
                    0.0, 1.25, 1.18, 0.022, 0.015, 0.0025);
            upsertUltraInstinctForm(newOrderedForms, ultraInstinct, UltraInstinctDefinitions.FORM_MASTERED, 2,
                    "#F2F5FF", "#EEF3FF", "#F7FCFF", "#FFFFFF",
                    3.80, 4.10, 1.55, 3.05, 1.30, 4.35, 1.50, 1.58,
                    0.0, 1.35, 1.22, 0.024, 0.016, 0.0030);
            upsertUltraInstinctForm(newOrderedForms, ultraInstinct, UltraInstinctDefinitions.FORM_AUTONOMOUS, 3,
                    "#E7EEFF", "#E3ECFF", "#FCFEFF", "#FFFFFF",
                    4.20, 4.55, 1.70, 3.35, 1.38, 4.85, 1.62, 1.70,
                    0.0, 1.48, 1.27, 0.026, 0.018, 0.0034);
            upsertUltraInstinctForm(newOrderedForms, ultraInstinct, UltraInstinctDefinitions.FORM_TRUE, 4,
                    "#1C1B2B", "#D8CCFF", "#C3B0FF", "#E8DAFF",
                    4.65, 5.05, 1.90, 3.70, 1.50, 5.40, 1.80, 1.82,
                    0.0, 1.62, 1.32, 0.030, 0.020, 0.0038);

            ultraInstinct.setGroupName(GROUP_ULTRA_INSTINCT);
            ultraInstinct.setFormType(UltraInstinctDefinitions.FORM_TYPE);
            ultraInstinct.setForms(newOrderedForms);

            FormConfig legacyGroup = raceForms.get(GROUP_LEGACY_SUPER_SAIYAN);
            if (legacyGroup != null && legacyGroup.getForms() != null) {
                removeLegacyUltraInstinctEntries(legacyGroup.getForms());
            }

            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Runtime Ultra Instinct injection failed: {}", e.getMessage());
            return false;
        }
    }

    private static void upsertUltraInstinctForm(Map<String, FormConfig.FormData> target,
                                                FormConfig sourceConfig,
                                                String formName,
                                                int unlockLevel,
                                                String hairColor,
                                                String eyeColor,
                                                String auraColor,
                                                String lightningColor,
                                                double str,
                                                double skp,
                                                double stm,
                                                double def,
                                                double vit,
                                                double pwr,
                                                double ene,
                                                double speed,
                                                double energyDrain,
                                                double staminaDrain,
                                                double attackSpeed,
                                                double statMultPerMastery,
                                                double costDecreasePerMastery,
                                                double passiveMasteryGain) {
        FormConfig.FormData formData = sourceConfig.getFormByKey(formName);
        if (formData == null) {
            formData = new FormConfig.FormData();
        }

        applyUltraInstinctFormValues(
                formData,
                formName,
                unlockLevel,
                hairColor,
                eyeColor,
                auraColor,
                lightningColor,
                str,
                skp,
                stm,
                def,
                vit,
                pwr,
                ene,
                speed,
                energyDrain,
                staminaDrain,
                attackSpeed,
                statMultPerMastery,
                costDecreasePerMastery,
                passiveMasteryGain
        );

        target.put(formName, formData);
    }

    private static boolean persistUltraInstinctFormFile() {
        Path formsFile = FMLPaths.CONFIGDIR.get()
                .resolve("dragonminez")
                .resolve("races")
                .resolve(SpecialRaceFormsDefinitions.SAIYAN_RACE)
                .resolve("forms")
                .resolve(GROUP_ULTRA_INSTINCT + ".json");

        try {
            Files.createDirectories(formsFile.getParent());

            JsonObject root;
            if (Files.exists(formsFile)) {
                try (Reader reader = Files.newBufferedReader(formsFile, StandardCharsets.UTF_8)) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }
            } else {
                root = new JsonObject();
            }

            root.addProperty("groupName", GROUP_ULTRA_INSTINCT);
            root.addProperty("formType", UltraInstinctDefinitions.FORM_TYPE);

            JsonObject forms = new JsonObject();
            forms.add(UltraInstinctDefinitions.FORM_SIGN, createUltraInstinctFormJson(UltraInstinctDefinitions.FORM_SIGN, 1,
                    "#161922", "#D6E7FF", "#E8F9FF", "#F5FFFF",
                    3.35, 3.65, 1.35, 2.70, 1.20, 3.90, 1.35, 1.45,
                    0.0, 1.25, 1.18, 0.022, 0.015, 0.0025));
            forms.add(UltraInstinctDefinitions.FORM_MASTERED, createUltraInstinctFormJson(UltraInstinctDefinitions.FORM_MASTERED, 2,
                    "#F2F5FF", "#EEF3FF", "#F7FCFF", "#FFFFFF",
                    3.80, 4.10, 1.55, 3.05, 1.30, 4.35, 1.50, 1.58,
                    0.0, 1.35, 1.22, 0.024, 0.016, 0.0030));
            forms.add(UltraInstinctDefinitions.FORM_AUTONOMOUS, createUltraInstinctFormJson(UltraInstinctDefinitions.FORM_AUTONOMOUS, 3,
                    "#E7EEFF", "#E3ECFF", "#FCFEFF", "#FFFFFF",
                    4.20, 4.55, 1.70, 3.35, 1.38, 4.85, 1.62, 1.70,
                    0.0, 1.48, 1.27, 0.026, 0.018, 0.0034));
            forms.add(UltraInstinctDefinitions.FORM_TRUE, createUltraInstinctFormJson(UltraInstinctDefinitions.FORM_TRUE, 4,
                    "#1C1B2B", "#D8CCFF", "#C3B0FF", "#E8DAFF",
                    4.65, 5.05, 1.90, 3.70, 1.50, 5.40, 1.80, 1.82,
                    0.0, 1.62, 1.32, 0.030, 0.020, 0.0038));

            root.add("forms", forms);

            try (Writer writer = Files.newBufferedWriter(formsFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Failed writing Ultra Instinct form file '{}': {}", formsFile, e.getMessage());
            return false;
        }
    }

    private static JsonObject createUltraInstinctFormJson(String formName,
                                                           int unlockLevel,
                                                           String hairColor,
                                                           String eyeColor,
                                                           String auraColor,
                                                           String lightningColor,
                                                           double str,
                                                           double skp,
                                                           double stm,
                                                           double def,
                                                           double vit,
                                                           double pwr,
                                                           double ene,
                                                           double speed,
                                                           double energyDrain,
                                                           double staminaDrain,
                                                           double attackSpeed,
                                                           double statMultPerMastery,
                                                           double costDecreasePerMastery,
                                                           double passiveMasteryGain) {
        JsonObject form = new JsonObject();
        form.addProperty("name", formName);
        form.addProperty("unlockOnSkillLevel", unlockLevel);
        form.addProperty("customModel", "");
        form.addProperty("bodyColor1", "");
        form.addProperty("bodyColor2", "");
        form.addProperty("bodyColor3", "");
        form.addProperty("hairType", "base");
        form.addProperty("forcedHairCode", "");
        form.addProperty("hairColor", hairColor);
        form.addProperty("eye1Color", eyeColor);
        form.addProperty("eye2Color", eyeColor);
        form.addProperty("auraColor", auraColor);
        form.addProperty("hasLightnings", true);
        form.addProperty("lightningColor", lightningColor);
        form.add("modelScaling", GSON.toJsonTree(new float[]{0.96f, 0.96f, 0.96f}));
        form.addProperty("strMultiplier", str);
        form.addProperty("skpMultiplier", skp);
        form.addProperty("stmMultiplier", stm);
        form.addProperty("defMultiplier", def);
        form.addProperty("vitMultiplier", vit);
        form.addProperty("pwrMultiplier", pwr);
        form.addProperty("eneMultiplier", ene);
        form.addProperty("speedMultiplier", speed);
        form.addProperty("energyDrain", energyDrain);
        form.addProperty("staminaDrain", staminaDrain);
        form.addProperty("attackSpeed", attackSpeed);
        form.addProperty("maxMastery", 100.0);
        form.addProperty("masteryPerHit", 0.08);
        form.addProperty("masteryPerDamageReceived", 0.08);
        form.addProperty("statMultPerMasteryPoint", statMultPerMastery);
        form.addProperty("costDecreasePerMasteryPoint", costDecreasePerMastery);
        form.addProperty("passiveMasteryGainEveryFiveSeconds", passiveMasteryGain);
        form.addProperty("kaiokenStackable", false);
        form.addProperty("kaiokenDrainMultiplier", 3.0);
        return form;
    }

    private static boolean cleanupLegacySuperSaiyanUltraInstinct() {
        Path formsFile = FMLPaths.CONFIGDIR.get()
                .resolve("dragonminez")
                .resolve("races")
                .resolve(SpecialRaceFormsDefinitions.SAIYAN_RACE)
                .resolve("forms")
                .resolve(GROUP_LEGACY_SUPER_SAIYAN + ".json");

        if (!Files.exists(formsFile)) {
            return true;
        }

        try (Reader reader = Files.newBufferedReader(formsFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("forms") || !root.get("forms").isJsonObject()) {
                return true;
            }

            JsonObject forms = root.getAsJsonObject("forms");
            boolean removedAny = false;
            for (String formKey : UI_FORM_KEYS) {
                removedAny |= forms.remove(formKey) != null;
            }
            if (!removedAny) {
                return true;
            }

            root.add("forms", forms);
            try (Writer writer = Files.newBufferedWriter(formsFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Failed cleaning supersaiyan file '{}': {}", formsFile, e.getMessage());
            return false;
        }
    }

    private static void removeLegacyUltraInstinctEntries(Map<String, FormConfig.FormData> forms) {
        for (String formKey : UI_FORM_KEYS) {
            forms.remove(formKey);
            forms.entrySet().removeIf(entry -> formKey.equalsIgnoreCase(entry.getValue().getName()));
        }
    }

    private static boolean injectSpecialRaceFormsIntoRuntimeFormRegistry() {
        boolean beast = injectSaiyanBeastFormRuntime();
        boolean orange = injectNamekianOrangeFormRuntime();
        boolean black = injectFrostDemonBlackFormRuntime();
        boolean fullPower = injectAlienFullPowerFormRuntime();
        return beast && orange && black && fullPower;
    }

    private static boolean injectSaiyanBeastFormRuntime() {
        try {
            Map<String, FormConfig> raceForms = ConfigManager.getAllFormsForRace(SpecialRaceFormsDefinitions.SAIYAN_RACE);
            if (raceForms == null) {
                return false;
            }

            FormConfig group = raceForms.computeIfAbsent(SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN, key -> {
                FormConfig cfg = new FormConfig();
                cfg.setGroupName(SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN);
                cfg.setFormType(FORM_TYPE_SUPER);
                cfg.setForms(new LinkedHashMap<>());
                return cfg;
            });

            if (group.getForms() == null) {
                group.setForms(new LinkedHashMap<>());
            }

            FormConfig.FormData beast = group.getFormByKey(SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST);
            if (beast == null) {
                beast = new FormConfig.FormData();
            }

            applySpecialFormValues(
                    beast,
                    SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST,
                    SpecialRaceFormsDefinitions.SAIYAN_BEAST_UNLOCK_LEVEL,
                    "",
                    "",
                    "",
                    "ssj",
                    "#ECEBEA",
                    "#D01C23",
                    "#B884FF",
                    "#F7D8FF",
                    new float[]{1.02f, 1.02f, 1.02f},
                    4.60, 4.90, 1.80, 3.30, 1.35, 5.20, 1.45, 1.55,
                    0.24, 1.55, 1.22,
                    0.030, 0.018, 0.0032,
                    false, 3.0
            );

            group.getForms().put(SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST, beast);
            group.setGroupName(SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN);
            group.setFormType(FORM_TYPE_SUPER);
            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Saiyan Beast runtime injection failed: {}", e.getMessage());
            return false;
        }
    }

    private static boolean injectNamekianOrangeFormRuntime() {
        try {
            Map<String, FormConfig> raceForms = ConfigManager.getAllFormsForRace(SpecialRaceFormsDefinitions.NAMEKIAN_RACE);
            if (raceForms == null) {
                return false;
            }

            FormConfig group = raceForms.computeIfAbsent(SpecialRaceFormsDefinitions.NAMEKIAN_GROUP_SUPERFORMS, key -> {
                FormConfig cfg = new FormConfig();
                cfg.setGroupName(SpecialRaceFormsDefinitions.NAMEKIAN_GROUP_SUPERFORMS);
                cfg.setFormType(FORM_TYPE_SUPER);
                cfg.setForms(new LinkedHashMap<>());
                return cfg;
            });

            if (group.getForms() == null) {
                group.setForms(new LinkedHashMap<>());
            }

            FormConfig.FormData orange = group.getFormByKey(SpecialRaceFormsDefinitions.NAMEKIAN_FORM_ORANGE);
            if (orange == null) {
                orange = new FormConfig.FormData();
            }

            applySpecialFormValues(
                    orange,
                    SpecialRaceFormsDefinitions.NAMEKIAN_FORM_ORANGE,
                    SpecialRaceFormsDefinitions.NAMEKIAN_ORANGE_UNLOCK_LEVEL,
                    "#D9731F",
                    "#BB4A1A",
                    "#FFB764",
                    "base",
                    "#FF7A18",
                    "#FF7A18",
                    "#FF9C2E",
                    "#FFD37A",
                    new float[]{1.12f, 1.12f, 1.12f},
                    4.10, 4.00, 2.20, 3.90, 1.70, 3.80, 1.35, 1.05,
                    0.20, 1.20, 1.05,
                    0.028, 0.017, 0.0030,
                    true, 2.0
            );

            group.getForms().put(SpecialRaceFormsDefinitions.NAMEKIAN_FORM_ORANGE, orange);
            group.setGroupName(SpecialRaceFormsDefinitions.NAMEKIAN_GROUP_SUPERFORMS);
            group.setFormType(FORM_TYPE_SUPER);
            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Namekian Orange runtime injection failed: {}", e.getMessage());
            return false;
        }
    }

    private static boolean injectFrostDemonBlackFormRuntime() {
        try {
            Map<String, FormConfig> raceForms = ConfigManager.getAllFormsForRace(SpecialRaceFormsDefinitions.FROST_DEMON_RACE);
            if (raceForms == null) {
                return false;
            }

            FormConfig group = raceForms.computeIfAbsent(SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_EVOLUTION, key -> {
                FormConfig cfg = new FormConfig();
                cfg.setGroupName(SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_EVOLUTION);
                cfg.setFormType(FORM_TYPE_SUPER);
                cfg.setForms(new LinkedHashMap<>());
                return cfg;
            });

            if (group.getForms() == null) {
                group.setForms(new LinkedHashMap<>());
            }

            FormConfig.FormData black = group.getFormByKey(SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK);
            if (black == null) {
                black = new FormConfig.FormData();
            }

            applySpecialFormValues(
                    black,
                    SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK,
                    SpecialRaceFormsDefinitions.FROST_DEMON_BLACK_UNLOCK_LEVEL,
                    "#0E0E12",
                    "#4F0F26",
                    "#1A1A24",
                    "base",
                    "",
                    "#D90B0B",
                    "#7A0CFF",
                    "#FF2E9C",
                    new float[]{1.18f, 1.18f, 1.18f},
                    4.40, 4.60, 1.80, 3.00, 1.25, 5.10, 1.50, 1.35,
                    0.26, 1.40, 1.18,
                    0.028, 0.018, 0.0030,
                    true, 2.0
            );

            group.getForms().put(SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK, black);
            group.setGroupName(SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_EVOLUTION);
            group.setFormType(FORM_TYPE_SUPER);
            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Frost Demon Black runtime injection failed: {}", e.getMessage());
            return false;
        }
    }

    private static boolean injectAlienFullPowerFormRuntime() {
        try {
            Map<String, FormConfig> raceForms = ConfigManager.getAllFormsForRace(SpecialRaceFormsDefinitions.ALIEN_RACE);
            if (raceForms == null) {
                return false;
            }

            FormConfig group = raceForms.computeIfAbsent(SpecialRaceFormsDefinitions.ALIEN_GROUP_SUPERFORMS, key -> {
                FormConfig cfg = new FormConfig();
                cfg.setGroupName(SpecialRaceFormsDefinitions.ALIEN_GROUP_SUPERFORMS);
                cfg.setFormType(FORM_TYPE_SUPER);
                cfg.setForms(new LinkedHashMap<>());
                return cfg;
            });

            if (group.getForms() == null) {
                group.setForms(new LinkedHashMap<>());
            }

            FormConfig.FormData fullPower = group.getFormByKey(SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER);
            if (fullPower == null) {
                fullPower = new FormConfig.FormData();
            }

            applySpecialFormValues(
                    fullPower,
                    SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER,
                    SpecialRaceFormsDefinitions.ALIEN_FULL_POWER_UNLOCK_LEVEL,
                    "#E67E7E",
                    "#C85F5F",
                    "#A94545",
                    "base",
                    "",
                    "#A80F0F",
                    "#F11212",
                    "#FF6E3A",
                    new float[]{1.22f, 1.22f, 1.22f},
                    5.00, 5.20, 2.10, 3.80, 1.55, 5.80, 1.80, 1.45,
                    0.28, 1.55, 1.24,
                    0.030, 0.018, 0.0032,
                    true, 2.4
            );

            group.getForms().put(SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER, fullPower);
            group.setGroupName(SpecialRaceFormsDefinitions.ALIEN_GROUP_SUPERFORMS);
            group.setFormType(FORM_TYPE_SUPER);
            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Alien Full Power runtime injection failed: {}", e.getMessage());
            return false;
        }
    }

    private static void applySpecialFormValues(FormConfig.FormData formData,
                                               String formName,
                                               int unlockLevel,
                                               String bodyColor1,
                                               String bodyColor2,
                                               String bodyColor3,
                                               String hairType,
                                               String hairColor,
                                               String eyeColor,
                                               String auraColor,
                                               String lightningColor,
                                               float[] modelScaling,
                                               double str,
                                               double skp,
                                               double stm,
                                               double def,
                                               double vit,
                                               double pwr,
                                               double ene,
                                               double speed,
                                               double energyDrain,
                                               double staminaDrain,
                                               double attackSpeed,
                                               double statMultPerMastery,
                                               double costDecreasePerMastery,
                                               double passiveMasteryGain,
                                               boolean kaiokenStackable,
                                               double kaiokenDrainMultiplier) {
        formData.setName(formName);
        formData.setUnlockOnSkillLevel(unlockLevel);
        formData.setCustomModel("");
        formData.setBodyColor1(bodyColor1);
        formData.setBodyColor2(bodyColor2);
        formData.setBodyColor3(bodyColor3);
        formData.setHairType(hairType);
        formData.setForcedHairCode("");
        formData.setHairColor(hairColor);
        formData.setEye1Color(eyeColor);
        formData.setEye2Color(eyeColor);
        formData.setAuraColor(auraColor);
        formData.setHasLightnings(true);
        formData.setLightningColor(lightningColor);
        formData.setModelScaling(modelScaling);
        formData.setStrMultiplier(str);
        formData.setSkpMultiplier(skp);
        formData.setStmMultiplier(stm);
        formData.setDefMultiplier(def);
        formData.setVitMultiplier(vit);
        formData.setPwrMultiplier(pwr);
        formData.setEneMultiplier(ene);
        formData.setSpeedMultiplier(speed);
        formData.setEnergyDrain(energyDrain);
        formData.setStaminaDrain(staminaDrain);
        formData.setAttackSpeed(attackSpeed);
        formData.setMaxMastery(100.0);
        formData.setMasteryPerHit(0.08);
        formData.setMasteryPerDamageReceived(0.08);
        formData.setStatMultPerMasteryPoint(statMultPerMastery);
        formData.setCostDecreasePerMasteryPoint(costDecreasePerMastery);
        // Passive mastery is persisted in JSON to avoid runtime signature mismatches on some DMZ builds.
        formData.setKaiokenStackable(kaiokenStackable);
        formData.setKaiokenDrainMultiplier(kaiokenDrainMultiplier);
    }

    private static boolean persistSpecialRaceFormFiles() {
        boolean beast = persistFormInGroupFile(
                SpecialRaceFormsDefinitions.SAIYAN_RACE,
                SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN,
                FORM_TYPE_SUPER,
                SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST,
                createSpecialFormJson(
                        SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST,
                        SpecialRaceFormsDefinitions.SAIYAN_BEAST_UNLOCK_LEVEL,
                        "", "", "", "ssj",
                        "#ECEBEA", "#D01C23", "#B884FF", "#F7D8FF",
                        new float[]{1.02f, 1.02f, 1.02f},
                        4.60, 4.90, 1.80, 3.30, 1.35, 5.20, 1.45, 1.55,
                        0.24, 1.55, 1.22,
                        0.030, 0.018, 0.0032,
                        false, 3.0
                )
        );

        boolean orange = persistFormInGroupFile(
                SpecialRaceFormsDefinitions.NAMEKIAN_RACE,
                SpecialRaceFormsDefinitions.NAMEKIAN_GROUP_SUPERFORMS,
                FORM_TYPE_SUPER,
                SpecialRaceFormsDefinitions.NAMEKIAN_FORM_ORANGE,
                createSpecialFormJson(
                        SpecialRaceFormsDefinitions.NAMEKIAN_FORM_ORANGE,
                        SpecialRaceFormsDefinitions.NAMEKIAN_ORANGE_UNLOCK_LEVEL,
                        "#D9731F", "#BB4A1A", "#FFB764", "base",
                        "#FF7A18", "#FF7A18", "#FF9C2E", "#FFD37A",
                        new float[]{1.12f, 1.12f, 1.12f},
                        4.10, 4.00, 2.20, 3.90, 1.70, 3.80, 1.35, 1.05,
                        0.20, 1.20, 1.05,
                        0.028, 0.017, 0.0030,
                        true, 2.0
                )
        );

        boolean black = persistFormInGroupFile(
                SpecialRaceFormsDefinitions.FROST_DEMON_RACE,
                SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_EVOLUTION,
                FORM_TYPE_SUPER,
                SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK,
                createSpecialFormJson(
                        SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK,
                        SpecialRaceFormsDefinitions.FROST_DEMON_BLACK_UNLOCK_LEVEL,
                        "#0E0E12", "#4F0F26", "#1A1A24", "base",
                        "", "#D90B0B", "#7A0CFF", "#FF2E9C",
                        new float[]{1.18f, 1.18f, 1.18f},
                        4.40, 4.60, 1.80, 3.00, 1.25, 5.10, 1.50, 1.35,
                        0.26, 1.40, 1.18,
                        0.028, 0.018, 0.0030,
                        true, 2.0
                )
        );

        boolean fullPower = persistFormInGroupFile(
                SpecialRaceFormsDefinitions.ALIEN_RACE,
                SpecialRaceFormsDefinitions.ALIEN_GROUP_SUPERFORMS,
                FORM_TYPE_SUPER,
                SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER,
                createSpecialFormJson(
                        SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER,
                        SpecialRaceFormsDefinitions.ALIEN_FULL_POWER_UNLOCK_LEVEL,
                        "#E67E7E", "#C85F5F", "#A94545", "base",
                        "", "#A80F0F", "#F11212", "#FF6E3A",
                        new float[]{1.22f, 1.22f, 1.22f},
                        5.00, 5.20, 2.10, 3.80, 1.55, 5.80, 1.80, 1.45,
                        0.28, 1.55, 1.24,
                        0.030, 0.018, 0.0032,
                        true, 2.4
                )
        );

        return beast && orange && black && fullPower;
    }

    private static boolean persistFormInGroupFile(String race,
                                                  String group,
                                                  String formType,
                                                  String formKey,
                                                  JsonObject formJson) {
        Path formsFile = FMLPaths.CONFIGDIR.get()
                .resolve("dragonminez")
                .resolve("races")
                .resolve(race)
                .resolve("forms")
                .resolve(group + ".json");

        try {
            Files.createDirectories(formsFile.getParent());

            JsonObject root;
            if (Files.exists(formsFile)) {
                try (Reader reader = Files.newBufferedReader(formsFile, StandardCharsets.UTF_8)) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }
            } else {
                root = new JsonObject();
            }

            root.addProperty("groupName", group);
            root.addProperty("formType", formType);

            JsonObject forms = root.has("forms") && root.get("forms").isJsonObject()
                    ? root.getAsJsonObject("forms")
                    : new JsonObject();
            forms.add(formKey, formJson);
            root.add("forms", forms);

            try (Writer writer = Files.newBufferedWriter(formsFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            return true;
        } catch (IOException | IllegalStateException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Failed writing form '{}' to '{}': {}", formKey, formsFile, e.getMessage());
            return false;
        }
    }

    private static JsonObject createSpecialFormJson(String formName,
                                                    int unlockLevel,
                                                    String bodyColor1,
                                                    String bodyColor2,
                                                    String bodyColor3,
                                                    String hairType,
                                                    String hairColor,
                                                    String eyeColor,
                                                    String auraColor,
                                                    String lightningColor,
                                                    float[] modelScaling,
                                                    double str,
                                                    double skp,
                                                    double stm,
                                                    double def,
                                                    double vit,
                                                    double pwr,
                                                    double ene,
                                                    double speed,
                                                    double energyDrain,
                                                    double staminaDrain,
                                                    double attackSpeed,
                                                    double statMultPerMastery,
                                                    double costDecreasePerMastery,
                                                    double passiveMasteryGain,
                                                    boolean kaiokenStackable,
                                                    double kaiokenDrainMultiplier) {
        JsonObject form = new JsonObject();
        form.addProperty("name", formName);
        form.addProperty("unlockOnSkillLevel", unlockLevel);
        form.addProperty("customModel", "");
        form.addProperty("bodyColor1", bodyColor1);
        form.addProperty("bodyColor2", bodyColor2);
        form.addProperty("bodyColor3", bodyColor3);
        form.addProperty("hairType", hairType);
        form.addProperty("forcedHairCode", "");
        form.addProperty("hairColor", hairColor);
        form.addProperty("eye1Color", eyeColor);
        form.addProperty("eye2Color", eyeColor);
        form.addProperty("auraColor", auraColor);
        form.addProperty("hasLightnings", true);
        form.addProperty("lightningColor", lightningColor);
        form.add("modelScaling", GSON.toJsonTree(modelScaling));
        form.addProperty("strMultiplier", str);
        form.addProperty("skpMultiplier", skp);
        form.addProperty("stmMultiplier", stm);
        form.addProperty("defMultiplier", def);
        form.addProperty("vitMultiplier", vit);
        form.addProperty("pwrMultiplier", pwr);
        form.addProperty("eneMultiplier", ene);
        form.addProperty("speedMultiplier", speed);
        form.addProperty("energyDrain", energyDrain);
        form.addProperty("staminaDrain", staminaDrain);
        form.addProperty("attackSpeed", attackSpeed);
        form.addProperty("maxMastery", 100.0);
        form.addProperty("masteryPerHit", 0.08);
        form.addProperty("masteryPerDamageReceived", 0.08);
        form.addProperty("statMultPerMasteryPoint", statMultPerMastery);
        form.addProperty("costDecreasePerMasteryPoint", costDecreasePerMastery);
        form.addProperty("passiveMasteryGainEveryFiveSeconds", passiveMasteryGain);
        form.addProperty("kaiokenStackable", kaiokenStackable);
        form.addProperty("kaiokenDrainMultiplier", kaiokenDrainMultiplier);
        return form;
    }

    private static boolean ensureSpecialRaceProgressionCapacities() {
        boolean saiyan = ensureSuperformLevelCapacity(
                SpecialRaceFormsDefinitions.SAIYAN_RACE,
                SpecialRaceFormsDefinitions.SAIYAN_BEAST_UNLOCK_LEVEL,
                SAIYAN_SUPERFORM_DEFAULT_COSTS
        );
        boolean namekian = ensureSuperformLevelCapacity(
                SpecialRaceFormsDefinitions.NAMEKIAN_RACE,
                SpecialRaceFormsDefinitions.NAMEKIAN_ORANGE_UNLOCK_LEVEL,
                NAMEKIAN_SUPERFORM_DEFAULT_COSTS
        );
        boolean frostDemon = ensureSuperformLevelCapacity(
                SpecialRaceFormsDefinitions.FROST_DEMON_RACE,
                SpecialRaceFormsDefinitions.FROST_DEMON_BLACK_UNLOCK_LEVEL,
                FROST_DEMON_SUPERFORM_DEFAULT_COSTS
        );
        boolean alien = ensureSuperformLevelCapacity(
                SpecialRaceFormsDefinitions.ALIEN_RACE,
                SpecialRaceFormsDefinitions.ALIEN_MAX_SUPERFORM_LEVEL,
                ALIEN_SUPERFORM_DEFAULT_COSTS
        );
        return saiyan && namekian && frostDemon && alien;
    }

    private static boolean ensureSuperformLevelCapacity(String race,
                                                        int requiredLevel,
                                                        int[] defaultCosts) {
        try {
            RaceCharacterConfig raceCharacter = ConfigManager.getRaceCharacter(race);
            if (raceCharacter == null) {
                return false;
            }

            int[] costs = raceCharacter.getSuperformTpCost();
            if (costs != null && costs.length >= requiredLevel) {
                return true;
            }

            raceCharacter.setSuperformTpCost(buildUpgradedCosts(costs, requiredLevel, defaultCosts));
            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not update superform capacity for race '{}': {}", race, e.getMessage());
            return false;
        }
    }

    private static int[] buildUpgradedCosts(int[] existing,
                                            int minimumLength,
                                            int[] defaults) {
        if (existing == null || existing.length == 0) {
            return defaults.length >= minimumLength ? defaults : Arrays.copyOf(defaults, minimumLength);
        }

        if (existing.length >= minimumLength) {
            return existing;
        }

        int[] upgraded = Arrays.copyOf(existing, minimumLength);
        int step = existing.length >= 2 ? upgraded[existing.length - 1] - upgraded[existing.length - 2] : 40000;
        if (step <= 0) {
            step = 40000;
        }

        int last = upgraded[existing.length - 1];
        for (int i = existing.length; i < minimumLength; i++) {
            last += step;
            upgraded[i] = last;
        }
        return upgraded;
    }

    private static void applyUltraInstinctFormValues(FormConfig.FormData formData,
                                                     String formName,
                                                     int unlockLevel,
                                                     String hairColor,
                                                     String eyeColor,
                                                     String auraColor,
                                                     String lightningColor,
                                                     double str,
                                                     double skp,
                                                     double stm,
                                                     double def,
                                                     double vit,
                                                     double pwr,
                                                     double ene,
                                                     double speed,
                                                     double energyDrain,
                                                     double staminaDrain,
                                                     double attackSpeed,
                                                     double statMultPerMastery,
                                                     double costDecreasePerMastery,
                                                     double passiveMasteryGain) {
        formData.setName(formName);
        formData.setUnlockOnSkillLevel(unlockLevel);
        formData.setCustomModel("");
        formData.setBodyColor1("");
        formData.setBodyColor2("");
        formData.setBodyColor3("");
        formData.setHairType("base");
        formData.setForcedHairCode("");
        formData.setHairColor(hairColor);
        formData.setEye1Color(eyeColor);
        formData.setEye2Color(eyeColor);
        formData.setAuraColor(auraColor);
        formData.setHasLightnings(true);
        formData.setLightningColor(lightningColor);
        formData.setModelScaling(new float[]{0.96f, 0.96f, 0.96f});
        formData.setStrMultiplier(str);
        formData.setSkpMultiplier(skp);
        formData.setStmMultiplier(stm);
        formData.setDefMultiplier(def);
        formData.setVitMultiplier(vit);
        formData.setPwrMultiplier(pwr);
        formData.setEneMultiplier(ene);
        formData.setSpeedMultiplier(speed);
        formData.setEnergyDrain(energyDrain);
        formData.setStaminaDrain(staminaDrain);
        formData.setAttackSpeed(attackSpeed);
        formData.setMaxMastery(100.0);
        formData.setMasteryPerHit(0.08);
        formData.setMasteryPerDamageReceived(0.08);
        formData.setStatMultPerMasteryPoint(statMultPerMastery);
        formData.setCostDecreasePerMasteryPoint(costDecreasePerMastery);
        // Passive mastery is persisted in JSON to avoid hard dependency on DMZ runtime setter signatures.
        formData.setKaiokenStackable(false);
        formData.setKaiokenDrainMultiplier(3.0);
    }
}
