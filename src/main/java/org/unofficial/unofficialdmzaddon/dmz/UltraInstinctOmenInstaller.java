package org.unofficial.unofficialdmzaddon.dmz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

public final class UltraInstinctOmenInstaller {

    private static final String RACE_SAIYAN = "saiyan";
    private static final String GROUP_SUPER_SAIYAN = "supersaiyan";
    private static final String FORM_OMEN = "ultrainstinctomen";
    private static final int REQUIRED_SUPERFORM_LEVEL = 8;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private UltraInstinctOmenInstaller() {
    }

    public static void install() {
        boolean runtimeOk = injectIntoRuntimeFormRegistry();
        boolean fileOk = persistInSaiyanFormFile();
        boolean raceOk = ensureSaiyanSuperformLevelCapacity();

        if (runtimeOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Installed Ultra Instinct Omen form into DragonMineZ runtime registry.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not inject Ultra Instinct Omen into DragonMineZ runtime registry.");
        }

        if (fileOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Persisted Ultra Instinct Omen config at dragonminez/races/saiyan/forms/supersaiyan.json.");
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not persist Ultra Instinct Omen config file.");
        }

        if (raceOk) {
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Saiyan super form progression supports level {}.", REQUIRED_SUPERFORM_LEVEL);
        } else {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not ensure Saiyan super form progression capacity.");
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean injectIntoRuntimeFormRegistry() {
        try {
            Class<?> configManagerClass = Class.forName("com.dragonminez.common.config.ConfigManager");
            Method getAllFormsForRace = configManagerClass.getMethod("getAllFormsForRace", String.class);
            Object raceFormsObj = getAllFormsForRace.invoke(null, RACE_SAIYAN);
            if (!(raceFormsObj instanceof Map<?, ?> raceFormsRaw)) {
                return false;
            }

            Object formGroup = getIgnoreCase((Map<String, Object>) raceFormsRaw, GROUP_SUPER_SAIYAN);
            if (formGroup == null) {
                return false;
            }

            Method getForms = formGroup.getClass().getMethod("getForms");
            Object formsObj = getForms.invoke(formGroup);
            if (!(formsObj instanceof Map<?, ?> formsRaw)) {
                return false;
            }

            Map<String, Object> forms = (Map<String, Object>) formsRaw;
            Object omen = getIgnoreCase(forms, FORM_OMEN);
            if (omen == null) {
                Class<?> formDataClass = Class.forName("com.dragonminez.common.config.FormConfig$FormData");
                Constructor<?> ctor = formDataClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                omen = ctor.newInstance();
            }

            applyFormValues(omen);
            forms.put(FORM_OMEN, omen);
            return true;
        } catch (ReflectiveOperationException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Runtime form injection failed: {}", e.getMessage());
            return false;
        }
    }

    private static boolean ensureSaiyanSuperformLevelCapacity() {
        try {
            Class<?> configManagerClass = Class.forName("com.dragonminez.common.config.ConfigManager");
            Method getRaceCharacter = configManagerClass.getMethod("getRaceCharacter", String.class);
            Object raceCharacter = getRaceCharacter.invoke(null, RACE_SAIYAN);
            if (raceCharacter == null) {
                return false;
            }

            Method getSuperformTpCost = raceCharacter.getClass().getMethod("getSuperformTpCost");
            int[] costs = (int[]) getSuperformTpCost.invoke(raceCharacter);
            if (costs != null && costs.length >= REQUIRED_SUPERFORM_LEVEL) {
                return true;
            }

            int[] upgraded = buildUpgradedCosts(costs, REQUIRED_SUPERFORM_LEVEL);
            Method setSuperformTpCost = raceCharacter.getClass().getMethod("setSuperformTpCost", int[].class);
            setSuperformTpCost.invoke(raceCharacter, (Object) upgraded);
            return true;
        } catch (ReflectiveOperationException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not update Saiyan superform level capacity: {}", e.getMessage());
            return false;
        }
    }

    private static int[] buildUpgradedCosts(int[] existing, int minimumLength) {
        int[] defaults = new int[]{20000, 40000, 60000, 80000, 100000, 120000, 140000, 160000};
        if (existing == null || existing.length == 0) {
            return defaults.length >= minimumLength ? defaults : Arrays.copyOf(defaults, minimumLength);
        }

        if (existing.length >= minimumLength) {
            return existing;
        }

        int[] upgraded = Arrays.copyOf(existing, minimumLength);
        int last = upgraded[existing.length - 1];
        for (int i = existing.length; i < minimumLength; i++) {
            last += 20000;
            upgraded[i] = last;
        }
        return upgraded;
    }

    private static boolean persistInSaiyanFormFile() {
        Path formsFile = FMLPaths.CONFIGDIR.get()
                .resolve("dragonminez")
                .resolve("races")
                .resolve(RACE_SAIYAN)
                .resolve("forms")
                .resolve(GROUP_SUPER_SAIYAN + ".json");

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

            if (!root.has("groupName")) {
                root.addProperty("groupName", GROUP_SUPER_SAIYAN);
            }
            if (!root.has("formType")) {
                root.addProperty("formType", "super");
            }

            JsonObject forms = root.has("forms") && root.get("forms").isJsonObject()
                    ? root.getAsJsonObject("forms")
                    : new JsonObject();

            forms.add(FORM_OMEN, createOmenJson());
            root.add("forms", forms);

            try (Writer writer = Files.newBufferedWriter(formsFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Failed writing Omen form file '{}': {}", formsFile, e.getMessage());
            return false;
        }
    }

    private static JsonObject createOmenJson() {
        JsonObject omen = new JsonObject();
        omen.addProperty("name", FORM_OMEN);
        omen.addProperty("unlockOnSkillLevel", REQUIRED_SUPERFORM_LEVEL);
        omen.addProperty("customModel", "");
        omen.addProperty("bodyColor1", "");
        omen.addProperty("bodyColor2", "");
        omen.addProperty("bodyColor3", "");
        omen.addProperty("hairType", "base");
        omen.addProperty("forcedHairCode", "");
        omen.addProperty("hairColor", "");
        omen.addProperty("eye1Color", "#D6E7FF");
        omen.addProperty("eye2Color", "#D6E7FF");
        omen.addProperty("auraColor", "#E8F9FF");
        omen.addProperty("hasLightnings", true);
        omen.addProperty("lightningColor", "#F5FFFF");
        omen.add("modelScaling", GSON.toJsonTree(new float[]{0.96f, 0.96f, 0.96f}));
        omen.addProperty("strMultiplier", 3.35);
        omen.addProperty("skpMultiplier", 3.65);
        omen.addProperty("stmMultiplier", 1.35);
        omen.addProperty("defMultiplier", 2.70);
        omen.addProperty("vitMultiplier", 1.20);
        omen.addProperty("pwrMultiplier", 3.90);
        omen.addProperty("eneMultiplier", 1.35);
        omen.addProperty("speedMultiplier", 1.45);
        omen.addProperty("energyDrain", 0.29);
        omen.addProperty("staminaDrain", 1.25);
        omen.addProperty("attackSpeed", 1.18);
        omen.addProperty("maxMastery", 100.0);
        omen.addProperty("masteryPerHit", 0.08);
        omen.addProperty("masteryPerDamageReceived", 0.08);
        omen.addProperty("statMultPerMasteryPoint", 0.022);
        omen.addProperty("costDecreasePerMasteryPoint", 0.015);
        omen.addProperty("passiveMasteryGainEveryFiveSeconds", 0.0025);
        omen.addProperty("kaiokenStackable", false);
        omen.addProperty("kaiokenDrainMultiplier", 3.0);
        return omen;
    }

    private static void applyFormValues(Object formData) throws ReflectiveOperationException {
        set(formData, "setName", String.class, FORM_OMEN);
        set(formData, "setUnlockOnSkillLevel", int.class, REQUIRED_SUPERFORM_LEVEL);
        set(formData, "setCustomModel", String.class, "");
        set(formData, "setBodyColor1", String.class, "");
        set(formData, "setBodyColor2", String.class, "");
        set(formData, "setBodyColor3", String.class, "");
        set(formData, "setHairType", String.class, "base");
        set(formData, "setForcedHairCode", String.class, "");
        set(formData, "setHairColor", String.class, "");
        set(formData, "setEye1Color", String.class, "#D6E7FF");
        set(formData, "setEye2Color", String.class, "#D6E7FF");
        set(formData, "setAuraColor", String.class, "#E8F9FF");
        set(formData, "setHasLightnings", boolean.class, true);
        set(formData, "setLightningColor", String.class, "#F5FFFF");
        set(formData, "setModelScaling", float[].class, new float[]{0.96f, 0.96f, 0.96f});
        set(formData, "setStrMultiplier", double.class, 3.35);
        set(formData, "setSkpMultiplier", double.class, 3.65);
        set(formData, "setStmMultiplier", double.class, 1.35);
        set(formData, "setDefMultiplier", double.class, 2.70);
        set(formData, "setVitMultiplier", double.class, 1.20);
        set(formData, "setPwrMultiplier", double.class, 3.90);
        set(formData, "setEneMultiplier", double.class, 1.35);
        set(formData, "setSpeedMultiplier", double.class, 1.45);
        set(formData, "setEnergyDrain", double.class, 0.29);
        set(formData, "setStaminaDrain", double.class, 1.25);
        set(formData, "setAttackSpeed", double.class, 1.18);
        set(formData, "setMaxMastery", double.class, 100.0);
        set(formData, "setMasteryPerHit", double.class, 0.08);
        set(formData, "setMasteryPerDamageReceived", double.class, 0.08);
        set(formData, "setStatMultPerMasteryPoint", double.class, 0.022);
        set(formData, "setCostDecreasePerMasteryPoint", double.class, 0.015);
        set(formData, "setPassiveMastery", double.class, 0.0025);
        set(formData, "setKaiokenStackable", boolean.class, false);
        set(formData, "setKaiokenDrainMultiplier", double.class, 3.0);
    }

    private static void set(Object target, String methodName, Class<?> type, Object value) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName, type);
        method.invoke(target, value);
    }

    private static Object getIgnoreCase(Map<String, Object> map, String key) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
