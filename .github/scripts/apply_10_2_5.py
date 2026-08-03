#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one occurrence, found {count}: {old[:100]!r}")
    write(path, text.replace(old, new, 1))


def replace_all_checked(path: str, replacements: list[tuple[str, str]]) -> None:
    text = read(path)
    for old, new in replacements:
        count = text.count(old)
        if count != 1:
            raise RuntimeError(f"{path}: expected one occurrence, found {count}: {old[:100]!r}")
        text = text.replace(old, new, 1)
    write(path, text)


replace_once("gradle.properties", "addon_mod_version=10.2.4", "addon_mod_version=10.2.5")
replace_once("CF-description.txt", "**Unofficial DMZ Addon 10.2.4**", "**Unofficial DMZ Addon 10.2.5**")
replace_once(
    "CF-description.txt",
    "Pack authors and server owners can toggle individual addon forms, dodges, auras, buffs, Alien passive behavior, Space systems, Fly requirements, planet interaction, floor height, pod speed, collision margins, and arrival distance.",
    "Pack authors and server owners can toggle individual addon forms, dodges, auras, buffs, Halal Mode text filtering, Alien passive behavior, Space systems, Fly requirements, planet interaction, floor height, pod speed, collision margins, and arrival distance."
)

replace_all_checked(
    "src/main/java/org/unofficial/unofficialdmzaddon/UnofficialDMZConfig.java",
    [
        (
            "    public static final ForgeConfigSpec.BooleanValue FIRST_PERSON_RACE_MODEL;\n",
            "    public static final ForgeConfigSpec.BooleanValue FIRST_PERSON_RACE_MODEL;\n"
            "    public static final ForgeConfigSpec.BooleanValue HALAL_MODE;\n",
        ),
        (
            '        FIRST_PERSON_RACE_MODEL = builder.define("first_person_race_model", true);\n',
            '        FIRST_PERSON_RACE_MODEL = builder.define("first_person_race_model", true);\n'
            '        HALAL_MODE = builder\n'
            '                .comment("For my muslim fellows",\n'
            '                        "Replaces whole-word God/god labels with Frog/frog on the client")\n'
            '                .define("halal_mode", false);\n',
        ),
    ],
)

replace_all_checked(
    "src/main/java/org/unofficial/unofficialdmzaddon/dmz/AddonClassInstaller.java",
    [
        (
            "0.65, 10.0, 7.0, 0.55, 0.8, 1.2, 1.4, 1.5, 2.4,",
            "0.65, 10.0, 7.0, 0.55, 0.8, 0.168, 1.4, 1.5, 2.4,",
        ),
        (
            "1.35, 4.0, 11.0, 1.0, 1.8, 0.9, 1.4, 0.7, 1.2,",
            "1.35, 4.0, 11.0, 1.0, 1.8, 0.18, 1.4, 0.7, 1.2,",
        ),
        (
            "2.15, 5.0, 10.0, 0.8, 0.8, 1.65, 2.4, 0.55, 1.0,",
            "2.15, 5.0, 10.0, 0.8, 0.8, 0.36, 2.4, 0.55, 1.0,",
        ),
        (
            "1.2, 7.0, 9.0, 1.0, 1.2, 1.2, 1.4, 1.25, 1.5,",
            "1.2, 7.0, 9.0, 1.0, 1.2, 0.24, 1.4, 1.25, 1.5,",
        ),
        (
            "        return changed;\n    }\n\n    private static void configure(",
            "        changed |= migrateLegacyDefenseScaling(config, \"kiadept\", 1.2, 0.168);\n"
            "        changed |= migrateLegacyDefenseScaling(config, \"duelist\", 0.9, 0.18);\n"
            "        changed |= migrateLegacyDefenseScaling(config, \"vanguard\", 1.65, 0.36);\n"
            "        changed |= migrateLegacyDefenseScaling(config, \"tactician\", 1.2, 0.24);\n"
            "        return changed;\n"
            "    }\n\n"
            "    private static boolean migrateLegacyDefenseScaling(RaceStatsConfig config,\n"
            "                                                       String classKey,\n"
            "                                                       double legacyValue,\n"
            "                                                       double balancedValue) {\n"
            "        RaceStatsConfig.ClassStats stats = config.getClasses().get(classKey);\n"
            "        if (stats == null || stats.getStatScaling() == null) return false;\n"
            "        double current = stats.getStatScaling().getDefenseScaling();\n"
            "        if (Math.abs(current - legacyValue) > 0.000001D) return false;\n"
            "        stats.getStatScaling().setDefenseScaling(balancedValue);\n"
            "        return true;\n"
            "    }\n\n"
            "    private static void configure(",
        ),
    ],
)

replace_all_checked(
    "src/main/java/org/unofficial/unofficialdmzaddon/dmz/TransformationInstaller.java",
    [
        (
            "            if (orange != null) {\n"
            "                orange.setFormStackable(false);\n"
            "                orange.setStackDrainMultiplier(1.0);\n"
            "                return true;\n"
            "            }\n",
            "            if (orange != null) {\n"
            "                orange.setFormStackable(false);\n"
            "                orange.setStackDrainMultiplier(1.0);\n"
            "                orange.setKeepBaseFormHeadBones(true);\n"
            "                return true;\n"
            "            }\n",
        ),
        (
            "        formData.setName(formName);\n"
            "        formData.setUnlockOnSkillLevel(unlockLevel);\n",
            "        formData.setName(formName);\n"
            "        formData.setUnlockOnSkillLevel(unlockLevel);\n"
            "        formData.setKeepBaseFormHeadBones(true);\n",
        ),
        (
            "                existing.addProperty(\"formStackable\", false);\n"
            "                existing.addProperty(\"stackDrainMultiplier\", 1.0);\n"
            "                if (SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER.equals(formKey)) {\n",
            "                existing.addProperty(\"formStackable\", false);\n"
            "                existing.addProperty(\"stackDrainMultiplier\", 1.0);\n"
            "                if (SpecialRaceFormsDefinitions.NAMEKIAN_FORM_ORANGE.equals(formKey)) {\n"
            "                    existing.addProperty(\"keepBaseFormHeadBones\", true);\n"
            "                }\n"
            "                if (SpecialRaceFormsDefinitions.ALIEN_FORM_FULL_POWER.equals(formKey)) {\n",
        ),
        (
            "        form.addProperty(\"name\", formName);\n"
            "        form.addProperty(\"unlockOnSkillLevel\", unlockLevel);\n",
            "        form.addProperty(\"name\", formName);\n"
            "        form.addProperty(\"unlockOnSkillLevel\", unlockLevel);\n"
            "        form.addProperty(\"keepBaseFormHeadBones\", true);\n",
        ),
    ],
)

divine_path = "src/main/java/org/unofficial/unofficialdmzaddon/dmz/DivineProgressionInstaller.java"
replace_all_checked(
    divine_path,
    [
        (
            '    private static final String ULTRA_EGO_MASTER = "vegeta";\n',
            '    private static final String ULTRA_EGO_MASTER = "vegeta";\n'
            "    private static final double UI_SIGN_STAMINA_DRAIN = 0.012;\n"
            "    private static final double UI_MASTERED_STAMINA_DRAIN = 0.018;\n"
            "    private static final double UI_TRUE_STAMINA_DRAIN = 0.024;\n",
        ),
        (
            "        defaults.getForms().forEach(existing.getForms()::putIfAbsent);\n"
            "        copyCanonicalDivineAuras(existing, defaults);\n",
            "        defaults.getForms().forEach(existing.getForms()::putIfAbsent);\n"
            "        copyCanonicalDivineAuras(existing, defaults);\n"
            "        if (StackForms.GROUP_ULTRAINSTINCT.equals(groupName)) balanceUltraInstinctDrain(existing);\n",
        ),
        (
            "        boolean ui = !UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()\n"
            "                || persistConfigBackedGroup(createExtendedUltraInstinctGroup());\n"
            "        boolean ue = !UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()\n"
            "                || persistConfigBackedGroup(createOriginalUltraEgoGroup());\n",
            "        boolean ui = UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()\n"
            "                ? persistConfigBackedGroup(createExtendedUltraInstinctGroup())\n"
            "                : archiveDisabledRaceFormFile(StackForms.GROUP_ULTRAINSTINCT);\n"
            "        boolean ue = UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()\n"
            "                ? persistConfigBackedGroup(createOriginalUltraEgoGroup())\n"
            "                : archiveDisabledRaceFormFile(StackForms.GROUP_ULTRAEGO);\n",
        ),
        (
            "            if (UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()) {\n"
            "                ensureString(formSkills, StackForms.GROUP_ULTRAINSTINCT);\n"
            "                ensureSkillCosts(skills, StackForms.GROUP_ULTRAINSTINCT, ULTRA_INSTINCT_COSTS);\n"
            "            }\n"
            "            if (UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()) {\n"
            "                ensureString(formSkills, StackForms.GROUP_ULTRAEGO);\n"
            "                ensureSkillCosts(skills, StackForms.GROUP_ULTRAEGO, ULTRA_EGO_COSTS);\n"
            "            }\n",
            "            if (UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()) {\n"
            "                ensureString(formSkills, StackForms.GROUP_ULTRAINSTINCT);\n"
            "                ensureSkillCosts(skills, StackForms.GROUP_ULTRAINSTINCT, ULTRA_INSTINCT_COSTS);\n"
            "            } else {\n"
            "                removeString(formSkills, StackForms.GROUP_ULTRAINSTINCT);\n"
            "                skills.remove(StackForms.GROUP_ULTRAINSTINCT);\n"
            "            }\n"
            "            if (UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()) {\n"
            "                ensureString(formSkills, StackForms.GROUP_ULTRAEGO);\n"
            "                ensureSkillCosts(skills, StackForms.GROUP_ULTRAEGO, ULTRA_EGO_COSTS);\n"
            "            } else {\n"
            "                removeString(formSkills, StackForms.GROUP_ULTRAEGO);\n"
            "                skills.remove(StackForms.GROUP_ULTRAEGO);\n"
            "            }\n",
        ),
        (
            "    private static boolean persistConfigBackedGroup(FormConfig defaults) {\n",
            "    private static boolean archiveDisabledRaceFormFile(String groupName) {\n"
            "        Path file = FMLPaths.CONFIGDIR.get()\n"
            "                .resolve(\"dragonminez\")\n"
            "                .resolve(\"races\")\n"
            "                .resolve(SpecialRaceFormsDefinitions.SAIYAN_RACE)\n"
            "                .resolve(\"forms\")\n"
            "                .resolve(groupName + \".json\");\n"
            "        if (!Files.exists(file)) return true;\n"
            "        Path backup = file.resolveSibling(groupName + \".addon-disabled\");\n"
            "        try {\n"
            "            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);\n"
            "            return true;\n"
            "        } catch (IOException e) {\n"
            "            UnofficialDMZAddon.LOGGER.warn(\n"
            "                    \"[Unofficial DMZ Addon] Failed disabling form file '{}': {}\", file, e.getMessage());\n"
            "            return false;\n"
            "        }\n"
            "    }\n\n"
            "    private static boolean persistConfigBackedGroup(FormConfig defaults) {\n",
        ),
        (
            "                JsonObject form = forms.getAsJsonObject(entry.getKey());\n"
            "                if (form != null) writeCanonicalAura(form, entry.getValue());\n",
            "                JsonObject form = forms.getAsJsonObject(entry.getKey());\n"
            "                if (form != null) writeCanonicalAura(form, entry.getValue());\n"
            "                if (form != null && StackForms.GROUP_ULTRAINSTINCT.equals(groupName)) {\n"
            "                    migrateUltraInstinctDrain(form, entry.getKey());\n"
            "                }\n",
        ),
        (
            "        configureUltraInstinctAuras(group);\n"
            "        return group;\n"
            "    }\n\n"
            "    private static FormConfig createOriginalUltraEgoGroup()",
            "        configureUltraInstinctAuras(group);\n"
            "        balanceUltraInstinctDrain(group);\n"
            "        return group;\n"
            "    }\n\n"
            "    private static FormConfig createOriginalUltraEgoGroup()",
        ),
        (
            "                        0.055, 0.085, 0.07, 0.08, 1.38,",
            "                        0.055, UI_TRUE_STAMINA_DRAIN, 0.07, 0.08, 1.38,",
        ),
        (
            "    private static void copyCanonicalDivineAuras(FormConfig target, FormConfig defaults) {\n",
            "    private static void balanceUltraInstinctDrain(FormConfig group) {\n"
            "        if (group == null || group.getForms() == null) return;\n"
            "        migrateUltraInstinctDrain(group.getFormByKey(StackForms.ULTRAINSTINCT_SIGN), 0.03, UI_SIGN_STAMINA_DRAIN);\n"
            "        migrateUltraInstinctDrain(group.getFormByKey(StackForms.ULTRAINSTINCT_MASTERED), 0.06, UI_MASTERED_STAMINA_DRAIN);\n"
            "        migrateUltraInstinctDrain(group.getFormByKey(UltraInstinctDefinitions.FORM_TRUE), 0.085, UI_TRUE_STAMINA_DRAIN);\n"
            "    }\n\n"
            "    private static void migrateUltraInstinctDrain(FormConfig.FormData form, double legacy, double balanced) {\n"
            "        if (form == null) return;\n"
            "        double current = form.getStaminaDrain();\n"
            "        if (current <= 0.0 || near(current, legacy)) form.setStaminaDrain(balanced);\n"
            "    }\n\n"
            "    private static void migrateUltraInstinctDrain(JsonObject form, String formKey) {\n"
            "        double legacy;\n"
            "        double balanced;\n"
            "        if (StackForms.ULTRAINSTINCT_SIGN.equalsIgnoreCase(formKey)) {\n"
            "            legacy = 0.03;\n"
            "            balanced = UI_SIGN_STAMINA_DRAIN;\n"
            "        } else if (StackForms.ULTRAINSTINCT_MASTERED.equalsIgnoreCase(formKey)) {\n"
            "            legacy = 0.06;\n"
            "            balanced = UI_MASTERED_STAMINA_DRAIN;\n"
            "        } else if (UltraInstinctDefinitions.FORM_TRUE.equalsIgnoreCase(formKey)) {\n"
            "            legacy = 0.085;\n"
            "            balanced = UI_TRUE_STAMINA_DRAIN;\n"
            "        } else {\n"
            "            return;\n"
            "        }\n"
            "        if (!form.has(\"staminaDrain\") || !form.get(\"staminaDrain\").isJsonPrimitive()\n"
            "                || near(form.get(\"staminaDrain\").getAsDouble(), legacy)) {\n"
            "            form.addProperty(\"staminaDrain\", balanced);\n"
            "        }\n"
            "    }\n\n"
            "    private static void copyCanonicalDivineAuras(FormConfig target, FormConfig defaults) {\n",
        ),
    ],
)

replace_all_checked(
    "src/main/java/org/unofficial/unofficialdmzaddon/dmz/UltraInstinctCombatHandler.java",
    [
        (
            "                energyCostAtZeroMastery = 0.025;\n"
            "                energyCostAtFullMastery = 0.018;\n",
            "                energyCostAtZeroMastery = 0.012;\n"
            "                energyCostAtFullMastery = 0.008;\n",
        ),
        (
            "                energyCostAtZeroMastery = 0.021;\n"
            "                energyCostAtFullMastery = 0.014;\n",
            "                energyCostAtZeroMastery = 0.010;\n"
            "                energyCostAtFullMastery = 0.006;\n",
        ),
        (
            "            case 3 -> {\n"
            "                chanceAtZeroMastery = 0.78;\n"
            "                chanceAtFullMastery = 0.91;\n"
            "                distanceAtZeroMastery = 1.25;\n"
            "                distanceAtFullMastery = 1.60;\n"
            "                verticalLift = 0.18;\n"
            "                energyCostAtZeroMastery = 0.017;\n"
            "                energyCostAtFullMastery = 0.010;\n"
            "            }\n"
            "            default -> {\n"
            "                chanceAtZeroMastery = 0.88;\n"
            "                chanceAtFullMastery = 0.97;\n"
            "                distanceAtZeroMastery = 1.50;\n"
            "                distanceAtFullMastery = 1.90;\n"
            "                verticalLift = 0.22;\n"
            "                energyCostAtZeroMastery = 0.013;\n"
            "                energyCostAtFullMastery = 0.007;\n"
            "            }\n",
            "            default -> {\n"
            "                chanceAtZeroMastery = 0.88;\n"
            "                chanceAtFullMastery = 0.97;\n"
            "                distanceAtZeroMastery = 1.50;\n"
            "                distanceAtFullMastery = 1.90;\n"
            "                verticalLift = 0.22;\n"
            "                energyCostAtZeroMastery = 0.0075;\n"
            "                energyCostAtFullMastery = 0.004;\n"
            "            }\n",
        ),
        (
            "        return Math.max(0.0, Math.min(1.0, (tier - 1) / 3.0));",
            "        return Math.max(0.0, Math.min(1.0, (tier - 1) / 2.0));",
        ),
    ],
)

write(
    "src/main/java/org/unofficial/unofficialdmzaddon/mixin/RadialLayoutStoreMixin.java",
    '''package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.radial.RadialLayoutStore;
import com.dragonminez.common.util.lists.StackForms;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(value = RadialLayoutStore.class, remap = false)
public abstract class RadialLayoutStoreMixin {
    @Shadow @Final private static Map<String, List<String>> ORDER;
    private static boolean unofficialdmzaddon$migrating;

    @Inject(method = "ensureLoaded", at = @At("RETURN"))
    private static void unofficialdmzaddon$migrateAndPruneLayout(CallbackInfo ci) {
        if (unofficialdmzaddon$migrating) return;

        boolean changed = migrateLegacyGodLayout();
        if (!UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()) {
            changed |= removeDisabledFormGroup(StackForms.GROUP_ULTRAINSTINCT);
        }
        if (!UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()) {
            changed |= removeDisabledFormGroup(StackForms.GROUP_ULTRAEGO);
        }
        if (!changed) return;

        unofficialdmzaddon$migrating = true;
        try {
            RadialLayoutStore.save();
        } finally {
            unofficialdmzaddon$migrating = false;
        }
    }

    private static boolean migrateLegacyGodLayout() {
        List<String> legacy = ORDER.get("superforms:supersaiyan");
        if (legacy == null) legacy = new ArrayList<>();
        boolean changed = false;
        List<String> god = new ArrayList<>(ORDER.getOrDefault("godforms:godforms", Collections.emptyList()));
        for (String form : List.of("super_saiyan_god", "super_saiyan_blue", "super_saiyan_rose", "super_saiyan_blue_evolved")) {
            String oldKey = "form:supersaiyan:" + form;
            String newKey = "form:godforms:" + form;
            if (legacy.remove(oldKey)) {
                if (!god.contains(newKey)) god.add(newKey);
                changed = true;
            }
        }
        if (changed) ORDER.put("godforms:godforms", god);
        return changed;
    }

    private static boolean removeDisabledFormGroup(String group) {
        boolean changed = ORDER.entrySet().removeIf(entry ->
                entry.getKey().equals(group)
                        || entry.getKey().endsWith(":" + group)
                        || entry.getKey().startsWith(group + ":"));

        String groupKey = "group:" + group;
        String stackGroupKey = "group:stack:" + group;
        String formPrefix = "form:" + group + ":";
        for (List<String> values : ORDER.values()) {
            changed |= values.removeIf(key ->
                    key.equals(groupKey) || key.equals(stackGroupKey) || key.startsWith(formPrefix));
        }
        return changed;
    }
}
''',
)

write(
    "src/main/java/org/unofficial/unofficialdmzaddon/client/HalalModeText.java",
    '''package org.unofficial.unofficialdmzaddon.client;

import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

import java.util.regex.Pattern;

public final class HalalModeText {
    private static final Pattern TITLE_CASE = Pattern.compile("\\\\bGod\\\\b");
    private static final Pattern LOWER_CASE = Pattern.compile("\\\\bgod\\\\b");
    private static final Pattern UPPER_CASE = Pattern.compile("\\\\bGOD\\\\b");

    private HalalModeText() {
    }

    public static String apply(String value) {
        if (value == null || value.isEmpty() || !UnofficialDMZConfig.HALAL_MODE.get()) return value;
        String filtered = TITLE_CASE.matcher(value).replaceAll("Frog");
        filtered = LOWER_CASE.matcher(filtered).replaceAll("frog");
        return UPPER_CASE.matcher(filtered).replaceAll("FROG");
    }
}
''',
)

write(
    "src/main/java/org/unofficial/unofficialdmzaddon/mixin/ClientLanguageMixin.java",
    '''package org.unofficial.unofficialdmzaddon.mixin;

import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.client.HalalModeText;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {
    @Inject(
            method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void unofficialdmzaddon$applyHalalMode(
            String key,
            CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(HalalModeText.apply(cir.getReturnValue()));
    }
}
''',
)

write(
    "src/main/java/org/unofficial/unofficialdmzaddon/mixin/SearchFlightAuraMixin.java",
    '''package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skill;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AuraRenderer.class, remap = false)
public abstract class SearchFlightAuraMixin {
    @Inject(
            method = "renderShaderAura",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/render/effects/AuraRenderer;executeAuraShaderDraw(Lnet/minecraft/world/entity/player/Player;Lcom/dragonminez/client/render/effects/AuraRenderer$CachedAuraData;Lcom/dragonminez/client/render/effects/AuraRenderer$AuraLayer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Minecraft;Lorg/joml/Matrix4f;FFZ)V"
            )
    )
    private static void unofficialdmzaddon$turnSearchFlightAuraSideways(
            PlayerEffectQueue.AuraRenderEntry entry,
            PoseStack poseStack,
            Minecraft minecraft,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        Player player = entry.player();
        if (player == null || player.getDeltaMovement().lengthSqr() < 0.01D) return;

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            Skill fly = data.getSkills().getSkill("fly");
            if (fly == null || !fly.isActive() || data.getStatus().getFlightMode() != Status.FLIGHT_SEARCH) return;

            float yaw = Mth.rotLerp(entry.partialTick(), player.yRotO, player.getYRot());
            float pitch = Mth.clamp(Mth.lerp(entry.partialTick(), player.xRotO, player.getXRot()), -75.0F, 75.0F);

            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F + pitch));
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.translate(0.0D, -0.45D, -0.20D);
        });
    }
}
''',
)

mixins_path = ROOT / "src/main/resources/unofficialdmzaddon.mixins.json"
mixins = json.loads(mixins_path.read_text(encoding="utf-8"))
client_mixins = mixins.setdefault("client", [])
for name in ("ClientLanguageMixin", "SearchFlightAuraMixin"):
    if name not in client_mixins:
        client_mixins.append(name)
mixins_path.write_text(json.dumps(mixins, indent=2) + "\n", encoding="utf-8")

lang_path = ROOT / "src/main/resources/assets/dragonminez/lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang["config.unofficialdmzaddon.halal_mode"] = "Halal Mode"
lang["config.unofficialdmzaddon.halal_mode.subtitle"] = "For my muslim fellows"
lang["skill.dragonminez.ultrainstinct.desc"] = (
    "Universal divine form tree. Every tier automatically cancels successful dodges. "
    "Sign has a 50-70% dodge chance, Mastered 65-82%, and True 88-97%, based on mastery; "
    "higher tiers dodge farther and use less energy, while all UI tiers have reduced stamina exhaustion. "
    "Hairless races receive matching body colors."
)
lang_path.write_text(json.dumps(lang, indent=4, ensure_ascii=True) + "\n", encoding="utf-8")

write(
    "changelog-v10.2.5.txt",
    '''Unofficial DMZ Addon 10.2.5

Accessibility and text
- Added Halal Mode under the addon rendering settings with the subtitle "For my muslim fellows".
- Halal Mode replaces whole-word God/god labels with Frog/frog at display time without renaming internal form IDs, config keys, saves, or DragonMineZ data.

Flight visuals
- Rotated the aura sideways while a player is moving with DragonMineZ Search Flight, matching the forward-flying anime and manga silhouette while leaving Combat Flight unchanged.

Class balance
- Rebalanced addon-class defense growth against DragonMineZ's native class range: Ki Adept 0.168, Duelist 0.18, Vanguard 0.36, and Tactician 0.24.
- Migrates only the exact 10.2.4 addon defaults so server and modpack custom defense values remain untouched.

Ultra Instinct and Ultra Ego
- Reduced Ultra Instinct stamina drain to 0.012 for Sign, 0.018 for Mastered, and 0.024 for True.
- Reduced automatic-dodge energy costs while preserving the documented 50-70%, 65-82%, and 88-97% mastery-scaled chances.
- Corrected the post-Autonomous three-tier scaling so True Ultra Instinct receives the intended top-tier dodge distance and combat scaling.
- Disabling Ultra Instinct or Ultra Ego now removes its runtime group, skill-tree entry, persisted race form file, and stale radial-menu layout entries.
- Disabled form files are archived with an .addon-disabled suffix so administrators can restore or compare previous tuning.

Namekian forms
- Orange Form now preserves DragonMineZ's base Namekian head bones, keeping ears and antennae visible for newly generated and existing configs.

Compatibility
- All fixes are implemented in Unofficial DMZ Addon through addon installers, config migration, and mixins; DragonMineZ base source is not modified.
''',
)

print("Applied Unofficial DMZ Addon 10.2.5 patch.")
