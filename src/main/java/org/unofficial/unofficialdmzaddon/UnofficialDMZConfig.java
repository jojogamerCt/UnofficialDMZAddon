package org.unofficial.unofficialdmzaddon;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common server-authoritative options for every addon gameplay system. */
public final class UnofficialDMZConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue FIRST_PERSON_RACE_MODEL;
    public static final ForgeConfigSpec.BooleanValue ULTRA_INSTINCT_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ULTRA_EGO_ENABLED;
    public static final ForgeConfigSpec.BooleanValue UI_DODGES_ENABLED;
    public static final ForgeConfigSpec.BooleanValue PERSISTENT_GOD_AURAS;
    public static final ForgeConfigSpec.BooleanValue PERSISTENT_ULTRA_EGO_AURA;
    public static final ForgeConfigSpec.BooleanValue SPECIAL_FORM_BUFFS;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_BEAST_FORM;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_RAGE_FORM;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_GOD_FORM;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_BLUE_FORM;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_ROSE_FORM;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_BLUE_EVOLVED_FORM;
    public static final ForgeConfigSpec.BooleanValue NAMEKIAN_ORANGE_FORM;
    public static final ForgeConfigSpec.BooleanValue FROST_DEMON_GOLDEN_FORM;
    public static final ForgeConfigSpec.BooleanValue FROST_DEMON_BLACK_FORM;
    public static final ForgeConfigSpec.BooleanValue ALIEN_FULL_POWER_FORM;
    public static final ForgeConfigSpec.BooleanValue ALIEN_RACIAL_PASSIVE;
    public static final ForgeConfigSpec.BooleanValue SPACE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SPACE_REQUIRE_FLY;
    public static final ForgeConfigSpec.BooleanValue SPACE_PLANET_TRAVEL;
    public static final ForgeConfigSpec.BooleanValue SPACE_PLANET_DESTRUCTION;
    public static final ForgeConfigSpec.BooleanValue SPACE_VISIBILITY_RECOVERY;
    public static final ForgeConfigSpec.BooleanValue SPACE_PLANET_HUD;
    public static final ForgeConfigSpec.BooleanValue SPACE_RENDER_COMPATIBILITY;
    public static final ForgeConfigSpec.DoubleValue SPACE_FLOOR_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue SPACE_POD_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SPACE_PLANET_ENTRY_MARGIN;
    public static final ForgeConfigSpec.DoubleValue SPACE_SPAWN_CLEARANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Client rendering options").push("rendering");
        FIRST_PERSON_RACE_MODEL = builder.define("first_person_race_model", true);
        SPACE_VISIBILITY_RECOVERY = builder.comment("Prevent non-potion invisibility while flying in space")
                .define("space_visibility_recovery", true);
        SPACE_PLANET_HUD = builder.comment("Show detailed information while aiming at a planet")
                .define("space_planet_info_hud", true);
        SPACE_RENDER_COMPATIBILITY = builder.comment("Use a late depth-safe celestial pass with shader renderers")
                .define("space_shader_compatibility", true);
        builder.pop();

        builder.comment("Transformation availability and behavior").push("transformations");
        ULTRA_INSTINCT_ENABLED = builder.define("ultra_instinct_enabled", true);
        ULTRA_EGO_ENABLED = builder.define("ultra_ego_enabled", true);
        UI_DODGES_ENABLED = builder.define("ultra_instinct_dodges", true);
        PERSISTENT_GOD_AURAS = builder.define("persistent_god_form_auras", true);
        PERSISTENT_ULTRA_EGO_AURA = builder.define("persistent_ultra_ego_aura", true);
        SPECIAL_FORM_BUFFS = builder.define("special_form_buffs", true);
        SAIYAN_BEAST_FORM = builder.define("saiyan_beast", true);
        SAIYAN_RAGE_FORM = builder.define("saiyan_super_saiyan_rage", true);
        SAIYAN_GOD_FORM = builder.define("saiyan_super_saiyan_god", true);
        SAIYAN_BLUE_FORM = builder.define("saiyan_super_saiyan_blue", true);
        SAIYAN_ROSE_FORM = builder.define("saiyan_super_saiyan_rose", true);
        SAIYAN_BLUE_EVOLVED_FORM = builder.define("saiyan_super_saiyan_blue_evolved", true);
        NAMEKIAN_ORANGE_FORM = builder.define("namekian_orange", true);
        FROST_DEMON_GOLDEN_FORM = builder.define("frost_demon_golden", true);
        FROST_DEMON_BLACK_FORM = builder.define("frost_demon_black", true);
        ALIEN_FULL_POWER_FORM = builder.define("alien_full_power", true);
        ALIEN_RACIAL_PASSIVE = builder.define("alien_racial_passive", true);
        builder.pop();

        builder.comment("Space dimension, travel, and vehicle options").push("space");
        SPACE_ENABLED = builder.define("enabled", true);
        SPACE_REQUIRE_FLY = builder.define("require_fly_skill", true);
        SPACE_PLANET_TRAVEL = builder.define("planet_travel", true);
        SPACE_PLANET_DESTRUCTION = builder.define("planet_destruction", true);
        SPACE_FLOOR_HEIGHT = builder.defineInRange("invisible_floor_height", 128.0D, -64.0D, 1024.0D);
        SPACE_POD_SPEED_MULTIPLIER = builder.defineInRange("space_pod_speed_multiplier", 3.0D, 1.0D, 20.0D);
        SPACE_PLANET_ENTRY_MARGIN = builder.defineInRange("planet_entry_margin", 2.0D, 0.0D, 32.0D);
        SPACE_SPAWN_CLEARANCE = builder.defineInRange("planet_spawn_clearance", 5.5D, 2.1D, 64.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private UnofficialDMZConfig() {}
}
