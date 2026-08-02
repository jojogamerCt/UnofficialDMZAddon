package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;

public final class DivineAuraHelper {
    private DivineAuraHelper() {}

    public static boolean hasPersistentAura(StatsData data) {
        String group = data.getCharacter().getActiveFormGroup();
        return hasPersistentGodAura(data) || (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.PERSISTENT_ULTRA_EGO_AURA.get()
                && "ultraego".equalsIgnoreCase(group));
    }

    /** Only God Forms own DragonMineZ's Aura Status; UI and UE render their signatures independently. */
    public static boolean hasPersistentGodAura(StatsData data) {
        return org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.PERSISTENT_GOD_AURAS.get()
                && TransformationsHelper.hasGodFormActive(data);
    }
}
