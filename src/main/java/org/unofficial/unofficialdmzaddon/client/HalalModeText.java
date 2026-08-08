package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.common.config.ConfigManager;
import java.util.regex.Pattern;

public final class HalalModeText {
    private static final Pattern UPPER_GOD = Pattern.compile("(?<![A-Za-z])God(?![A-Za-z])");
    private static final Pattern LOWER_GOD = Pattern.compile("(?<![A-Za-z])god(?![A-Za-z])");

    private HalalModeText() {}

    public static boolean enabled() {
        try {
            Object config = ConfigManager.getUserConfig();
            return config instanceof HalalModeConfigAccess access && access.unofficialdmzaddon$isHalalMode();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static String apply(String text) {
        if (!enabled() || text == null || text.isEmpty()) return text;
        return LOWER_GOD.matcher(UPPER_GOD.matcher(text).replaceAll("Frog")).replaceAll("frog");
    }
}
