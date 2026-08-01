package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;

public final class DivineAuraHelper {
    private DivineAuraHelper() {}

    public static boolean hasPersistentAura(StatsData data) {
        String group = data.getCharacter().getActiveFormGroup();
        return TransformationsHelper.hasGodFormActive(data) || "ultrainstinct".equalsIgnoreCase(group)
                || "ultraego".equalsIgnoreCase(group);
    }
}
