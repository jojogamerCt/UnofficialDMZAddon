package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsData;

/** One authoritative alignment rule shared by radial selection, charging, and active-form validation. */
public final class GodFormAlignment {
    private GodFormAlignment() {}

    public static boolean isAllowed(StatsData data, String group, String form) {
        if (data == null || data.getResources() == null || group == null || form == null) return true;
        if (!UniversalDivineAndSaiyanInstaller.GOD_FORMS_GROUP.equalsIgnoreCase(group)) return true;
        int alignment = data.getResources().getAlignment();
        if ("super_saiyan_rose".equalsIgnoreCase(form)
                || "super_saiyan_rose_evolved".equalsIgnoreCase(form)) return alignment <= 40;
        if ("super_saiyan_blue".equalsIgnoreCase(form)
                || "super_saiyan_blue_evolved".equalsIgnoreCase(form)) return alignment > 40;
        return true;
    }
}
