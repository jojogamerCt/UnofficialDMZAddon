package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;

public final class DivineAuraHelper {
    private DivineAuraHelper() {}

    public static boolean hasPersistentAura(StatsData data) {
        String group = data.getCharacter().getActiveFormGroup();
        return hasPersistentGodAura(data) || "ultrainstinct".equalsIgnoreCase(group)
                || "ultraego".equalsIgnoreCase(group);
    }

    /** Only God Forms own DragonMineZ's Aura Status; UI and UE render their signatures independently. */
    public static boolean hasPersistentGodAura(StatsData data) {
        return TransformationsHelper.hasGodFormActive(data);
    }
}
