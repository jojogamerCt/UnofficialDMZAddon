package org.unofficial.unofficialdmzaddon.dmz;

import java.util.List;
import java.util.Locale;

public final class UltraInstinctDefinitions {

    public static final String SKILL_ID = "ultrainstinct";
    public static final String FORM_TYPE = "ultrainstinct";
    public static final String GROUP_NAME = "ultrainstinct";

    public static final String FORM_SIGN = "ultrainstinctsign";
    public static final String FORM_MASTERED = "masteredultrainstinct";
    public static final String FORM_AUTONOMOUS = "ultrainstinctautonomous";
    public static final String FORM_TRUE = "trueultrainstinct";
    public static final String LEGACY_FORM_OMEN = "ultrainstinctomen";

    public static final int[] SKILL_TP_COSTS = new int[]{120000, 180000, 260000, 360000};
    public static final List<String> FORM_ORDER = List.of(
            FORM_SIGN,
            FORM_MASTERED,
            FORM_AUTONOMOUS,
            FORM_TRUE
    );

    private UltraInstinctDefinitions() {
    }

    public static int maxSkillLevel() {
        return SKILL_TP_COSTS.length;
    }

    public static boolean isUltraInstinctForm(String formName) {
        if (formName == null || formName.isEmpty()) {
            return false;
        }
        String normalized = formName.toLowerCase(Locale.ROOT);
        return FORM_ORDER.contains(normalized) || LEGACY_FORM_OMEN.equals(normalized);
    }

    public static int tierForForm(String formName) {
        if (formName == null || formName.isEmpty()) {
            return 0;
        }
        String normalized = formName.toLowerCase(Locale.ROOT);
        int index = FORM_ORDER.indexOf(normalized);
        if (index >= 0) {
            return index + 1;
        }
        if (LEGACY_FORM_OMEN.equals(normalized)) {
            return 1;
        }
        return 0;
    }

    public static int requiredSkillLevel(String formName) {
        int tier = tierForForm(formName);
        return Math.max(1, tier);
    }

    public static String nextForm(String activeForm) {
        if (activeForm == null || activeForm.isEmpty()) {
            return FORM_SIGN;
        }

        String normalized = activeForm.toLowerCase(Locale.ROOT);
        if (LEGACY_FORM_OMEN.equals(normalized)) {
            return FORM_MASTERED;
        }

        int index = FORM_ORDER.indexOf(normalized);
        if (index < 0 || index + 1 >= FORM_ORDER.size()) {
            return null;
        }
        return FORM_ORDER.get(index + 1);
    }

    public static float requiredHealthRatioForTarget(String targetForm) {
        return switch (normalize(targetForm)) {
            case FORM_SIGN -> 0.50f;
            case FORM_MASTERED -> 0.43f;
            case FORM_AUTONOMOUS -> 0.36f;
            case FORM_TRUE -> 0.30f;
            default -> 1.0f;
        };
    }

    public static double requiredMasteryForTarget(String targetForm) {
        return switch (normalize(targetForm)) {
            case FORM_MASTERED -> 35.0;
            case FORM_AUTONOMOUS -> 55.0;
            case FORM_TRUE -> 75.0;
            default -> 0.0;
        };
    }

    public static String requiredPreviousFormForTarget(String targetForm) {
        return switch (normalize(targetForm)) {
            case FORM_MASTERED -> FORM_SIGN;
            case FORM_AUTONOMOUS -> FORM_MASTERED;
            case FORM_TRUE -> FORM_AUTONOMOUS;
            default -> "";
        };
    }

    private static String normalize(String formName) {
        return formName == null ? "" : formName.toLowerCase(Locale.ROOT);
    }
}
