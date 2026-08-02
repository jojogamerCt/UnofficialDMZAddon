package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.util.lists.StackForms;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

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

public final class TransformationInstaller {

    private static final String GROUP_LEGACY_SUPER_SAIYAN = "supersaiyan";
    private static final String FORM_TYPE_SUPER = "superforms";

    private static final int[] SAIYAN_SUPERFORM_DEFAULT_COSTS = new int[]{13000, 21000, 31000, 42000, 52000, 65000, 78000, 104000, 130000};
    private static final int[] NAMEKIAN_SUPERFORM_DEFAULT_COSTS = new int[]{20000, 80000, 120000, 160000};
    private static final int[] FROST_DEMON_SUPERFORM_DEFAULT_COSTS = new int[]{20000, 80000, 120000, 160000, 200000, 240000, 280000};
    private static final int[] ALIEN_SUPERFORM_DEFAULT_COSTS = new int[]{20000, 80000, 120000, 160000};

    private static final List<String> UI_FORM_KEYS = List.of(
            UltraInstinctDefinitions.LEGACY_FORM_OMEN,
            StackForms.ULTRAINSTINCT_SIGN,
            StackForms.ULTRAINSTINCT_MASTERED,
            UltraInstinctDefinitions.FORM_TRUE
    );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TransformationInstaller() {
    }

    private static Float[] boxScaling(float[] values) {
        Float[] boxed = new Float[values.length];
        for (int i = 0; i < values.length; i++) {
            boxed[i] = values[i];
        }
        return boxed;
    }

    private static Integer[] boxCosts(int[] values) {
        Integer[] boxed = new Integer[values.length];
        for (int i = 0; i < values.length; i++) {
            boxed[i] = values[i];
        }
        return boxed;
    }

    private static double masteryStatsMultiplier(double bonusPerPoint) {
        return 1.0 + (100.0 * bonusPerPoint);
    }

    private static double masteryCostMultiplier(double decreasePerPoint) {
        return 1.0 / (1.0 + (100.0 * decreasePerPoint));
    }

    public static void install() {
        boolean divineOk = DivineProgressionInstaller.install();
        boolean uiCleanupOk = cleanupLegacySuperSaiyanUltraInstinct();
        boolean specialRuntimeOk = injectSpecialRaceFormsIntoRuntimeFormRegistry();
        boolean specialFilesOk = persistSpecialRaceFormFiles();
        boolean raceCapacityOk = ensureSpecialRaceProgressionCapacities();

        if (!divineOk) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not fully install native divine progression.");
        }
        if (!uiCleanupOk) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not clean legacy Ultra Instinct entries from supersaiyan group.");
        }
        if (specialRuntimeOk && specialFilesOk && raceCapacityOk) {
            UnofficialDMZAddon.LOGGER.info(
                    "[Unofficial DMZ Addon] Special race forms now use native superform skill levels and mastery prerequisites."
            );
        } else {
            UnofficialDMZAddon.LOGGER.warn(
                    "[Unofficial DMZ Addon] Could not fully integrate one or more special race forms into native progression."
            );
        }
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

    private static boolean injectSpecialRaceFormsIntoRuntimeFormRegistry() {
        cleanupSaiyanBeastFromSuperSaiyanRuntime();
        boolean beast     = !UnofficialDMZConfig.SAIYAN_BEAST_FORM.get()      || injectSaiyanBeastFormRuntime();
        boolean orange    = !UnofficialDMZConfig.NAMEKIAN_ORANGE_FORM.get()    || injectNamekianOrangeFormRuntime();
        boolean golden    = !UnofficialDMZConfig.FROST_DEMON_GOLDEN_FORM.get() || injectFrostDemonGoldenFormRuntime();
        boolean black     = !UnofficialDMZConfig.FROST_DEMON_BLACK_FORM.get()  || injectFrostDemonBlackFormRuntime();
        boolean fullPower = !UnofficialDMZConfig.ALIEN_FULL_POWER_FORM.get()   || injectAlienFullPowerFormRuntime();
        cleanupFrostDemonBlackFromEvolutionFormsRuntime(); // always remove old slot
        return beast && orange && golden && black && fullPower;
    }

    private static boolean injectSaiyanBeastFormRuntime() {
        try {
            Map<String, FormConfig> raceForms = ConfigManager.getAllFormsForRace(SpecialRaceFormsDefinitions.SAIYAN_RACE);
            if (raceForms == null) {
                return false;
            }

            FormConfig group = raceForms.computeIfAbsent(SpecialRaceFormsDefinitions.SAIYAN_GROUP_BEAST, key -> {
                FormConfig cfg = new FormConfig();
                cfg.setGroupName(SpecialRaceFormsDefinitions.SAIYAN_GROUP_BEAST);
                cfg.setFormType(FORM_TYPE_SUPER);
                cfg.setForms(new LinkedHashMap<>());
                return cfg;
            });

            if (group.getForms() == null) {
                group.setForms(new LinkedHashMap<>());
            }

            FormConfig.FormData beast = group.getFormByKey(SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST);
            if (beast != null) {
                beast.setFormStackable(false);
                beast.setStackDrainMultiplier(1.0);
                return true;
            }
            beast = new FormConfig.FormData();

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
            if (orange != null) {
                orange.setFormStackable(false);
                orange.setStackDrainMultiplier(1.0);
                return true;
            }
            orange = new FormConfig.FormData();

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
                    false, 1.0
            );

            group.getForms().put(SpecialRaceFormsDefinitions.NAMEKIAN_FORM_ORANGE, orange);
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

            FormConfig group = raceForms.computeIfAbsent(SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_SUPERFORMS2, key -> {
                FormConfig cfg = new FormConfig();
                cfg.setGroupName(SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_SUPERFORMS2);
                cfg.setFormType(FORM_TYPE_SUPER);
                cfg.setForms(new LinkedHashMap<>());
                return cfg;
            });

            if (group.getForms() == null) {
                group.setForms(new LinkedHashMap<>());
            }

            FormConfig.FormData black = group.getFormByKey(SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK);
            if (black != null) {
                black.setFormStackable(false);
                black.setStackDrainMultiplier(1.0);
                return true;
            }
            black = new FormConfig.FormData();

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
                    5.80, 6.20, 2.00, 3.80, 1.45, 7.50, 1.65, 1.75,
                    0.35, 1.40, 1.35,
                    0.038, 0.025, 0.0040,
                    false, 1.0
            );

            group.getForms().put(SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK, black);
            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Frost Demon Black runtime injection failed: {}", e.getMessage());
            return false;
        }
    }

    private static boolean injectFrostDemonGoldenFormRuntime() {
        try {
            Map<String, FormConfig> raceForms = ConfigManager.getAllFormsForRace(SpecialRaceFormsDefinitions.FROST_DEMON_RACE);
            if (raceForms == null) {
                return false;
            }

            FormConfig group = raceForms.computeIfAbsent(SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_SUPERFORMS2, key -> {
                FormConfig cfg = new FormConfig();
                cfg.setGroupName(SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_SUPERFORMS2);
                cfg.setFormType(FORM_TYPE_SUPER);
                cfg.setForms(new LinkedHashMap<>());
                return cfg;
            });

            if (group.getForms() == null) {
                group.setForms(new LinkedHashMap<>());
            }

            FormConfig.FormData golden = group.getFormByKey(SpecialRaceFormsDefinitions.FROST_DEMON_FORM_GOLDEN);
            if (golden != null) {
                golden.setFormStackable(false);
                golden.setStackDrainMultiplier(1.0);
                return true;
            }
            golden = new FormConfig.FormData();

            applySpecialFormValues(
                    golden,
                    SpecialRaceFormsDefinitions.FROST_DEMON_FORM_GOLDEN,
                    SpecialRaceFormsDefinitions.FROST_DEMON_GOLDEN_UNLOCK_LEVEL,
                    "#FFD700",
                    "#E6B800",
                    "#C99A00",
                    "base",
                    "",
                    "#D90B0B",
                    "#FFD700",
                    "#FFE566",
                    new float[]{1.02f, 1.02f, 1.02f},
                    4.80, 5.00, 1.65, 2.80, 1.22, 5.90, 1.35, 1.50,
                    0.50, 1.50, 1.26,
                    0.030, 0.019, 0.0032,
                    false, 1.0
            );

            group.getForms().put(SpecialRaceFormsDefinitions.FROST_DEMON_FORM_GOLDEN, golden);
            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Frost Demon Golden runtime injection failed: {}", e.getMessage());
            return false;
        }
    }

    private static void cleanupSaiyanBeastFromSuperSaiyanRuntime() {
        try {
            Map<String, FormConfig> raceForms = ConfigManager.getAllFormsForRace(SpecialRaceFormsDefinitions.SAIYAN_RACE);
            if (raceForms == null) {
                return;
            }
            FormConfig oldGroup = raceForms.get(SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN);
            if (oldGroup != null && oldGroup.getForms() != null) {
                oldGroup.getForms().remove(SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST);
            }
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not migrate Beast out of supersaiyan runtime: {}", e.getMessage());
        }
    }
    private static void cleanupFrostDemonBlackFromEvolutionFormsRuntime() {
        try {
            Map<String, FormConfig> raceForms = ConfigManager.getAllFormsForRace(SpecialRaceFormsDefinitions.FROST_DEMON_RACE);
            if (raceForms == null) {
                return;
            }
            FormConfig evolutionGroup = raceForms.get(SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_EVOLUTION);
            if (evolutionGroup == null || evolutionGroup.getForms() == null) {
                return;
            }
            evolutionGroup.getForms().remove(SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK);
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not clean up Black Form from evolutionforms runtime: {}", e.getMessage());
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
            if (fullPower != null) {
                fullPower.setFormStackable(false);
                fullPower.setStackDrainMultiplier(1.0);
                fullPower.setCustomModel("");
                fullPower.setModelScaling(new Float[]{1.4f, 1.3f, 1.4f});
                return true;
            }
            fullPower = new FormConfig.FormData();

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
                    new float[]{1.4f, 1.3f, 1.4f},
                    5.00, 5.20, 2.10, 3.80, 1.55, 5.80, 1.80, 1.45,
                    0.28, 1.55, 1.24,
                    0.030, 0.018, 0.0032,
                    false, 1.0
            );

            group.getForms().put(SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER, fullPower);
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
        formData.setCustomModel(usesFrostDemonFinalModel(formName) ? "frostdemon_final" : "");
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
        formData.setModelScaling(boxScaling(modelScaling));
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
        formData.setMasteryPerHitDealt(0.08);
        formData.setMasteryPerHitReceived(0.08);
        formData.setMaxStatsMultiplier(masteryStatsMultiplier(statMultPerMastery));
        formData.setMaxCostMultiplier(masteryCostMultiplier(costDecreasePerMastery));
        formData.setPassiveMasteryEveryFiveSeconds(passiveMasteryGain);
        formData.setFormStackable(kaiokenStackable);
        formData.setStackDrainMultiplier(kaiokenDrainMultiplier);
        formData.setFormRequisite(nativeRequisiteForSpecialForm(formName));
        formData.setFormRequisiteType("all");
        formData.setUnlockOnMastery(nativeRequisiteMasteryForSpecialForm(formName));
    }

    private static String nativeRequisiteForSpecialForm(String formName) {
        return switch (formName) {
            case SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST -> "supersaiyan.supersaiyan4";
            case SpecialRaceFormsDefinitions.NAMEKIAN_FORM_ORANGE -> "superforms.supernamekian";
            case SpecialRaceFormsDefinitions.FROST_DEMON_FORM_GOLDEN -> "evolutionforms.fifth";
            case SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK -> "superforms2.golden";
            default -> "";
        };
    }

    private static double nativeRequisiteMasteryForSpecialForm(String formName) {
        return nativeRequisiteForSpecialForm(formName).isEmpty() ? 0.0 : 50.0;
    }

    private static boolean persistSpecialRaceFormFiles() {
        cleanupSaiyanBeastFromSuperSaiyanFile();
        boolean beast = !UnofficialDMZConfig.SAIYAN_BEAST_FORM.get() || persistFormInGroupFile(
                SpecialRaceFormsDefinitions.SAIYAN_RACE,
                SpecialRaceFormsDefinitions.SAIYAN_GROUP_BEAST,
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

        boolean orange = !UnofficialDMZConfig.NAMEKIAN_ORANGE_FORM.get() || persistFormInGroupFile(
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
                        false, 1.0
                )
        );

        boolean golden = !UnofficialDMZConfig.FROST_DEMON_GOLDEN_FORM.get() || persistFormInGroupFile(
                SpecialRaceFormsDefinitions.FROST_DEMON_RACE,
                SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_SUPERFORMS2,
                FORM_TYPE_SUPER,
                SpecialRaceFormsDefinitions.FROST_DEMON_FORM_GOLDEN,
                createSpecialFormJson(
                        SpecialRaceFormsDefinitions.FROST_DEMON_FORM_GOLDEN,
                        SpecialRaceFormsDefinitions.FROST_DEMON_GOLDEN_UNLOCK_LEVEL,
                        "#FFD700", "#E6B800", "#C99A00", "base",
                        "", "#D90B0B", "#FFD700", "#FFE566",
                        new float[]{1.02f, 1.02f, 1.02f},
                        4.80, 5.00, 1.65, 2.80, 1.22, 5.90, 1.35, 1.50,
                        0.50, 1.50, 1.26,
                        0.030, 0.019, 0.0032,
                        false, 1.0
                )
        );

        boolean black = !UnofficialDMZConfig.FROST_DEMON_BLACK_FORM.get() || persistFormInGroupFile(
                SpecialRaceFormsDefinitions.FROST_DEMON_RACE,
                SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_SUPERFORMS2,
                FORM_TYPE_SUPER,
                SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK,
                createSpecialFormJson(
                        SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK,
                        SpecialRaceFormsDefinitions.FROST_DEMON_BLACK_UNLOCK_LEVEL,
                        "#0E0E12", "#4F0F26", "#1A1A24", "base",
                        "", "#D90B0B", "#7A0CFF", "#FF2E9C",
                        new float[]{1.18f, 1.18f, 1.18f},
                        5.80, 6.20, 2.00, 3.80, 1.45, 7.50, 1.65, 1.75,
                        0.35, 1.40, 1.35,
                        0.038, 0.025, 0.0040,
                        false, 1.0
                )
        );

        cleanupFrostDemonBlackFromEvolutionFormsFile();

        boolean fullPower = !UnofficialDMZConfig.ALIEN_FULL_POWER_FORM.get() || persistFormInGroupFile(
                SpecialRaceFormsDefinitions.ALIEN_RACE,
                SpecialRaceFormsDefinitions.ALIEN_GROUP_SUPERFORMS,
                FORM_TYPE_SUPER,
                SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER,
                createSpecialFormJson(
                        SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER,
                        SpecialRaceFormsDefinitions.ALIEN_FULL_POWER_UNLOCK_LEVEL,
                        "#E67E7E", "#C85F5F", "#A94545", "base",
                        "", "#A80F0F", "#F11212", "#FF6E3A",
                        new float[]{1.4f, 1.3f, 1.4f},
                        5.00, 5.20, 2.10, 3.80, 1.55, 5.80, 1.80, 1.45,
                        0.28, 1.55, 1.24,
                        0.030, 0.018, 0.0032,
                        false, 1.0
                )
        );

        return beast && orange && black && golden && fullPower;
    }

    private static void cleanupSaiyanBeastFromSuperSaiyanFile() {
        Path formsFile = FMLPaths.CONFIGDIR.get()
                .resolve("dragonminez")
                .resolve("races")
                .resolve(SpecialRaceFormsDefinitions.SAIYAN_RACE)
                .resolve("forms")
                .resolve(SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN + ".json");
        removeFormFromFile(formsFile, SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST,
                "Could not migrate Beast out of");
    }
    private static void removeFormFromFile(Path formsFile, String formKey, String warningPrefix) {
        if (!Files.exists(formsFile)) {
            return;
        }

        try {
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(formsFile, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            if (!root.has("forms") || !root.get("forms").isJsonObject()) {
                return;
            }

            JsonObject forms = root.getAsJsonObject("forms");
            if (forms.remove(formKey) == null) {
                return;
            }
            root.add("forms", forms);
            try (Writer writer = Files.newBufferedWriter(formsFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException | IllegalStateException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] {} '{}': {}", warningPrefix, formsFile, e.getMessage());
        }
    }
    private static void cleanupFrostDemonBlackFromEvolutionFormsFile() {
        Path formsFile = FMLPaths.CONFIGDIR.get()
                .resolve("dragonminez")
                .resolve("races")
                .resolve(SpecialRaceFormsDefinitions.FROST_DEMON_RACE)
                .resolve("forms")
                .resolve(SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_EVOLUTION + ".json");

        if (!Files.exists(formsFile)) {
            return;
        }

        try {
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(formsFile, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }

            if (!root.has("forms") || !root.get("forms").isJsonObject()) {
                return;
            }

            JsonObject forms = root.getAsJsonObject("forms");
            if (forms.remove(SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK) == null) {
                return;
            }

            root.add("forms", forms);
            try (Writer writer = Files.newBufferedWriter(formsFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException | IllegalStateException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not clean up Black Form from '{}': {}", formsFile, e.getMessage());
        }
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

            JsonObject forms = root.has("forms") && root.get("forms").isJsonObject()
                    ? root.getAsJsonObject("forms")
                    : new JsonObject();
            if (forms.has(formKey) && forms.get(formKey).isJsonObject()) {
                JsonObject existing = forms.getAsJsonObject(formKey);
                existing.addProperty("formStackable", false);
                existing.addProperty("stackDrainMultiplier", 1.0);
                if (SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER.equals(formKey)) {
                    existing.addProperty("customModel", "");
                    existing.add("modelScaling", GSON.toJsonTree(new float[]{1.4f, 1.3f, 1.4f}));
                }
            } else {
                forms.add(formKey, formJson);
            }

            if (!root.has("groupName")) root.addProperty("groupName", group);
            if (!root.has("formType")) root.addProperty("formType", formType);
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

    private static boolean usesFrostDemonFinalModel(String formName) {
        return SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK.equals(formName)
                || SpecialRaceFormsDefinitions.FROST_DEMON_FORM_GOLDEN.equals(formName);
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
        form.addProperty("customModel", usesFrostDemonFinalModel(formName) ? "frostdemon_final" : "");
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
        form.addProperty("masteryPerHitDealt", 0.08);
        form.addProperty("masteryPerHitReceived", 0.08);
        form.addProperty("maxStatsMultiplier", masteryStatsMultiplier(statMultPerMastery));
        form.addProperty("maxCostMultiplier", masteryCostMultiplier(costDecreasePerMastery));
        form.addProperty("passiveMasteryEveryFiveSeconds", passiveMasteryGain);
        form.addProperty("formStackable", kaiokenStackable);
        form.addProperty("stackDrainMultiplier", kaiokenDrainMultiplier);
        form.addProperty("formRequisite", nativeRequisiteForSpecialForm(formName));
        form.addProperty("formRequisiteType", "all");
        form.addProperty("unlockOnMastery", nativeRequisiteMasteryForSpecialForm(formName));
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

            Integer[] costs = raceCharacter.getFormSkillTpCosts("superforms");
            if (costs != null && costs.length >= requiredLevel) {
                return true;
            }

            Integer[] upgraded = buildUpgradedCosts(costs, requiredLevel, defaultCosts);
            raceCharacter.setFormSkillTpCosts("superforms", upgraded);
            return persistSuperformLevelCapacity(race, upgraded);
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not update superform capacity for race '{}': {}", race, e.getMessage());
            return false;
        }
    }

    private static boolean persistSuperformLevelCapacity(String race, Integer[] costs) {
        Path file = FMLPaths.CONFIGDIR.get()
                .resolve("dragonminez")
                .resolve("races")
                .resolve(race)
                .resolve("character.json");
        try {
            if (!Files.exists(file)) {
                return false;
            }
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            JsonObject formSkills = root.has("formSkillsCosts") && root.get("formSkillsCosts").isJsonObject()
                    ? root.getAsJsonObject("formSkillsCosts")
                    : new JsonObject();
            JsonArray prices = GSON.toJsonTree(costs).getAsJsonArray();

            if (formSkills.has("superforms") && formSkills.get("superforms").isJsonObject()) {
                formSkills.getAsJsonObject("superforms").add("prices", prices);
            } else if (formSkills.has("superforms") && formSkills.get("superforms").isJsonArray()) {
                formSkills.add("superforms", prices);
            } else {
                JsonObject skill = new JsonObject();
                skill.addProperty("buyFromMaster", false);
                skill.add("prices", prices);
                formSkills.add("superforms", skill);
            }

            root.add("formSkillsCosts", formSkills);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            UnofficialDMZAddon.LOGGER.warn(
                    "[Unofficial DMZ Addon] Could not persist superform capacity for race '{}' in '{}': {}",
                    race, file, e.getMessage());
            return false;
        }
    }
    private static Integer[] buildUpgradedCosts(Integer[] existing,
                                            int minimumLength,
                                            int[] defaults) {
        if (existing == null || existing.length == 0) {
            return boxCosts(defaults.length >= minimumLength ? defaults : Arrays.copyOf(defaults, minimumLength));
        }

        if (existing.length >= minimumLength) {
            return existing;
        }

        Integer[] upgraded = Arrays.copyOf(existing, minimumLength);
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

}
