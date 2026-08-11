package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.config.TechniqueConfig;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.StatsData;
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

/**
 * Installs the addon's predefined techniques into DragonMineZ's native registries and skill
 * configuration. The operation is deliberately idempotent so it can be repeated after DMZ
 * replaces its client-side configuration with a server-synced copy.
 */
@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AddonTechniqueInstaller {
    public static final String TRI_BEAM = "tri_beam";
    public static final String HELLZONE_GRENADE = "hellzone_grenade";
    public static final String PLANET_BURST = "planet_burst";
    public static final String JAN_KEN_FIST = "jan_ken_fist";
    public static final String SPIRIT_SWORD_RUSH = "spirit_sword_rush";
    public static final String SADISTIC_18 = "sadistic_18";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object INSTALL_LOCK = new Object();

    private AddonTechniqueInstaller() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AddonTechniqueInstaller::install);
    }

    /** Installs the runtime entries and persists newly missing native DMZ configuration entries. */
    public static boolean install() {
        return installInternal(true);
    }

    /**
     * Reapplies the runtime entries without writing local files. Call this after DMZ config sync;
     * on a multiplayer client the returned config objects are the authoritative synced copies.
     */
    public static boolean reinstallAfterConfigSync() {
        return installInternal(false);
    }

    private static boolean installInternal(boolean persistLocalConfig) {
        synchronized (INSTALL_LOCK) {
            try {
                SkillsConfig skills = ConfigManager.getSkillsConfig();
                TechniqueConfig techniques = ConfigManager.getTechniqueConfig();

                boolean techniqueConfigChanged = false;
                techniqueConfigChanged |= ensureStrikeConfig(techniques, JAN_KEN_FIST, 160);
                techniqueConfigChanged |= ensureStrikeConfig(techniques, SPIRIT_SWORD_RUSH, 340);
                techniqueConfigChanged |= ensureStrikeConfig(techniques, SADISTIC_18, 280);

                registerKi(TRI_BEAM, "Tenshinhan", KiAttackData.KiType.BEAM,
                        2.25F, 0xFFF4A3, 0xF6D34A, 0xFFFBE6,
                        1.25F, 1.50F, 12, 15, "addon.tri_beam");
                registerKi(HELLZONE_GRENADE, "Piccolo", KiAttackData.KiType.BARRAGE,
                        2.15F, 0xFFE55C, 0xF5B52E, 0xFFF4B0,
                        0.45F, 1.35F, 5, 16, "addon.hellzone_grenade");
                registerKi(PLANET_BURST, "Majin Buu", KiAttackData.KiType.GIANT_BALL,
                        3.25F, 0xFF9BEF, 0xD929CF, 0xFFE2FF,
                        5.00F, 0.50F, 15, 20, "addon.planet_burst");

                registerStrike(JAN_KEN_FIST, "Son Goku", 1.30F, 28, "addon.jan_ken_fist");
                registerStrike(SPIRIT_SWORD_RUSH, "Vegetto", 2.15F, 38, "addon.spirit_sword_rush");
                registerStrike(SADISTIC_18, "Android 18", 1.85F, 38, "addon.sadistic_18");

                boolean skillsChanged = false;
                skillsChanged |= ensureId(skills.getKiSkills(), TRI_BEAM);
                skillsChanged |= ensureId(skills.getKiSkills(), HELLZONE_GRENADE);
                skillsChanged |= ensureId(skills.getKiSkills(), PLANET_BURST);
                skillsChanged |= ensureId(skills.getStrikeSkills(), JAN_KEN_FIST);
                skillsChanged |= ensureId(skills.getStrikeSkills(), SPIRIT_SWORD_RUSH);
                skillsChanged |= ensureId(skills.getStrikeSkills(), SADISTIC_18);

                skillsChanged |= ensureCost(skills, TRI_BEAM, 7_000);
                skillsChanged |= ensureCost(skills, HELLZONE_GRENADE, 8_000);
                skillsChanged |= ensureCost(skills, PLANET_BURST, 18_000);
                skillsChanged |= ensureCost(skills, JAN_KEN_FIST, 3_000);
                skillsChanged |= ensureCost(skills, SPIRIT_SWORD_RUSH, 12_000);
                skillsChanged |= ensureCost(skills, SADISTIC_18, 8_000);

                skillsChanged |= ensureOffering(skills, "piccolo", HELLZONE_GRENADE);
                skillsChanged |= ensureOffering(skills, "goku", JAN_KEN_FIST);

                boolean persisted = true;
                if (persistLocalConfig && skillsChanged) {
                    persisted &= persist("skills.json", skills);
                }
                if (persistLocalConfig && techniqueConfigChanged) {
                    persisted &= persist("techniques.json", techniques);
                }

                if (!persisted) {
                    UnofficialDMZAddon.LOGGER.warn(
                            "[Unofficial DMZ Addon] Techniques are installed at runtime, but one or more DMZ config files could not be updated."
                    );
                }
                return persisted;
            } catch (Exception | LinkageError error) {
                UnofficialDMZAddon.LOGGER.error(
                        "[Unofficial DMZ Addon] Failed installing addon techniques into DragonMineZ.", error
                );
                return false;
            }
        }
    }

    private static void registerKi(String id,
                                   String author,
                                   KiAttackData.KiType type,
                                   float damageMultiplier,
                                   int interiorColor,
                                   int exteriorColor,
                                   int outlineColor,
                                   float size,
                                   float speed,
                                   int armorPenetration,
                                   int cooldownSeconds,
                                   String animation) {
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
        data.setAnimation(animation);
        data.getAllowedRaces().add("ALL");
        data.calculateDerivedValues();
        // calculateDerivedValues supplies TP cost and normalization. Predefined cooldown is an
        // authored balance value, expressed in seconds just like DMZ's native Ki templates.
        data.setCooldown(cooldownSeconds);
        PredefinedTechniques.REGISTRY.put(id, data);
    }

    private static void registerStrike(String id,
                                       String author,
                                       float damageMultiplier,
                                       int durationTicks,
                                       String animationId) {
        StrikeAttackData data = new StrikeAttackData();
        data.setId(id);
        data.setName(nameKey(id));
        data.setAuthor(author);
        data.setDamageMultiplier(damageMultiplier);
        data.setAnimationId(animationId);
        data.setDurationTicks(durationTicks);
        data.applyConfigDefaults();
        PredefinedTechniques.STRIKE_REGISTRY.put(id, data);
    }

    private static boolean ensureStrikeConfig(TechniqueConfig config, String id, int cooldownTicks) {
        TechniqueConfig.StrikeAttackConfig existing = config.getStrikeAttacks().get(id);
        if (existing != null) return false;

        TechniqueConfig.StrikeAttackConfig created = TechniqueConfig.StrikeAttackConfig.defaults();
        created.setCooldownTicks(cooldownTicks);
        config.getStrikeAttacks().put(id, created);
        return true;
    }

    private static boolean ensureCost(SkillsConfig config, String id, int defaultCost) {
        SkillsConfig.SkillCosts existing = config.getSkills().get(id);
        if (existing == null) {
            config.getSkills().put(id, new SkillsConfig.SkillCosts(
                    new ArrayList<>(List.of(defaultCost)), new ArrayList<>()
            ));
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

    /** Migrates saved copies so existing worlds receive the final animations and durations. */
    public static boolean refreshOwnedTechniques(StatsData stats) {
        boolean changed = false;
        for (String id : List.of(TRI_BEAM, HELLZONE_GRENADE, PLANET_BURST, JAN_KEN_FIST, SPIRIT_SWORD_RUSH, SADISTIC_18)) {
            TechniqueData owned = stats.getTechniques().getUnlockedTechniques().get(id);
            TechniqueData template = owned instanceof StrikeAttackData ? PredefinedTechniques.STRIKE_REGISTRY.get(id) : PredefinedTechniques.REGISTRY.get(id);
            if (owned == null || template == null) continue;
            int experience = owned.getExperience();
            if (owned instanceof KiAttackData ownedKi && template instanceof KiAttackData templateKi) {
                if (!templateKi.getAnimation().equals(ownedKi.getAnimation())) { ownedKi.setAnimation(templateKi.getAnimation()); changed = true; }
            } else if (owned instanceof StrikeAttackData ownedStrike && template instanceof StrikeAttackData templateStrike) {
                if (!templateStrike.getAnimationId().equals(ownedStrike.getAnimationId()) || templateStrike.getDurationTicks() != ownedStrike.getDurationTicks()) {
                    ownedStrike.setAnimationId(templateStrike.getAnimationId()); ownedStrike.setDurationTicks(templateStrike.getDurationTicks()); changed = true;
                }
            }
            owned.setExperience(experience);
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
            UnofficialDMZAddon.LOGGER.warn(
                    "[Unofficial DMZ Addon] Could not persist DragonMineZ config '{}': {}",
                    file, error.getMessage()
            );
            return false;
        }
    }
}
