package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.common.config.ConfigManager;

public final class GodAuraPreference {
    private GodAuraPreference() {}

    public static boolean enabled() {
        try {
            Object config = ConfigManager.getUserConfig();
            return !(config instanceof GodAuraConfigAccess access)
                    || access.unofficialdmzaddon$constantGodFormAuras();
        } catch (RuntimeException | LinkageError ignored) {
            return true;
        }
    }
}
