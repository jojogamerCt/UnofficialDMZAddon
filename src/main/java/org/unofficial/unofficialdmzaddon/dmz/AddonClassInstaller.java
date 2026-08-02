package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.passives.IClassPassive;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.loading.FMLPaths;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Adds addon classes to every DMZ race and registers their native class passives. */
public final class AddonClassInstaller {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AddonClassInstaller() {}

    public static void install() {
        ClassPassives.register(new KiAdeptPassive());
        ClassPassives.register(new DuelistPassive());
        ClassPassives.register(new VanguardPassive());
        ClassPassives.register(new TacticianPassive());

        for (Map.Entry<String, RaceStatsConfig> race : ConfigManager.getAllRaceStats().entrySet()) {
            boolean changed = addClasses(race.getValue());
            if (changed) save(race.getKey(), race.getValue());
        }
    }

    private static boolean addClasses(RaceStatsConfig config) {
        boolean changed = false;
        if (!config.getClasses().containsKey("kiadept")) {
            configure(config.getClassStats("kiadept"), 0, 0, 0, 5, 10, 10,
                    0.65, 10.0, 7.0, 0.55, 0.8, 1.2, 1.4, 1.5, 2.4,
                    map("cooldownMultiplier", 0.82, "durationMultiplier", 1.10));
            changed = true;
        }
        if (!config.getClasses().containsKey("duelist")) {
            configure(config.getClassStats("duelist"), 5, 10, 0, 5, 0, 0,
                    1.35, 4.0, 11.0, 1.0, 1.8, 0.9, 1.4, 0.7, 1.2,
                    map("strikeMultiplier", 1.10, "critBonus", 0.08, "armorPen", 0.05));
            changed = true;
        }
        if (!config.getClasses().containsKey("vanguard")) {
            configure(config.getClassStats("vanguard"), 5, 0, 10, 10, 0, 0,
                    2.15, 5.0, 10.0, 0.8, 0.8, 1.65, 2.4, 0.55, 1.0,
                    map("healthRegen", 1.25, "staminaRegen", 1.10, "healingReceived", 1.10));
            changed = true;
        }
        if (!config.getClasses().containsKey("tactician")) {
            configure(config.getClassStats("tactician"), 5, 5, 5, 5, 5, 5,
                    1.2, 7.0, 9.0, 1.0, 1.2, 1.2, 1.4, 1.25, 1.5,
                    map("meleeAfterKi", 1.18, "kiAfterMelee", 0.75, "windowTicks", 80.0));
            changed = true;
        }
        return changed;
    }

    private static void configure(RaceStatsConfig.ClassStats stats, int str, int skp, int res, int vit, int pwr, int ene,
                                  double hp5, double ep5, double sp5, double strScale, double skpScale,
                                  double defScale, double vitScale, double pwrScale, double eneScale,
                                  Map<String, Double> passiveValues) {
        stats.getBaseStats().setStrength(str);
        stats.getBaseStats().setStrikePower(skp);
        stats.getBaseStats().setResistance(res);
        stats.getBaseStats().setVitality(vit);
        stats.getBaseStats().setKiPower(pwr);
        stats.getBaseStats().setEnergy(ene);
        stats.setBaseHp5(hp5);
        stats.setBaseEp5(ep5);
        stats.setBaseSp5(sp5);
        stats.getStatScaling().setStrengthScaling(strScale);
        stats.getStatScaling().setStrikePowerScaling(skpScale);
        stats.getStatScaling().setDefenseScaling(defScale);
        stats.getStatScaling().setStaminaScaling(1.2);
        stats.getStatScaling().setVitalityScaling(vitScale);
        stats.getStatScaling().setKiPowerScaling(pwrScale);
        stats.getStatScaling().setEnergyScaling(eneScale);
        stats.getPassive().setEnabled(true);
        stats.getPassive().setValues(passiveValues);
    }

    private static Map<String, Double> map(Object... values) {
        Map<String, Double> result = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], (Double) values[i + 1]);
        return result;
    }

    private static void save(String race, RaceStatsConfig config) {
        Path path = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("races").resolve(race).resolve("stats.json");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not save addon classes for race {}: {}", race, e.getMessage());
        }
    }

    private static final class KiAdeptPassive implements IClassPassive {
        public String classKey() { return "kiadept"; }
        public double kiCooldownMultiplier(StatsData data, KiAttackData ki) { return ClassPassives.value(data, "cooldownMultiplier", 0.82); }
        public double secondaryDurationMultiplier(StatsData data, KiAttackData ki) { return ClassPassives.value(data, "durationMultiplier", 1.10); }
    }

    private static final class DuelistPassive implements IClassPassive {
        public String classKey() { return "duelist"; }
        public double strikeDamageMultiplier(StatsData data, LivingEntity target) { return ClassPassives.value(data, "strikeMultiplier", 1.10); }
        public double critChanceBonus(StatsData data) { return ClassPassives.value(data, "critBonus", 0.08); }
        public double armorPenBonus(StatsData data) { return ClassPassives.value(data, "armorPen", 0.05); }
    }

    private static final class VanguardPassive implements IClassPassive {
        public String classKey() { return "vanguard"; }
        public double healthRegenMultiplier(StatsData data) { return ClassPassives.value(data, "healthRegen", 1.25); }
        public double staminaRegenMultiplier(StatsData data) { return ClassPassives.value(data, "staminaRegen", 1.10); }
        public double healingReceivedMultiplier(StatsData data) { return ClassPassives.value(data, "healingReceived", 1.10); }
    }

    private static final class TacticianPassive implements IClassPassive {
        private final Map<StatsData, Long> lastMelee = new WeakHashMap<>();
        private final Map<StatsData, Long> lastKi = new WeakHashMap<>();
        private final Map<StatsData, Long> currentTick = new WeakHashMap<>();
        public String classKey() { return "tactician"; }
        public void onPlayerTick(ServerPlayer player, StatsData data) { currentTick.put(data, player.serverLevel().getGameTime()); }
        public void onMeleeHit(ServerPlayer attacker, StatsData data, LivingEntity target, boolean blocked) {
            if (!blocked) lastMelee.put(data, attacker.serverLevel().getGameTime());
        }
        public double kiCooldownMultiplier(StatsData data, KiAttackData ki) {
            long now = currentTick.getOrDefault(data, 0L);
            long window = (long) ClassPassives.value(data, "windowTicks", 80.0);
            boolean combo = now - lastMelee.getOrDefault(data, Long.MIN_VALUE / 2) <= window;
            lastKi.put(data, now);
            return combo ? ClassPassives.value(data, "kiAfterMelee", 0.75) : 1.0;
        }
        public double strikeDamageMultiplier(StatsData data, LivingEntity target) {
            long now = currentTick.getOrDefault(data, 0L);
            long window = (long) ClassPassives.value(data, "windowTicks", 80.0);
            return now - lastKi.getOrDefault(data, Long.MIN_VALUE / 2) <= window
                    ? ClassPassives.value(data, "meleeAfterKi", 1.18) : 1.0;
        }
    }
}