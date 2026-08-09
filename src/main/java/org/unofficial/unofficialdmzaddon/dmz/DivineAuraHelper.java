package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import org.unofficial.unofficialdmzaddon.dmz.UltraInstinctDefinitions;

public final class DivineAuraHelper {
    private DivineAuraHelper() {}

    public static boolean hasPersistentAura(StatsData data) {
        return hasPersistentGodAura(data) || isTrueUltraInstinct(data);
    }

    /** Only God Forms own DragonMineZ's Aura Status; UI and UE render their signatures independently. */
    public static boolean hasPersistentGodAura(StatsData data) {
        return org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.PERSISTENT_GOD_AURAS.get()
                && TransformationsHelper.hasGodFormActive(data);
    }

    public static boolean isGodForm(StatsData data) {
        return data != null && TransformationsHelper.hasGodFormActive(data);
    }

    public static boolean isUltraInstinct(StatsData data) {
        return data != null && data.getCharacter() != null
                && "ultrainstinct".equalsIgnoreCase(data.getCharacter().getActiveFormGroup());
    }

    public static boolean isTrueUltraInstinct(StatsData data) {
        return isUltraInstinct(data) && UltraInstinctDefinitions.FORM_TRUE.equalsIgnoreCase(
                data.getCharacter().getActiveForm());
    }

    public static boolean isUltraEgo(StatsData data) {
        return data != null && data.getCharacter() != null
                && "ultraego".equalsIgnoreCase(data.getCharacter().getActiveFormGroup());
    }
}
