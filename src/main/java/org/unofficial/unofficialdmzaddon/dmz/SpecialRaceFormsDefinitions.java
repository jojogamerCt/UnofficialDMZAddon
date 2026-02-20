package org.unofficial.unofficialdmzaddon.dmz;

public final class SpecialRaceFormsDefinitions {

    public static final String SAIYAN_RACE = "saiyan";
    public static final String NAMEKIAN_RACE = "namekian";
    public static final String FROST_DEMON_RACE = "frostdemon";
    public static final String ALIEN_RACE = "alien";

    public static final String SAIYAN_GROUP_SUPERSAIYAN = "supersaiyan";
    public static final String NAMEKIAN_GROUP_SUPERFORMS = "superforms";
    public static final String FROST_DEMON_GROUP_EVOLUTION = "evolutionforms";
    public static final String ALIEN_GROUP_SUPERFORMS = "superforms";

    public static final String SAIYAN_FORM_BEAST = "beast";
    public static final String NAMEKIAN_FORM_ORANGE = "orange";
    public static final String FROST_DEMON_FORM_BLACK = "black";
    public static final String ALIEN_FORM_FULL_POWER = "fullpower";

    public static final String SAIYAN_REQUIRED_PREVIOUS_FORM = "supersaiyan3";
    public static final String NAMEKIAN_REQUIRED_PREVIOUS_FORM = "supernamekian";
    public static final String FROST_DEMON_REQUIRED_PREVIOUS_FORM = "fifth";

    public static final int SAIYAN_BEAST_UNLOCK_LEVEL = 8;
    public static final int NAMEKIAN_ORANGE_UNLOCK_LEVEL = 4;
    public static final int FROST_DEMON_BLACK_UNLOCK_LEVEL = 6;
    public static final int ALIEN_FULL_POWER_UNLOCK_LEVEL = 1;

    /** The highest superform level the Alien race can reach (boosting Full Power). */
    public static final int ALIEN_MAX_SUPERFORM_LEVEL = 4;

    /**
     * Per-level scaling factors applied to the Full Power form's base multipliers.
     * Index 0 = level 1 (1.0x, no bonus), index 3 = level 4 (max).
     */
    public static final double[] ALIEN_FULL_POWER_LEVEL_SCALING = {1.0, 1.15, 1.30, 1.50};

    public static final float SAIYAN_BEAST_REQUIRED_HEALTH_RATIO = 0.35f;
    public static final float FROST_DEMON_BLACK_REQUIRED_HEALTH_RATIO = 0.40f;
    public static final float NAMEKIAN_ORANGE_REQUIRED_ENERGY_RATIO = 0.70f;

    public static final double SAIYAN_BEAST_REQUIRED_MASTERY = 65.0;
    public static final double NAMEKIAN_ORANGE_REQUIRED_MASTERY = 50.0;
    public static final double FROST_DEMON_BLACK_REQUIRED_MASTERY = 60.0;

    private SpecialRaceFormsDefinitions() {
    }
}
