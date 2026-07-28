package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.util.lists.StackForms;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.io.Writer;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Makes divine forms universal and installs the missing Saiyan divine branches. */
public final class UniversalDivineAndSaiyanInstaller {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Integer> UI_COSTS = List.of(120000, 180000, 260000, 360000);
    private static final List<Integer> UE_COSTS = List.of(120000, 180000);
    private UniversalDivineAndSaiyanInstaller() {}

    public static void install() {
        try {
            Map<String, Map<String, FormConfig>> all = ConfigManager.getAllForms();
            Map<String, FormConfig> saiyan = all.get("saiyan");
            if (saiyan == null) return;
            FormConfig uiTemplate = saiyan.get(StackForms.GROUP_ULTRAINSTINCT);
            FormConfig ueTemplate = saiyan.get(StackForms.GROUP_ULTRAEGO);
            if (uiTemplate == null || ueTemplate == null) return;
            correctDivineColors(uiTemplate, true, false);
            correctDivineColors(ueTemplate, false, false);

            List<String> races = new ArrayList<>(ConfigManager.getLoadedRaces());
            SkillsConfig skills = ConfigManager.getSkillsConfig();
            allowAllRaces(skills, StackForms.GROUP_ULTRAINSTINCT, races);
            allowAllRaces(skills, StackForms.GROUP_ULTRAEGO, races);

            for (String race : races) {
                Map<String, FormConfig> registry = all.computeIfAbsent(race.toLowerCase(), ignored -> new LinkedHashMap<>());
                RaceCharacterConfig character = ConfigManager.getRaceCharacter(race);
                boolean hairless = character == null || character.getHeadBones() == null || character.getHeadBones().length == 0;
                FormConfig ui = copy(uiTemplate); FormConfig ue = copy(ueTemplate);
                correctDivineColors(ui, true, hairless); correctDivineColors(ue, false, hairless);
                registry.put(StackForms.GROUP_ULTRAINSTINCT, ui);
                registry.put(StackForms.GROUP_ULTRAEGO, ue);
                if (character != null) {
                    character.getFormSkillsCosts().put(StackForms.GROUP_ULTRAINSTINCT,
                            new RaceCharacterConfig.FormSkillCost(false, new ArrayList<>(UI_COSTS)));
                    character.getFormSkillsCosts().put(StackForms.GROUP_ULTRAEGO,
                            new RaceCharacterConfig.FormSkillCost(false, new ArrayList<>(UE_COSTS)));
                }
                persist(race, ui); persist(race, ue);
                persistRaceSkillCosts(race);
            }
            persistUniversalSkillAccess(races);
            installSaiyanForms(saiyan);
            persist("saiyan", saiyan.get(SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN));
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] Universal UI/UE access and Saiyan God branches installed for {} races.", races.size());
        } catch (Exception | LinkageError e) {
            UnofficialDMZAddon.LOGGER.error("[Unofficial DMZ Addon] Universal form installation failed", e);
        }
    }

    private static void allowAllRaces(SkillsConfig skills, String id, List<String> races) {
        SkillsConfig.SkillCosts cost = skills.getSkills().get(id);
        if (cost != null) cost.setAllowedRaces(new ArrayList<>(races));
    }

    private static void correctDivineColors(FormConfig group, boolean ui, boolean hairless) {
        if (group == null || group.getForms() == null) return;
        for (Map.Entry<String, FormConfig.FormData> entry : group.getForms().entrySet()) {
            String key = entry.getKey().toLowerCase(); FormConfig.FormData form = entry.getValue();
            if (ui) {
                form.setEye1Color("#DDF7FF"); form.setEye2Color("#AEEBFF");
                if (key.contains("mastered")) form.setHairColor("#E8F2FF");
                if (hairless) { form.setHairType(""); form.setBodyColor1(key.contains("mastered") ? "#E8F2FF" : "#B8CAD8"); form.setBodyColor2("#DDF7FF"); }
            } else {
                form.setEye1Color("#D889FF"); form.setEye2Color("#8C24B8");
                form.setHairColor("#8E2AAA");
                if (key.contains("sign") && !hairless) form.setHairType("ssj2");
                if (hairless) { form.setHairType(""); form.setBodyColor1(key.contains("mastered") ? "#76208F" : "#9C42B5"); form.setBodyColor2("#D889FF"); }
            }
            form.setFormStackable(false); form.setIncompatibleWith(List.of());
        }
    }

    private static void installSaiyanForms(Map<String, FormConfig> saiyan) {
        FormConfig group = saiyan.get(SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN);
        if (group == null) return;
        if (group.getForms() == null) group.setForms(new LinkedHashMap<>());
        group.getForms().putIfAbsent("super_saiyan_god", saiyanForm("super_saiyan_god", 5, "base", "#D92F3D", "#E84C4C", "#FF665E", 2.35, 0.08));
        group.getForms().putIfAbsent("super_saiyan_blue", saiyanForm("super_saiyan_blue", 6, "ssj", "#24CBE8", "#83F4FF", "#35DFFF", 2.75, 0.13));
        group.getForms().putIfAbsent("super_saiyan_rose", saiyanForm("super_saiyan_rose", 7, "ssj", "#E146A8", "#FF9AD6", "#EE4DB2", 3.05, 0.15));
        group.getForms().putIfAbsent("super_saiyan_rage", saiyanForm("super_saiyan_rage", 8, "ssj2", "#F4D34A", "#63DFFF", "#E8DB55", 3.25, 0.18));
    }

    private static FormConfig.FormData saiyanForm(String name, int level, String hairType, String hair, String eye, String aura, double mult, double drain) {
        FormConfig.FormData f = new FormConfig.FormData(); f.setName(name); f.setUnlockOnSkillLevel(level);
        f.setHairType(hairType); f.setHairColor(hair); f.setEye1Color(eye); f.setEye2Color(eye);
        f.setAuraType("kakarot"); f.setAuraColor(aura); f.setAuraLayer(1); f.setHasLightnings(level >= 7);
        f.setLightningColor(eye); f.setStrMultiplier(mult); f.setSkpMultiplier(mult); f.setDefMultiplier(mult * 0.92);
        f.setPwrMultiplier(mult * 1.05); f.setStmMultiplier(1.25); f.setVitMultiplier(1.20); f.setEneMultiplier(1.30); f.setSpeedMultiplier(1.35);
        f.setEnergyDrain(drain); f.setStaminaDrain(drain * 0.35); f.setMaxMastery(100.0); f.setMasteryPerHitDealt(0.05);
        f.setMasteryPerHitReceived(0.04); f.setPassiveMasteryEveryFiveSeconds(0.003); f.setMaxStatsMultiplier(1.35); f.setMaxCostMultiplier(0.70);
        f.setFormStackable(false); f.setIncompatibleWith(List.of()); return f;
    }

    private static FormConfig copy(FormConfig source) { return GSON.fromJson(GSON.toJson(source), FormConfig.class); }

    private static void persistUniversalSkillAccess(List<String> races) throws Exception {
        Path file = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("skills.json");
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonObject skills = root.getAsJsonObject("skills");
        for (String id : List.of(StackForms.GROUP_ULTRAINSTINCT, StackForms.GROUP_ULTRAEGO)) {
            JsonObject skill = skills.getAsJsonObject(id);
            if (skill == null) continue;
            JsonArray allowed = new JsonArray();
            races.forEach(allowed::add);
            skill.add("allowedRaces", allowed);
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private static void persistRaceSkillCosts(String race) throws Exception {
        Path file = FMLPaths.CONFIGDIR.get().resolve("dragonminez/races").resolve(race).resolve("character.json");
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonObject costs = root.has("formSkillsCosts") && root.get("formSkillsCosts").isJsonObject()
                ? root.getAsJsonObject("formSkillsCosts") : new JsonObject();
        costs.add(StackForms.GROUP_ULTRAINSTINCT, formSkillCost(UI_COSTS));
        costs.add(StackForms.GROUP_ULTRAEGO, formSkillCost(UE_COSTS));
        root.add("formSkillsCosts", costs);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private static JsonObject formSkillCost(List<Integer> prices) {
        JsonObject entry = new JsonObject();
        entry.addProperty("buyFromMaster", false);
        JsonArray values = new JsonArray();
        prices.forEach(values::add);
        entry.add("prices", values);
        return entry;
    }

    private static void persist(String race, FormConfig group) throws Exception {
        if (group == null) return; Path file = FMLPaths.CONFIGDIR.get().resolve("dragonminez/races").resolve(race).resolve("forms").resolve(group.getGroupName().toLowerCase()+".json");
        Files.createDirectories(file.getParent()); try (Writer w=Files.newBufferedWriter(file, StandardCharsets.UTF_8)) { GSON.toJson(group,w); }
    }
}