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
    private static final List<Integer> UI_COSTS = List.of(120000, 180000, 360000);
    private static final List<Integer> UE_COSTS = List.of(180000);
    private static final List<Integer> GOD_COSTS = List.of(150000, 250000, 350000);
    public static final String GOD_FORMS_GROUP = "godforms";
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
                boolean hairless = !hasHairBone(character);
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
            configureBlueKaiokenOverlay();
            RaceCharacterConfig saiyanCharacter = ConfigManager.getRaceCharacter("saiyan");
            if (saiyanCharacter != null) {
                saiyanCharacter.getFormSkillsCosts().put(GOD_FORMS_GROUP,
                        new RaceCharacterConfig.FormSkillCost(false, new ArrayList<>(GOD_COSTS)));
            }
            persistSaiyanGodCosts();
            persist("saiyan", saiyan.get(SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN));
            persist("saiyan", saiyan.get(GOD_FORMS_GROUP));
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
            if (hairless) form.setKeepBaseFormHeadBones(true);
            if (ui) {
                form.setEye1Color("#DDF7FF"); form.setEye2Color("#AEEBFF");
                if (key.contains("mastered")) form.setHairColor("#BFC6D2");
                if (hairless) {
                    String bodyColor = key.contains("mastered") ? "#E8F2FF" : "#B8CAD8";
                    form.setHairType("");
                    form.setBodyColor1(bodyColor);
                    form.setBodyColor2("#DDF7FF");
                    form.setBodyColor3(bodyColor);
                }
            } else {
                form.setEye1Color("#D889FF"); form.setEye2Color("#8C24B8");
                form.setHairColor("#8E2AAA");
                if (key.contains("sign") && !hairless) form.setHairType("ssj2");
                if (hairless) {
                    String bodyColor = key.contains("mastered") ? "#76208F" : "#9C42B5";
                    form.setHairType("");
                    form.setBodyColor1(bodyColor);
                    form.setBodyColor2("#D889FF");
                    form.setBodyColor3(bodyColor);
                }
            }
            form.setFormStackable(false); form.setIncompatibleWith(List.of());
        }
    }

    private static boolean hasHairBone(RaceCharacterConfig character) {
        if (character == null || character.getHeadBones() == null) return false;
        for (String bone : character.getHeadBones()) {
            if (bone != null && bone.toLowerCase().contains("hair")) return true;
        }
        return false;
    }
    private static void installSaiyanForms(Map<String, FormConfig> saiyan) {
        FormConfig superGroup = saiyan.get(SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN);
        if (superGroup == null) return;
        if (superGroup.getForms() == null) superGroup.setForms(new LinkedHashMap<>());

        // Migrate the old 10.2.x placement so existing configs cannot keep stale colors or duplicate entries.
        superGroup.getForms().remove("super_saiyan_god");
        superGroup.getForms().remove("super_saiyan_blue");
        superGroup.getForms().remove("super_saiyan_rose");
        superGroup.getForms().remove("super_saiyan_rose_evolved");
        superGroup.getForms().put("super_saiyan_rage",
                superSaiyanRageForm());

        superGroup.getForms().remove("super_saiyan_blue_evolved");
        FormConfig godGroup = saiyan.get(GOD_FORMS_GROUP);
        if (godGroup == null) godGroup = new FormConfig();
        godGroup.setConfigVersion(FormConfig.CURRENT_VERSION);
        godGroup.setGroupName(GOD_FORMS_GROUP);
        godGroup.setFormType(GOD_FORMS_GROUP);
        LinkedHashMap<String, FormConfig.FormData> forms = new LinkedHashMap<>();
        if (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SAIYAN_GOD_FORM.get()) forms.put("super_saiyan_god",
                saiyanForm("super_saiyan_god", 1, "base", "#D92F3D", "#FF655F", "#FF3B35", 3.60, 0.08, false));
        if (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SAIYAN_BLUE_FORM.get()) forms.put("super_saiyan_blue",
                saiyanForm("super_saiyan_blue", 2, "ssj", "#22CFE8", "#91F7FF", "#24DDF4", 4.25, 0.13, true));
        if (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SAIYAN_ROSE_FORM.get()) forms.put("super_saiyan_rose",
                saiyanForm("super_saiyan_rose", 2, "ssj", "#E146A8", "#FF9AD6", "#EE4DB2", 4.25, 0.15, false));
        if (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SAIYAN_BLUE_EVOLVED_FORM.get()) forms.put("super_saiyan_blue_evolved",
                saiyanForm("super_saiyan_blue_evolved", 3, "ssj", "#1594C7", "#7FEAFF", "#148FCB", 5.00, 0.19, false));
        if (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SAIYAN_ROSE_EVOLVED_FORM.get()) forms.put("super_saiyan_rose_evolved",
                saiyanForm("super_saiyan_rose_evolved", 3, "ssj", "#B72B79", "#FFB0E4", "#C52A87", 5.00, 0.19, false));
        godGroup.setForms(forms);
        saiyan.put(GOD_FORMS_GROUP, godGroup);
    }

    private static FormConfig.FormData superSaiyanRageForm() {
        FormConfig.FormData rage = saiyanForm("super_saiyan_rage", 8, "ssj2", "#F4D34A",
                "#63DFFF", "#FFD83D", 3.25, 0.18, false);
        // Anime treatment: a thin cyan light contour hugs the fighter while the full aura stays yellow.
        FormConfig.FormData.OutlineShaderConfig outline = new FormConfig.FormData.OutlineShaderConfig();
        outline.setEnabled(true);
        outline.setPrimaryColor("#37E8FF");
        outline.setSecondaryColor("#B9FAFF");
        outline.setOutlineThickness(1.85D);
        rage.setOutlineShader(outline);
        rage.setAuraType("kakarot");
        rage.setAuraLayer(1);
        rage.setAuraColor("#FFD83D");
        rage.setHasLightnings(true);
        rage.setLightningColor("#63E8FF");
        return rage;
    }

    private static void configureBlueKaiokenOverlay() {
        FormConfig kaioken = ConfigManager.getStackFormGroup(StackForms.GROUP_KAIOKEN);
        if (kaioken == null || kaioken.getForms() == null) return;
        for (FormConfig.FormData form : kaioken.getForms().values()) {
            form.setAuraLayer(3);
            form.setTintIntensity(Math.min(0.22D, Math.max(0.10D, form.getTintIntensity() * 0.45D)));
        }
    }

    private static FormConfig.FormData saiyanForm(String name, int level, String hairType, String hair,
                                                   String eye, String aura, double mult, double drain,
                                                   boolean kaiokenStackable) {
        FormConfig.FormData f = new FormConfig.FormData(); f.setName(name); f.setUnlockOnSkillLevel(level);
        f.setHairType(hairType); f.setHairColor(hair); f.setEye1Color(eye); f.setEye2Color(eye);
        f.setAuraType("kakarot"); f.setAuraColor(aura); f.setAuraLayer(1); f.setHasLightnings(level >= 2);
        f.setLightningColor(eye); f.setStrMultiplier(mult); f.setSkpMultiplier(mult); f.setDefMultiplier(mult * 0.92);
        f.setPwrMultiplier(mult * 1.05); f.setStmMultiplier(1.25); f.setVitMultiplier(1.20); f.setEneMultiplier(1.30); f.setSpeedMultiplier(1.35);
        f.setEnergyDrain(drain); f.setStaminaDrain(drain * 0.35); f.setMaxMastery(100.0); f.setMasteryPerHitDealt(0.05);
        f.setMasteryPerHitReceived(0.04); f.setPassiveMasteryEveryFiveSeconds(0.003); f.setMaxStatsMultiplier(1.35); f.setMaxCostMultiplier(0.70);
        f.setFormStackable(kaiokenStackable); f.setStackOnMastery(0.0); f.setIncompatibleWith(List.of()); return f;
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


    private static void persistSaiyanGodCosts() throws Exception {
        Path file = FMLPaths.CONFIGDIR.get().resolve("dragonminez/races/saiyan/character.json");
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonObject costs = root.has("formSkillsCosts") && root.get("formSkillsCosts").isJsonObject()
                ? root.getAsJsonObject("formSkillsCosts") : new JsonObject();
        costs.add(GOD_FORMS_GROUP, formSkillCost(GOD_COSTS));
        root.add("formSkillsCosts", costs);
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
