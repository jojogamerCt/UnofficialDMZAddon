package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.DefaultFormsFactory;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.hair.HairManager;
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enables DragonMineZ's own divine stack forms and extends Ultra Instinct with
 * addon-only tiers. DragonMineZ remains responsible for form configs, teaching,
 * skill-tree upgrades, mastery, charging, syncing, previews, and X-wheel groups.
 */
public final class DivineProgressionInstaller {

    private static final List<String> SAIYAN_ONLY = List.of(SpecialRaceFormsDefinitions.SAIYAN_RACE);
    private static final List<Integer> LEGACY_ULTRA_INSTINCT_COSTS = List.of(-1, 5_000, 260_000, 360_000);
    private static final List<Integer> LEGACY_ULTRA_EGO_COSTS = List.of(-1, 5_000);
    private static final List<Integer> ULTRA_INSTINCT_COSTS = List.of(120_000, 180_000, 360_000);
    private static final List<Integer> ULTRA_EGO_COSTS = List.of(180_000);
    private static final String ULTRA_INSTINCT_MASTER = "goku";
    private static final String ULTRA_EGO_MASTER = "vegeta";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DivineProgressionInstaller() {
    }

    public static boolean install() {
        boolean skillsOk = configureNativeSkills();
        boolean migrationOk = migrateLegacyBaseFormGroup();
        boolean runtimeOk = installRuntimeStackForms();
        boolean filesOk = persistNativeConfigFiles();

        if (skillsOk && runtimeOk && filesOk && migrationOk) {
            UnofficialDMZAddon.LOGGER.info(
                    "[Unofficial DMZ Addon] Divine progression installed from DragonMineZ defaults: "
                            + "Ultra Instinct and Ultra Ego appear only in native "
                            + "form skill trees and independent X-wheel form groups, and config overrides are preserved."
            );
            return true;
        }

        UnofficialDMZAddon.LOGGER.warn(
                "[Unofficial DMZ Addon] Divine progression was only partially installed "
                        + "(skills={}, migration={}, runtime={}, files={}).",
                skillsOk, migrationOk, runtimeOk, filesOk
        );
        return false;
    }

    private static boolean configureNativeSkills() {
        try {
            SkillsConfig skills = ConfigManager.getSkillsConfig();
            removeOffering(skills, "whis", StackForms.GROUP_ULTRAINSTINCT);
            removeOffering(skills, "beerus", StackForms.GROUP_ULTRAEGO);
            if (UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()) {
                registerIndependentFormSkill(skills, StackForms.GROUP_ULTRAINSTINCT, ULTRA_INSTINCT_COSTS, ULTRA_INSTINCT_MASTER);
            } else {
                unregisterIndependentFormSkill(skills, StackForms.GROUP_ULTRAINSTINCT, ULTRA_INSTINCT_MASTER);
            }
            if (UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()) {
                registerIndependentFormSkill(skills, StackForms.GROUP_ULTRAEGO, ULTRA_EGO_COSTS, ULTRA_EGO_MASTER);
            } else {
                unregisterIndependentFormSkill(skills, StackForms.GROUP_ULTRAEGO, ULTRA_EGO_MASTER);
            }
            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn(
                    "[Unofficial DMZ Addon] Failed configuring divine skills: {}", e.getMessage());
            return false;
        }
    }

    private static void registerIndependentFormSkill(SkillsConfig config,
                                                     String skillId,
                                                     List<Integer> costs,
                                                     String master) {
        config.getStackSkills().removeIf(skillId::equalsIgnoreCase);
        if (config.getFormSkills().stream().noneMatch(skillId::equalsIgnoreCase)) {
            config.getFormSkills().add(skillId);
        }

        SkillsConfig.SkillCosts existing = config.getSkills().get(skillId);
        if (existing == null) {
            config.getSkills().put(skillId,
                    new SkillsConfig.SkillCosts(new ArrayList<>(costs), new ArrayList<>(SAIYAN_ONLY)));
        } else {
            existing.setCosts(migrateCosts(skillId, existing.getCosts()));
            existing.setAllowedRaces(new ArrayList<>(SAIYAN_ONLY));
        }

        RaceCharacterConfig saiyan = ConfigManager.getRaceCharacter(SpecialRaceFormsDefinitions.SAIYAN_RACE);
        if (saiyan != null) {
            RaceCharacterConfig.FormSkillCost formCost = saiyan.getFormSkillsCosts().get(skillId);
            if (formCost == null) {
                saiyan.getFormSkillsCosts().put(skillId,
                        new RaceCharacterConfig.FormSkillCost(false, new ArrayList<>(costs)));
            } else {
                formCost.setPrices(migrateCosts(skillId, formCost.getPrices()));
                formCost.setBuyFromMaster(false);
            }
        }
        removeOffering(config, master, skillId);
    }

    private static void unregisterIndependentFormSkill(SkillsConfig config, String skillId, String master) {
        config.getStackSkills().removeIf(skillId::equalsIgnoreCase);
        config.getFormSkills().removeIf(skillId::equalsIgnoreCase);
        config.getSkills().keySet().removeIf(skillId::equalsIgnoreCase);
        RaceCharacterConfig saiyan = ConfigManager.getRaceCharacter(SpecialRaceFormsDefinitions.SAIYAN_RACE);
        if (saiyan != null) saiyan.getFormSkillsCosts().remove(skillId);
        List<String> offerings = config.getSkillOfferings().get(master);
        if (offerings != null) offerings.removeIf(skillId::equalsIgnoreCase);
    }
    private static void removeOffering(SkillsConfig config, String master, String skillId) {
        List<String> offerings = config.getSkillOfferings().get(master);
        if (offerings != null) {
            offerings.removeIf(skillId::equalsIgnoreCase);
        }
    }

    private static boolean installRuntimeStackForms() {
        try {
            Map<String, FormConfig> stackForms = ConfigManager.getAllStackForms();
            stackForms.remove(StackForms.GROUP_ULTRAINSTINCT);
            stackForms.remove(StackForms.GROUP_ULTRAEGO);

            Map<String, FormConfig> saiyanForms = ConfigManager.getAllFormsForRace(SpecialRaceFormsDefinitions.SAIYAN_RACE);
            if (saiyanForms == null) return false;
            if (UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()) {
                mergeConfigBackedGroup(saiyanForms, createExtendedUltraInstinctGroup());
            } else {
                saiyanForms.remove(StackForms.GROUP_ULTRAINSTINCT);
            }
            if (UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()) {
                mergeConfigBackedGroup(saiyanForms, createOriginalUltraEgoGroup());
            } else {
                saiyanForms.remove(StackForms.GROUP_ULTRAEGO);
            }
            return true;
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.warn(
                    "[Unofficial DMZ Addon] Failed installing independent divine forms at runtime: {}", e.getMessage());
            return false;
        }
    }
    private static void mergeConfigBackedGroup(Map<String, FormConfig> registry, FormConfig defaults) {
        String groupName = defaults.getGroupName().toLowerCase();
        FormConfig existing = registry.get(groupName);
        if (existing == null) {
            registry.put(groupName, defaults);
            return;
        }

        if (existing.getForms() == null) {
            existing.setForms(new LinkedHashMap<>());
        }

        migrateLegacyAddonDuplicates(existing, defaults, groupName);
        if (StackForms.GROUP_ULTRAINSTINCT.equals(groupName)) existing.getForms().remove("autonomous");
        if (StackForms.GROUP_ULTRAINSTINCT.equals(groupName)) {
            FormConfig.FormData trueUi = existing.getFormByKey(UltraInstinctDefinitions.FORM_TRUE);
            if (trueUi != null) {
                trueUi.setUnlockOnSkillLevel(3);
                trueUi.setFormRequisite(StackForms.GROUP_ULTRAINSTINCT + "." + StackForms.ULTRAINSTINCT_MASTERED);
                trueUi.setUnlockOnMastery(75.0);
            }
        }
        if (StackForms.GROUP_ULTRAEGO.equals(groupName)) existing.getForms().remove(StackForms.ULTRAEGO_SIGN);
        if (StackForms.GROUP_ULTRAEGO.equals(groupName)) {
            FormConfig.FormData ultraEgo = existing.getFormByKey(StackForms.ULTRAEGO_MASTERED);
            if (ultraEgo != null) {
                ultraEgo.setUnlockOnSkillLevel(1);
                ultraEgo.setFormRequisite("");
                ultraEgo.setUnlockOnMastery(0.0);
            }
        }
        defaults.getForms().forEach(existing.getForms()::putIfAbsent);
        copyCanonicalDivineAuras(existing, defaults);
        if (existing.getGroupName() == null || existing.getGroupName().isBlank()) {
            existing.setGroupName(groupName);
        }
        if (existing.getFormType() == null || existing.getFormType().isBlank()) {
            existing.setFormType(groupName);
        }
    }

    private static void migrateLegacyAddonDuplicates(FormConfig existing,
                                                       FormConfig originalDefaults,
                                                       String groupName) {
        for (String formKey : builtInFormKeys(groupName)) {
            FormConfig.FormData current = existing.getFormByKey(formKey);
            FormConfig.FormData original = originalDefaults.getFormByKey(formKey);
            if (current != null && original != null && matchesLegacyAddonDuplicate(groupName, formKey, current)) {
                existing.getForms().put(formKey, original);
            }
        }
    }

    private static boolean matchesLegacyAddonDuplicate(String groupName,
                                                        String formKey,
                                                        FormConfig.FormData form) {
        if (StackForms.GROUP_ULTRAINSTINCT.equals(groupName)) {
            if (StackForms.ULTRAINSTINCT_SIGN.equals(formKey)) {
                return near(form.getStrMultiplier(), 1.35) && near(form.getSkpMultiplier(), 1.45);
            }
            if (StackForms.ULTRAINSTINCT_MASTERED.equals(formKey)) {
                return near(form.getStrMultiplier(), 1.55) && near(form.getSkpMultiplier(), 1.65);
            }
        }
        if (StackForms.GROUP_ULTRAEGO.equals(groupName)) {
            if (StackForms.ULTRAEGO_SIGN.equals(formKey)) {
                return near(form.getStrMultiplier(), 1.48) && near(form.getSkpMultiplier(), 1.38);
            }
            if (StackForms.ULTRAEGO_MASTERED.equals(formKey)) {
                return near(form.getStrMultiplier(), 1.82) && near(form.getSkpMultiplier(), 1.62);
            }
        }
        return false;
    }

    private static boolean near(Double actual, double expected) {
        return actual != null && Math.abs(actual - expected) < 0.000001;
    }

    private static boolean persistNativeConfigFiles() {
        boolean skills = persistNativeSkillConfig() && persistSaiyanDivineSkillCosts();
        boolean ui = !UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()
                || persistConfigBackedGroup(createExtendedUltraInstinctGroup());
        boolean ue = !UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()
                || persistConfigBackedGroup(createOriginalUltraEgoGroup());
        boolean legacy = archiveLegacyStackFormFile(StackForms.GROUP_ULTRAINSTINCT)
                && archiveLegacyStackFormFile(StackForms.GROUP_ULTRAEGO);
        return skills && ui && ue && legacy;
    }

    private static boolean persistNativeSkillConfig() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("skills.json");
        try {
            JsonObject root;
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }
            } else {
                root = new JsonObject();
            }

            JsonArray stackSkills = objectArray(root, "stackSkills");
            JsonArray formSkills = objectArray(root, "formSkills");
            JsonObject skills = objectObject(root, "skills");
            JsonObject offerings = objectObject(root, "skillOfferings");
            removeOffering(offerings, "whis", StackForms.GROUP_ULTRAINSTINCT);
            removeOffering(offerings, ULTRA_INSTINCT_MASTER, StackForms.GROUP_ULTRAINSTINCT);
            removeOffering(offerings, "beerus", StackForms.GROUP_ULTRAEGO);
            removeOffering(offerings, ULTRA_EGO_MASTER, StackForms.GROUP_ULTRAEGO);
            removeString(stackSkills, StackForms.GROUP_ULTRAINSTINCT);
            removeString(stackSkills, StackForms.GROUP_ULTRAEGO);

            if (UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()) {
                ensureString(formSkills, StackForms.GROUP_ULTRAINSTINCT);
                ensureSkillCosts(skills, StackForms.GROUP_ULTRAINSTINCT, ULTRA_INSTINCT_COSTS);
            }
            if (UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()) {
                ensureString(formSkills, StackForms.GROUP_ULTRAEGO);
                ensureSkillCosts(skills, StackForms.GROUP_ULTRAEGO, ULTRA_EGO_COSTS);
            }

            root.add("stackSkills", stackSkills);
            root.add("formSkills", formSkills);
            root.add("skills", skills);
            root.add("skillOfferings", offerings);
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            UnofficialDMZAddon.LOGGER.warn(
                    "[Unofficial DMZ Addon] Failed persisting divine skill config '{}': {}", file, e.getMessage());
            return false;
        }
    }

    private static JsonArray objectArray(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonArray() ? root.getAsJsonArray(key) : new JsonArray();
    }

    private static JsonObject objectObject(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonObject() ? root.getAsJsonObject(key) : new JsonObject();
    }

    private static List<Integer> legacyCosts(String skillId) {
        return StackForms.GROUP_ULTRAINSTINCT.equalsIgnoreCase(skillId)
                ? LEGACY_ULTRA_INSTINCT_COSTS
                : LEGACY_ULTRA_EGO_COSTS;
    }

    private static void ensureSkillCosts(JsonObject skills, String skillId, List<Integer> costs) {
        if (!skills.has(skillId) || !skills.get(skillId).isJsonObject()) {
            skills.add(skillId, GSON.toJsonTree(new SkillsConfig.SkillCosts(
                    new ArrayList<>(costs), new ArrayList<>(SAIYAN_ONLY))));
            return;
        }

        JsonObject skill = skills.getAsJsonObject(skillId);
        List<Integer> current = new ArrayList<>();
        if (skill.has("costs") && skill.get("costs").isJsonArray()) {
            for (var value : skill.getAsJsonArray("costs")) if (value.isJsonPrimitive()) current.add(value.getAsInt());
        }
        skill.add("costs", GSON.toJsonTree(migrateCosts(skillId, current)));
    }

    private static List<Integer> migrateCosts(String skillId, List<Integer> current) {
        List<Integer> source = current == null ? List.of() : current;
        if (StackForms.GROUP_ULTRAINSTINCT.equalsIgnoreCase(skillId)) {
            if (source.size() == 3) return new ArrayList<>(source);
            if (source.size() >= 4) return new ArrayList<>(List.of(source.get(0), source.get(1), source.get(source.size() - 1)));
            return new ArrayList<>(ULTRA_INSTINCT_COSTS);
        }
        if (StackForms.GROUP_ULTRAEGO.equalsIgnoreCase(skillId)) {
            if (source.size() == 1) return new ArrayList<>(source);
            if (source.size() >= 2) return new ArrayList<>(List.of(source.get(source.size() - 1)));
            return new ArrayList<>(ULTRA_EGO_COSTS);
        }
        return new ArrayList<>(source);
    }
    private static boolean matchesCosts(JsonArray actual, List<Integer> expected) {
        if (actual.size() != expected.size()) return false;
        for (int i = 0; i < expected.size(); i++) {
            if (!actual.get(i).isJsonPrimitive() || actual.get(i).getAsInt() != expected.get(i)) return false;
        }
        return true;
    }

    private static void ensureString(JsonArray values, String value) {
        for (var element : values) {
            if (element.isJsonPrimitive() && value.equalsIgnoreCase(element.getAsString())) {
                return;
            }
        }
        values.add(value);
    }

    private static void ensureOffering(JsonObject offerings, String master, String skill) {
        JsonArray values = offerings.has(master) && offerings.get(master).isJsonArray()
                ? offerings.getAsJsonArray(master)
                : new JsonArray();
        ensureString(values, skill);
        offerings.add(master, values);
    }

    private static void removeOffering(JsonObject offerings, String master, String skill) {
        if (!offerings.has(master) || !offerings.get(master).isJsonArray()) {
            return;
        }
        JsonArray values = offerings.getAsJsonArray(master);
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i).isJsonPrimitive()
                    && skill.equalsIgnoreCase(values.get(i).getAsString())) {
                values.remove(i);
            }
        }
        if (values.isEmpty()) {
            offerings.remove(master);
        }
    }

    private static void removeString(JsonArray values, String value) {
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i).isJsonPrimitive() && value.equalsIgnoreCase(values.get(i).getAsString())) values.remove(i);
        }
    }

    private static boolean persistSaiyanDivineSkillCosts() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("races")
                .resolve(SpecialRaceFormsDefinitions.SAIYAN_RACE).resolve("character.json");
        try {
            JsonObject root;
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }
            } else {
                root = new JsonObject();
            }
            JsonObject formCosts = objectObject(root, "formSkillsCosts");
            if (UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()) {
                ensureRaceFormCost(formCosts, StackForms.GROUP_ULTRAINSTINCT, ULTRA_INSTINCT_COSTS);
            } else formCosts.remove(StackForms.GROUP_ULTRAINSTINCT);
            if (UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()) {
                ensureRaceFormCost(formCosts, StackForms.GROUP_ULTRAEGO, ULTRA_EGO_COSTS);
            } else formCosts.remove(StackForms.GROUP_ULTRAEGO);
            root.add("formSkillsCosts", formCosts);
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) { GSON.toJson(root, writer); }
            return true;
        } catch (IOException | IllegalStateException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Failed persisting Saiyan divine skill costs '{}': {}", file, e.getMessage());
            return false;
        }
    }

    private static void ensureRaceFormCost(JsonObject costs, String skillId, List<Integer> prices) {
        JsonObject entry = costs.has(skillId) && costs.get(skillId).isJsonObject()
                ? costs.getAsJsonObject(skillId) : new JsonObject();
        boolean missing = !entry.has("prices") || !entry.get("prices").isJsonArray();
        List<Integer> current = new ArrayList<>();
        if (!missing) for (var value : entry.getAsJsonArray("prices")) if (value.isJsonPrimitive()) current.add(value.getAsInt());
        entry.add("prices", GSON.toJsonTree(migrateCosts(skillId, current)));
        entry.addProperty("buyFromMaster", false);
        costs.add(skillId, entry);
    }

    private static boolean archiveLegacyStackFormFile(String groupName) {
        Path oldFile = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("forms").resolve(groupName + ".json");
        if (!Files.exists(oldFile)) return true;
        Path backup = oldFile.resolveSibling(groupName + ".independent-form.disabled");
        try {
            Files.move(oldFile, backup, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Failed archiving obsolete stack-form file '{}': {}", oldFile, e.getMessage());
            return false;
        }
    }
    private static boolean persistConfigBackedGroup(FormConfig defaults) {
        String groupName = defaults.getGroupName().toLowerCase();
        Path file = FMLPaths.CONFIGDIR.get()
                .resolve("dragonminez")
                .resolve("races")
                .resolve(SpecialRaceFormsDefinitions.SAIYAN_RACE)
                .resolve("forms")
                .resolve(groupName + ".json");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(defaults, writer);
                }
                return true;
            }

            JsonObject root;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            JsonObject forms = root.has("forms") && root.get("forms").isJsonObject()
                    ? root.getAsJsonObject("forms")
                    : new JsonObject();
            if (StackForms.GROUP_ULTRAINSTINCT.equals(groupName)) forms.remove("autonomous");
            if (StackForms.GROUP_ULTRAEGO.equals(groupName)) forms.remove(StackForms.ULTRAEGO_SIGN);

            for (Map.Entry<String, FormConfig.FormData> entry : defaults.getForms().entrySet()) {
                String formKey = entry.getKey();
                if (!forms.has(formKey)) {
                    forms.add(formKey, GSON.toJsonTree(entry.getValue()));
                } else if (isBuiltInForm(groupName, formKey)
                        && matchesLegacyAddonDuplicate(groupName, formKey, forms.getAsJsonObject(formKey))) {
                    forms.add(formKey, GSON.toJsonTree(entry.getValue()));
                }
            }

            for (Map.Entry<String, FormConfig.FormData> entry : defaults.getForms().entrySet()) {
                JsonObject form = forms.getAsJsonObject(entry.getKey());
                if (form != null) writeCanonicalAura(form, entry.getValue());
                if (form != null && StackForms.GROUP_ULTRAINSTINCT.equals(groupName)
                        && UltraInstinctDefinitions.FORM_TRUE.equalsIgnoreCase(entry.getKey())) {
                    form.addProperty("unlockOnSkillLevel", 3);
                    form.addProperty("formRequisite", StackForms.GROUP_ULTRAINSTINCT + "." + StackForms.ULTRAINSTINCT_MASTERED);
                    form.addProperty("unlockOnMastery", 75.0);
                }
                if (form != null && StackForms.GROUP_ULTRAEGO.equals(groupName)
                        && StackForms.ULTRAEGO_MASTERED.equalsIgnoreCase(entry.getKey())) {
                    form.addProperty("unlockOnSkillLevel", 1);
                    form.addProperty("formRequisite", "");
                    form.addProperty("unlockOnMastery", 0.0);
                }}

            if (!root.has("groupName")) root.addProperty("groupName", groupName);
            root.addProperty("formType", groupName);
            for (var formEntry : forms.entrySet()) {
                if (formEntry.getValue().isJsonObject()) {
                    JsonObject form = formEntry.getValue().getAsJsonObject();
                    form.addProperty("formStackable", false);
                    form.addProperty("stackDrainMultiplier", 1.0);
                    form.add("incompatibleWith", new JsonArray());
                }
            }
            root.add("forms", forms);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            UnofficialDMZAddon.LOGGER.warn(
                    "[Unofficial DMZ Addon] Failed persisting independent divine form group '{}': {}", file, e.getMessage());
            return false;
        }
    }

    private static boolean matchesLegacyAddonDuplicate(String groupName, String formKey, JsonObject form) {
        if (!form.has("strMultiplier") || !form.has("skpMultiplier")) return false;
        double str = form.get("strMultiplier").getAsDouble();
        double skp = form.get("skpMultiplier").getAsDouble();
        if (StackForms.GROUP_ULTRAINSTINCT.equals(groupName)) {
            if (StackForms.ULTRAINSTINCT_SIGN.equals(formKey)) return near(str, 1.35) && near(skp, 1.45);
            if (StackForms.ULTRAINSTINCT_MASTERED.equals(formKey)) return near(str, 1.55) && near(skp, 1.65);
        }
        if (StackForms.GROUP_ULTRAEGO.equals(groupName)) {
            if (StackForms.ULTRAEGO_SIGN.equals(formKey)) return near(str, 1.48) && near(skp, 1.38);
            if (StackForms.ULTRAEGO_MASTERED.equals(formKey)) return near(str, 1.82) && near(skp, 1.62);
        }
        return false;
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) < 0.000001;
    }

    private static List<String> builtInFormKeys(String groupName) {
        if (StackForms.GROUP_ULTRAINSTINCT.equals(groupName)) {
            return List.of(StackForms.ULTRAINSTINCT_SIGN, StackForms.ULTRAINSTINCT_MASTERED);
        }
        if (StackForms.GROUP_ULTRAEGO.equals(groupName)) {
            return List.of(StackForms.ULTRAEGO_SIGN, StackForms.ULTRAEGO_MASTERED);
        }
        return List.of();
    }

    private static boolean isBuiltInForm(String groupName, String formKey) {
        return builtInFormKeys(groupName).stream().anyMatch(formKey::equalsIgnoreCase);
    }

    private static boolean migrateLegacyBaseFormGroup() {
        return true;
    }

    private static FormConfig createExtendedUltraInstinctGroup() {
        FormConfig group = createOriginalUltraInstinctGroup();
        Map<String, FormConfig.FormData> forms = group.getForms();
        forms.putIfAbsent(UltraInstinctDefinitions.FORM_TRUE,
                addonUltraInstinctForm(UltraInstinctDefinitions.FORM_TRUE, 3,
                        "base", "#242333", "#D8CCFF", "#C3B0FF", "#E8DAFF",
                        1.88, 2.00, 1.68, 1.76, 1.52, 2.00, 1.52, 1.55,
                        0.055, 0.085, 0.07, 0.08, 1.38,
                        StackForms.GROUP_ULTRAINSTINCT + "." + StackForms.ULTRAINSTINCT_MASTERED, 75.0));
        configureUltraInstinctAuras(group);
        return group;
    }

    private static FormConfig createOriginalUltraInstinctGroup() {
        Map<String, FormConfig> groups = new LinkedHashMap<>();
        try {
            new DefaultFormsFactory().createDefaultUltraInstinctForms(stackFormsDirectory(), groups);
        } catch (IOException e) {
            throw new IllegalStateException("DragonMineZ could not create its Ultra Instinct defaults", e);
        }
        FormConfig group = makeIndependent(groups.get(StackForms.GROUP_ULTRAINSTINCT));
        configureUltraInstinctAuras(group);
        return group;
    }

    private static FormConfig createOriginalUltraEgoGroup() {
        Map<String, FormConfig> groups = new LinkedHashMap<>();
        try {
            new DefaultFormsFactory().createDefaultUltraEgoForms(stackFormsDirectory(), groups);
        } catch (IOException e) {
            throw new IllegalStateException("DragonMineZ could not create its Ultra Ego defaults", e);
        }
        FormConfig group = makeIndependent(groups.get(StackForms.GROUP_ULTRAEGO));
        if (group != null && group.getForms() != null) {
            group.getForms().remove(StackForms.ULTRAEGO_SIGN);
            FormConfig.FormData ultraEgo = group.getFormByKey(StackForms.ULTRAEGO_MASTERED);
            if (ultraEgo != null) {
                ultraEgo.setUnlockOnSkillLevel(1);
                ultraEgo.setFormRequisite("");
                ultraEgo.setUnlockOnMastery(0.0);
            }
        }
        configureUltraEgoAuras(group);
        return group;
    }

    private static void configureUltraInstinctAuras(FormConfig group) {
        if (group == null || group.getForms() == null) return;
        for (FormConfig.FormData form : group.getForms().values()) {
            String key = form.getName() == null ? "" : form.getName().toLowerCase();
            boolean mastered = key.contains("mastered");
            boolean trueUi = key.equals(UltraInstinctDefinitions.FORM_TRUE);
            form.setAuraType("god");
            form.setAuraLayer(-1);
            form.setAuraColor(trueUi ? "#D9CCFF" : mastered ? "#F8FCFF" : "#DDE8F2");
            form.setExtraAuraType("kakarot");
            form.setExtraAuraLayer(-1);
            form.setExtraAuraColor(trueUi ? "#A993FF" : mastered ? "#AEEBFF" : "#BFD8E8");
            form.setHasLightnings(false);
            form.setLightningColor(trueUi ? "#D9C7FF" : "#C7F2FF");
            if (key.equals(StackForms.ULTRAINSTINCT_MASTERED)) form.setHairColor("#BFC6D2");
        }
    }
    private static void configureUltraEgoAuras(FormConfig group) {
        if (group == null || group.getForms() == null) return;
        int tier = 0;
        for (FormConfig.FormData form : group.getForms().values()) {
            boolean mastered = form.getName() != null && form.getName().toLowerCase().contains("mastered");
            form.setAuraType("god");
            form.setAuraLayer(1);
            form.setAuraColor(mastered ? "#6F20C8" : "#51207F");
            form.setExtraAuraType("kakarot");
            form.setExtraAuraLayer(2);
            form.setExtraAuraColor(mastered ? "#F03CFF" : "#B52CE0");
            form.setHasLightnings(mastered || tier > 0);
            form.setLightningColor(mastered ? "#FF74FF" : "#C85CFF");
            tier++;
        }
    }

    private static void copyCanonicalDivineAuras(FormConfig target, FormConfig defaults) {
        for (Map.Entry<String, FormConfig.FormData> entry : defaults.getForms().entrySet()) {
            FormConfig.FormData current = target.getFormByKey(entry.getKey());
            FormConfig.FormData aura = entry.getValue();
            if (current == null || aura == null) continue;
            current.setAuraType(aura.getAuraType());
            current.setAuraLayer(aura.getAuraLayer());
            current.setAuraColor(aura.getAuraColor());
            current.setExtraAuraType(aura.getExtraAuraType());
            current.setExtraAuraLayer(aura.getExtraAuraLayer());
            current.setExtraAuraColor(aura.getExtraAuraColor());
            current.setHasLightnings(aura.getHasLightnings());
            current.setLightningColor(aura.getLightningColor());
        }
    }

    private static void writeCanonicalAura(JsonObject target, FormConfig.FormData aura) {
        target.addProperty("auraType", aura.getAuraType());
        target.addProperty("auraLayer", aura.getAuraLayer());
        target.addProperty("auraColor", aura.getAuraColor());
        target.addProperty("extraAuraType", aura.getExtraAuraType());
        target.addProperty("extraAuraLayer", aura.getExtraAuraLayer());
        target.addProperty("extraAuraColor", aura.getExtraAuraColor());
        target.addProperty("hasLightnings", aura.getHasLightnings());
        target.addProperty("lightningColor", aura.getLightningColor());
    }
    private static FormConfig makeIndependent(FormConfig group) {
        if (group == null) return null;
        group.setFormType(group.getGroupName());
        if (group.getForms() != null) {
            for (FormConfig.FormData form : group.getForms().values()) {
                form.setFormStackable(false);
                form.setStackDrainMultiplier(1.0);
                form.setIncompatibleWith(List.of());
            }
        }
        return group;
    }
    private static Path stackFormsDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("forms");
    }

    private static FormConfig.FormData addonUltraInstinctForm(String name,
                                                               int skillLevel,
                                                               String hairType,
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
                                                               double masteryPerHitDealt,
                                                               double masteryPerHitReceived,
                                                               double maxStatsMultiplier,
                                                               String requisite,
                                                               double requisiteMastery) {
        FormConfig.FormData form = new FormConfig.FormData();
        form.setName(name);
        form.setUnlockOnSkillLevel(skillLevel);
        form.setKeepBaseFormHeadBones(true);
        form.setHairType(hairType);
        form.setHairColor(hairColor);
        form.setEye1Color(eyeColor);
        form.setEye2Color(eyeColor);
        form.setAuraType("kakarot");
        form.setAuraLayer(1);
        form.setAuraColor(auraColor);
        form.setHasLightnings(true);
        form.setLightningColor(lightningColor);
        form.setStrMultiplier(str);
        form.setSkpMultiplier(skp);
        form.setStmMultiplier(stm);
        form.setDefMultiplier(def);
        form.setVitMultiplier(vit);
        form.setPwrMultiplier(pwr);
        form.setEneMultiplier(ene);
        form.setSpeedMultiplier(speed);
        form.setEnergyDrain(energyDrain);
        form.setStaminaDrain(staminaDrain);
        form.setMaxMastery(100.0);
        form.setMasteryPerHitDealt(masteryPerHitDealt);
        form.setMasteryPerHitReceived(masteryPerHitReceived);
        form.setPassiveMasteryEveryFiveSeconds(0.0025);
        form.setMaxStatsMultiplier(maxStatsMultiplier);
        form.setMaxCostMultiplier(0.72);
        form.setFormRequisite(requisite);
        form.setUnlockOnMastery(requisiteMastery);
        form.setAllowFreeTransformOnMastery(0.0);
        form.setFormStackable(false);
        form.setStackDrainMultiplier(1.0);
        form.setIncompatibleWith(List.of());
        return form;
    }
}
