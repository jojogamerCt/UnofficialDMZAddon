package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Resolves the first native transformation for each race from DragonMineZ's live form configs. */
public final class SnakeUnlocks {
    private static final Set<String> UNIVERSAL_GROUPS = Set.of(
            "ultrainstinct", "ultraego", "godforms", "beastforms"
    );

    private SnakeUnlocks() {}

    public static boolean hasUnlockedFirstRaceForm(StatsData data) {
        if (data == null || data.getCharacter() == null) return false;
        String race = data.getCharacter().getRaceName();
        if (race == null || race.isBlank()) return false;

        Map<String, FormConfig> groups = ConfigManager.getAllFormsForRace(race);
        for (Map.Entry<String, FormConfig> entry : groups.entrySet()) {
            String group = entry.getKey().toLowerCase(Locale.ROOT);
            if (UNIVERSAL_GROUPS.contains(group)) continue;
            boolean hasEarnedForm = TransformationsHelper.getUnlockedForms(data, race, entry.getKey()).stream()
                    .anyMatch(form -> form.getUnlockOnSkillLevel() != null && form.getUnlockOnSkillLevel() > 0);
            if (hasEarnedForm) return true;
        }
        return false;
    }
}
