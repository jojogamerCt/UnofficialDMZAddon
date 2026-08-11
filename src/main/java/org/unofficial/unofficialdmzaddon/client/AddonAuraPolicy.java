package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.common.stats.StatsData;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormManager;
import org.unofficial.unofficialdmzaddon.dmz.DivineAuraHelper;

/** Client-side policy shared by every passive addon aura effect. */
public final class AddonAuraPolicy {
    private AddonAuraPolicy() {}

    public static boolean isAddonAuraForm(StatsData data) {
        return isDivineAddonAuraForm(data)
                || CustomFormManager.hasActiveCustomForm(data);
    }

    private static boolean isDivineAddonAuraForm(StatsData data) {
        return DivineAuraHelper.isGodForm(data)
                || DivineAuraHelper.isUltraInstinct(data)
                || DivineAuraHelper.isUltraEgo(data);
    }

    public static boolean isNativeAuraRequested(StatsData data) {
        return data != null && data.getStatus() != null
                && (data.getStatus().isAuraActive() || data.getStatus().isPermanentAura());
    }

    public static boolean hasConstantAddonAura(StatsData data) {
        if (!GodAuraPreference.enabled()) return false;
        if (isDivineAddonAuraForm(data)) return true;
        return UnofficialDMZConfig.CUSTOM_FORMS_ALLOW_CONSTANT_AURA.get()
                && CustomFormManager.getActiveDefinition(data)
                .map(form -> form.constantAura())
                .orElse(false);
    }

    /** Custom persistent auras do not inherit the rubble reserved for divine transformations. */
    public static boolean shouldSpawnPassiveDivineParticles(StatsData data) {
        return isDivineAddonAuraForm(data) && GodAuraPreference.enabled();
    }

    public static boolean shouldShowAddonAura(StatsData data) {
        return isNativeAuraRequested(data) || hasConstantAddonAura(data);
    }

    /** Preserve DragonMineZ's normal passive lightning behavior outside addon aura forms. */
    public static boolean shouldQueueSparks(StatsData data) {
        return !isAddonAuraForm(data) || shouldShowAddonAura(data);
    }
}
