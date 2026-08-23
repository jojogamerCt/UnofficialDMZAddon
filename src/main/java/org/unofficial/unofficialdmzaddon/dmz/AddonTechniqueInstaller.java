package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.config.TechniqueConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AddonTechniqueInstaller {
    public static final String TRI_BEAM = "tri_beam";
    public static final String HELLZONE_GRENADE = "hellzone_grenade";
    public static final String PLANET_BURST = "planet_burst";

    private static final List<String> REMOVED_STRIKE_IDS = List.of(
            "jan_ken_fist", "spirit_sword_rush", "sadistic_18"
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object INSTALL_LOCK = new Object();

    private AddonTechniqueInstaller() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AddonTechniqueInstaller::install);
    }

    public static boolean install() { return installInternal(true); }
    public static boolean reinstallAfterConfigSync() { return installInternal(false); }

    private static boolean installInternal(boolean persistLocalConfig) {
        synchronized (INSTALL_LOCK) {
            try {
                SkillsConfig skills = ConfigManager.getSkillsConfig();
                TechniqueConfig techniques = ConfigManager.getTechniqueConfig();

                boolean skillsChanged = removeLegacyStrikeSkillConfig(skills);
                boolean techniqueConfigChanged = removeLegacyStrikeTechniqueConfig(techniques);
                for (String id : REMOVED_STRIKE_IDS) PredefinedTechniques.STRIKE_REGISTRY.remove(id);

                registerKi(TRI_BEAM, "Tenshinhan", KiAttackData.KiType.BEAM,
                        2.25F, 0xFFF4A3, 0xF6D34A, 0xFFFBE6,
                        1.25F, 1.50F, 12, 15, "addon.tri_beam");
                registerKi(HELLZONE_GRENADE, "Piccolo", KiAttackData.KiType.BARRAGE,
                        2.15F, 0xFFE55C, 0xF5B52E, 0xFFF4B0,
                        0.45F, 1.35F, 5, 16, "addon.hellzone_grenade");
                registerKi(PLANET_BURST, "Majin Buu", KiAttackData.KiType.GIANT_BALL,
                        3.25F, 0xFF9BEF, 0xD929CF, 0xFFE2FF,
                        5.00F, 0.50F, 15, 20, "addon.planet_burst");

                skillsChanged |= ensureId(skills.getKiSkills(), TRI_BEAM);
                skillsChanged |= ensureId(skills.getKiSkills(), HELLZONE_GRENADE);
                skillsChanged |= ensureId(skills.getKiSkills(), PLANET_BURST);
                skillsChanged |= ensureCost(skills, TRI_BEAM, 7_000);
                skillsChanged |= ensureCost(skills, HELLZONE_GRENADE, 8_000);
                skillsChanged |= ensureCost(skills, PLANET_BURST, 18_000);
                skillsChanged |= ensureOffering(skills, "piccolo", HELLZONE_GRENADE);

                boolean persisted = true;
                if (persistLocalConfig && skillsChanged) persisted &= persist("skills.json", skills);
                if (persistLocalConfig && techniqueConfigChanged) persisted &= persist("techniques.json", techniques);
                if (!persisted) {
                    UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Techniques are installed at runtime, but one or more DMZ config files could not be updated.");
                }
                return persisted;
            } catch (Exception | LinkageError error) {
                UnofficialDMZAddon.LOGGER.error("[Unofficial DMZ Addon] Failed installing addon techniques into DragonMineZ.", error);
                return false;
            }
        }
    }

    private static void registerKi(String id, String author, KiAttackData.KiType type,
                                   float damageMultiplier, int interiorColor, int exteriorColor, int outlineColor,
                                   float size, float speed, int armorPenetration, int cooldownSeconds,
                                   String nativeAnimationPrefix) {
        KiAttackData data = new KiAttackData();
        data.setId(id);
        data.setName(nameKey(id));
        data.setAuthor(author);
        data.setKiType(type);
        data.setUtility(KiAttackData.Utility.DAMAGE);
        data.setDamageMultiplier(damageMultiplier);
        data.setColorInterior(interiorColor);
        data.setColorExterior(exteriorColor);
        data.setColorOutline(outlineColor);
        data.setSize(size);
        data.setSpeed(speed);
        data.setArmorPenetration(armorPenetration);
        data.setAnimation(nativeAnimationPrefix);
        data.getAllowedRaces().add("ALL");
        data.setCastTime(5 * 20);
        data.calculateDerivedValues();
        data.setCooldown(cooldownSeconds);
        PredefinedTechniques.REGISTRY.put(id, data);
    }

    private static boolean removeLegacyStrikeSkillConfig(SkillsConfig config) {
        boolean changed = config.getStrikeSkills().removeIf(REMOVED_STRIKE_IDS::contains);
        for (String id : REMOVED_STRIKE_IDS) changed |= config.getSkills().remove(id) != null;
        for (List<String> offerings : config.getSkillOfferings().values()) changed |= offerings.removeIf(REMOVED_STRIKE_IDS::contains);
        return changed;
    }

    private static boolean removeLegacyStrikeTechniqueConfig(TechniqueConfig config) {
        boolean changed = false;
        for (String id : REMOVED_STRIKE_IDS) changed |= config.getStrikeAttacks().remove(id) != null;
        return changed;
    }

    private static boolean ensureCost(SkillsConfig config, String id, int defaultCost) {
        SkillsConfig.SkillCosts existing = config.getSkills().get(id);
        if (existing == null) {
            config.getSkills().put(id, new SkillsConfig.SkillCosts(new ArrayList<>(List.of(defaultCost)), new ArrayList<>()));
            return true;
        }
        if (existing.getCosts() == null || existing.getCosts().isEmpty()) {
            existing.setCosts(new ArrayList<>(List.of(defaultCost)));
            return true;
        }
        return false;
    }

    private static boolean ensureOffering(SkillsConfig config, String master, String id) {
        List<String> offerings = config.getSkillOfferings().computeIfAbsent(master, ignored -> new ArrayList<>());
        return ensureId(offerings, id);
    }

    private static boolean ensureId(List<String> ids, String id) {
        if (ids.stream().anyMatch(id::equalsIgnoreCase)) return false;
        ids.add(id);
        return true;
    }

    private static String nameKey(String id) {
        return "technique." + UnofficialDMZAddon.MODID + "." + id;
    }

    public static boolean refreshOwnedTechniques(StatsData stats) {
        boolean changed = false;
        for (String id : REMOVED_STRIKE_IDS) {
            if (stats.getTechniques().getUnlockedTechniques().containsKey(id)) {
                stats.getTechniques().removeTechnique(id);
                changed = true;
            }
        }

        for (String id : List.of(TRI_BEAM, HELLZONE_GRENADE, PLANET_BURST)) {
            TechniqueData owned = stats.getTechniques().getUnlockedTechniques().get(id);
            KiAttackData template = PredefinedTechniques.REGISTRY.get(id);
            if (!(owned instanceof KiAttackData ownedKi) || template == null) continue;
            if (!template.getAnimation().equals(ownedKi.getAnimation())) {
                ownedKi.setAnimation(template.getAnimation());
                changed = true;
            }
        }
        return changed;
    }

    private static boolean persist(String fileName, Object config) {
        Path file = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve(fileName);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            return true;
        } catch (Exception error) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not persist DragonMineZ config '{}': {}", file, error.getMessage());
            return false;
        }
    }
}
