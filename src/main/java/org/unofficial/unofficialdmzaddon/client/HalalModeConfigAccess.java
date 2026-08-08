package org.unofficial.unofficialdmzaddon.client;

/** Added to DragonMineZ's user config so its normal save/load path persists Halal Mode. */
public interface HalalModeConfigAccess {
    boolean unofficialdmzaddon$isHalalMode();
    void unofficialdmzaddon$setHalalMode(boolean enabled);
}
