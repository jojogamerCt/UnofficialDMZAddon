package org.unofficial.unofficialdmzaddon.dmz;

public final class SpecialRaceFormsDefinitions {

    public static final String SAIYAN_RACE = "saiyan";
    public static final String NAMEKIAN_RACE = "namekian";
    public static final String FROST_DEMON_RACE = "frostdemon";
    public static final String ALIEN_RACE = "alien";

    public static final String SAIYAN_GROUP_SUPERSAIYAN = "supersaiyan";
    public static final String NAMEKIAN_GROUP_SUPERFORMS = "superforms";
    public static final String FROST_DEMON_GROUP_EVOLUTION = "evolutionforms";
    public static final String FROST_DEMON_GROUP_SUPERFORMS2 = "superforms2";
    public static final String ALIEN_GROUP_SUPERFORMS = "superforms";

    public static final String SAIYAN_FORM_BEAST = "beast";
    public static final String NAMEKIAN_FORM_ORANGE = "orange";
    public static final String FROST_DEMON_FORM_GOLDEN = "golden";
    public static final String FROST_DEMON_FORM_BLACK = "black";
    public static final String ALIEN_FORM_FULL_POWER = "fullpower";

    public static final int SAIYAN_BEAST_UNLOCK_LEVEL = 8;
    public static final int NAMEKIAN_ORANGE_UNLOCK_LEVEL = 4;
    public static final int FROST_DEMON_GOLDEN_UNLOCK_LEVEL = 6;
    public static final int FROST_DEMON_BLACK_UNLOCK_LEVEL = 7;
    public static final int ALIEN_FULL_POWER_UNLOCK_LEVEL = 1;

    /** The highest superform level the Alien race can reach (boosting Full Power). */
    public static final int ALIEN_MAX_SUPERFORM_LEVEL = 4;

    /**
     * Per-level scaling factors applied to the Full Power form's base multipliers.
     * Index 0 = level 1 (1.0x, no bonus), index 3 = level 4 (max).
     */
    public static final double[] ALIEN_FULL_POWER_LEVEL_SCALING = {1.0, 1.15, 1.30, 1.50};

    private SpecialRaceFormsDefinitions() {
    }
}
