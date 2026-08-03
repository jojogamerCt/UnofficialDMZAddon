package org.unofficial.unofficialdmzaddon.client;

import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

import java.util.regex.Pattern;

public final class HalalModeText {
    private static final Pattern TITLE_CASE = Pattern.compile("\\bGod\\b");
    private static final Pattern LOWER_CASE = Pattern.compile("\\bgod\\b");
    private static final Pattern UPPER_CASE = Pattern.compile("\\bGOD\\b");

    private HalalModeText() {
    }

    public static String apply(String value) {
        if (value == null || value.isEmpty() || !UnofficialDMZConfig.HALAL_MODE.get()) return value;
        String filtered = TITLE_CASE.matcher(value).replaceAll("Frog");
        filtered = LOWER_CASE.matcher(filtered).replaceAll("frog");
        return UPPER_CASE.matcher(filtered).replaceAll("FROG");
    }
}
