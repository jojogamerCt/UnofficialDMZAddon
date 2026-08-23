package org.unofficial.unofficialdmzaddon;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common server-authoritative options for every addon gameplay system. */
public final class UnofficialDMZConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue FIRST_PERSON_RACE_MODEL;
    public static final ForgeConfigSpec.BooleanValue ULTRA_INSTINCT_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ULTRA_EGO_ENABLED;
    public static final ForgeConfigSpec.BooleanValue UI_DODGES_ENABLED;
    public static final ForgeConfigSpec.DoubleValue UI_SIGN_STAMINA_DRAIN;
    public static final ForgeConfigSpec.DoubleValue UI_MASTERED_STAMINA_DRAIN;
    public static final ForgeConfigSpec.DoubleValue UI_TRUE_STAMINA_DRAIN;
    public static final ForgeConfigSpec.DoubleValue ULTRA_EGO_DAMAGE_POWER_GAIN;
    public static final ForgeConfigSpec.DoubleValue ULTRA_EGO_DAMAGE_POWER_CAP;
    public static final ForgeConfigSpec.BooleanValue PERSISTENT_GOD_AURAS;
    public static final ForgeConfigSpec.BooleanValue SPECIAL_FORM_BUFFS;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_BEAST_FORM;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_RAGE_FORM;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_GOD_FORM;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_BLUE_FORM;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_ROSE_FORM;
    public static final ForgeConfigSpec.BooleanValue SAIYAN_ROSE_EVOLVED_FORM;
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
    public static final ForgeConfigSpec.BooleanValue SPACE_CELESTIAL_TRAVEL;
    public static final ForgeConfigSpec.BooleanValue SPACE_DEEP_SPACE_WALLS;
    public static final ForgeConfigSpec.BooleanValue SPACE_GALAXY_WALL_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SPACE_UNIVERSE_WALL_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SPACE_VERTICAL_BOUNDARIES;
    public static final ForgeConfigSpec.BooleanValue SPACE_VISIBILITY_RECOVERY;
    public static final ForgeConfigSpec.BooleanValue SPACE_PLANET_HUD;
    public static final ForgeConfigSpec.DoubleValue SPACE_FLOOR_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue SPACE_ROOF_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue SPACE_POD_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SPACE_PLANET_ENTRY_MARGIN;
    public static final ForgeConfigSpec.DoubleValue SPACE_SPAWN_CLEARANCE;
    public static final ForgeConfigSpec.DoubleValue SPACE_GALAXY_WALL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SPACE_UNIVERSE_WALL_RADIUS;
    public static final ForgeConfigSpec.BooleanValue SNAKE_MINIGAME_ENABLED;
    public static final ForgeConfigSpec.IntValue SNAKE_BASE_MOVE_TICKS;
    public static final ForgeConfigSpec.IntValue SNAKE_MIN_MOVE_TICKS;
    public static final ForgeConfigSpec.IntValue SNAKE_APPLES_PER_SPEEDUP;
    public static final ForgeConfigSpec.BooleanValue CUSTOM_FORMS_ENABLED;
    public static final ForgeConfigSpec.IntValue CUSTOM_FORMS_STARTING_SLOTS;
    public static final ForgeConfigSpec.BooleanValue CUSTOM_FORMS_SAGA_SLOT_UNLOCKS;
    public static final ForgeConfigSpec.IntValue CUSTOM_FORMS_SAGAS_PER_SLOT_MILESTONE;
    public static final ForgeConfigSpec.IntValue CUSTOM_FORMS_SLOTS_PER_SAGA_MILESTONE;
    public static final ForgeConfigSpec.IntValue CUSTOM_FORMS_MAX_PER_PLAYER;
    public static final ForgeConfigSpec.BooleanValue CUSTOM_FORMS_ALLOW_CONSTANT_AURA;
    public static final ForgeConfigSpec.BooleanValue CUSTOM_FORMS_ALLOW_AURA_OUTLINE;
    public static final ForgeConfigSpec.DoubleValue CUSTOM_FORMS_MAX_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CUSTOM_FORMS_MAX_ENERGY_DRAIN;
    public static final ForgeConfigSpec.BooleanValue CUSTOM_FORMS_TP_COSTS_ENABLED;
    public static final ForgeConfigSpec.IntValue CUSTOM_FORMS_BASE_TP_COST;
    public static final ForgeConfigSpec.IntValue CUSTOM_FORMS_LINEAR_POWER_TP_COST;
    public static final ForgeConfigSpec.IntValue CUSTOM_FORMS_QUADRATIC_POWER_TP_COST;
    public static final ForgeConfigSpec.IntValue CUSTOM_FORMS_MIN_TP_COST;
    public static final ForgeConfigSpec.IntValue CUSTOM_FORMS_MAX_TP_COST;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Client rendering options").push("rendering");
        FIRST_PERSON_RACE_MODEL = builder.define("first_person_race_model", true);
        SPACE_VISIBILITY_RECOVERY = builder.comment("Prevent non-potion invisibility while flying in space")
                .define("space_visibility_recovery", true);
        SPACE_PLANET_HUD = builder.comment("Show detailed information while aiming at a planet")
                .define("space_planet_info_hud", true);
        builder.pop();

        builder.comment("Transformation availability and behavior").push("transformations");
        ULTRA_INSTINCT_ENABLED = builder.define("ultra_instinct_enabled", true);
        ULTRA_EGO_ENABLED = builder.define("ultra_ego_enabled", true);
        UI_DODGES_ENABLED = builder.define("ultra_instinct_dodges", true);
        UI_SIGN_STAMINA_DRAIN = builder.comment("Stamina/exhaustion drain for Ultra Instinct Sign")
                .defineInRange("ultra_instinct_sign_stamina_drain", 0.018D, 0.0D, 0.5D);
        UI_MASTERED_STAMINA_DRAIN = builder.comment("Stamina/exhaustion drain for Mastered Ultra Instinct")
                .defineInRange("mastered_ultra_instinct_stamina_drain", 0.032D, 0.0D, 0.5D);
        UI_TRUE_STAMINA_DRAIN = builder.comment("Stamina/exhaustion drain for True Ultra Instinct")
                .defineInRange("true_ultra_instinct_stamina_drain", 0.045D, 0.0D, 0.5D);
        ULTRA_EGO_DAMAGE_POWER_GAIN = builder.comment("Stat bonus accumulated after taking damage equal to the user's maximum health")
                .defineInRange("ultra_ego_damage_power_gain", 0.075D, 0.0D, 2.0D);
        ULTRA_EGO_DAMAGE_POWER_CAP = builder.comment("Maximum accumulated Ultra Ego stat bonus; the default preserves Orange Form's higher tier")
                .defineInRange("ultra_ego_damage_power_cap", 0.075D, 0.0D, 2.0D);
        PERSISTENT_GOD_AURAS = builder.define("persistent_god_form_auras", true);
        SPECIAL_FORM_BUFFS = builder.define("special_form_buffs", true);
        SAIYAN_BEAST_FORM = builder.define("saiyan_beast", true);
        SAIYAN_RAGE_FORM = builder.define("saiyan_super_saiyan_rage", true);
        SAIYAN_GOD_FORM = builder.define("saiyan_super_saiyan_god", true);
        SAIYAN_BLUE_FORM = builder.define("saiyan_super_saiyan_blue", true);
        SAIYAN_ROSE_FORM = builder.define("saiyan_super_saiyan_rose", true);
        SAIYAN_ROSE_EVOLVED_FORM = builder.define("saiyan_super_saiyan_rose_evolved", true);
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
        SPACE_CELESTIAL_TRAVEL = builder.comment("Allow outward boundary travel and inward celestial gateways between the separate solar, galaxy and universe dimensions")
                .define("celestial_travel", true);
        SPACE_DEEP_SPACE_WALLS = builder.comment("Keep players and space pods inside navigable galaxy and universe space")
                .define("deep_space_invisible_walls", true);
        SPACE_GALAXY_WALL_ENABLED = builder.comment("Enable the invisible navigation wall in galaxy dimensions")
                .define("galaxy_invisible_wall_enabled", true);
        SPACE_UNIVERSE_WALL_ENABLED = builder.comment("Enable the invisible navigation wall in universe dimensions")
                .define("universe_invisible_wall_enabled", true);
        SPACE_VERTICAL_BOUNDARIES = builder.comment("Enable an invisible floor and roof in solar systems, galaxies and universes")
                .define("vertical_invisible_boundaries", true);
        SPACE_FLOOR_HEIGHT = builder.comment("Lowest reachable height in every space domain")
                .defineInRange("invisible_floor_height", 128.0D, -64.0D, 1023.0D);
        SPACE_ROOF_HEIGHT = builder.comment("Highest reachable height in every space domain")
                .defineInRange("invisible_roof_height", 224.0D, -63.0D, 1024.0D);
        SPACE_POD_SPEED_MULTIPLIER = builder.defineInRange("space_pod_speed_multiplier", 3.0D, 1.0D, 20.0D);
        SPACE_PLANET_ENTRY_MARGIN = builder.defineInRange("planet_entry_margin", 2.0D, 0.0D, 32.0D);
        SPACE_SPAWN_CLEARANCE = builder.defineInRange("planet_spawn_clearance", 5.5D, 2.1D, 64.0D);
        SPACE_GALAXY_WALL_RADIUS = builder.comment("Distance in blocks from a galaxy's navigation center")
                .defineInRange("galaxy_invisible_wall_distance", 200.0D, 96.0D, 8192.0D);
        SPACE_UNIVERSE_WALL_RADIUS = builder.comment("Distance in blocks from a universe's navigation center")
                .defineInRange("universe_invisible_wall_distance", 200.0D, 96.0D, 8192.0D);
        builder.pop();

        builder.comment("Addon minigames and TP training options").push("minigames");
        SNAKE_MINIGAME_ENABLED = builder.comment("Add Snake to DragonMineZ's minigames menu")
                .define("snake_enabled", true);
        SNAKE_BASE_MOVE_TICKS = builder.comment("Game ticks between moves when a Snake run starts")
                .defineInRange("snake_base_move_ticks", 8, 3, 20);
        SNAKE_MIN_MOVE_TICKS = builder.comment("Fastest movement interval Snake can reach")
                .defineInRange("snake_min_move_ticks", 2, 1, 10);
        SNAKE_APPLES_PER_SPEEDUP = builder.comment("Apples required for each speed increase")
                .defineInRange("snake_apples_per_speedup", 3, 1, 20);
        builder.pop();

        builder.comment("Player-created race form options").push("custom_forms");
        CUSTOM_FORMS_ENABLED = builder.define("enabled", true);
        CUSTOM_FORMS_STARTING_SLOTS = builder.comment("Slots available before completing any saga")
                .defineInRange("starting_form_slots", 1, 0, 64);
        CUSTOM_FORMS_SAGA_SLOT_UNLOCKS = builder.comment("Allow completed DragonMineZ sagas to unlock more slots")
                .define("saga_slot_unlocks_enabled", true);
        CUSTOM_FORMS_SAGAS_PER_SLOT_MILESTONE = builder.comment("Completed sagas required for each slot-unlock milestone")
                .defineInRange("completed_sagas_per_slot_milestone", 1, 1, 64);
        CUSTOM_FORMS_SLOTS_PER_SAGA_MILESTONE = builder.comment("Slots granted whenever a saga milestone is reached")
                .defineInRange("slots_per_saga_milestone", 1, 0, 64);
        CUSTOM_FORMS_MAX_PER_PLAYER = builder.comment("Absolute cap after starting and saga-earned slots are combined")
                .defineInRange("maximum_forms_per_player", 5, 1, 64);
        CUSTOM_FORMS_ALLOW_CONSTANT_AURA = builder.comment("Allow creators to make a custom form's aura remain active")
                .define("allow_constant_aura", true);
        CUSTOM_FORMS_ALLOW_AURA_OUTLINE = builder.comment("Allow creators to add the SSJ Rage-style light outline to custom forms")
                .define("allow_aura_outline", true);
        CUSTOM_FORMS_MAX_MULTIPLIER = builder.comment("Highest selectable custom-form power multiplier")
                .defineInRange("maximum_power_multiplier", 5.0D, 1.0D, 20.0D);
        CUSTOM_FORMS_MAX_ENERGY_DRAIN = builder.comment("Highest selectable custom-form Ki drain")
                .defineInRange("maximum_ki_drain", 0.5D, 0.0D, 2.0D);
        CUSTOM_FORMS_TP_COSTS_ENABLED = builder.comment("Charge TP when creating or upgrading custom forms")
                .define("tp_costs_enabled", true);
        CUSTOM_FORMS_BASE_TP_COST = builder.comment("Base value used by the power-cost formula")
                .defineInRange("base_tp_cost", 500, 0, 1_000_000);
        CUSTOM_FORMS_LINEAR_POWER_TP_COST = builder.comment("Linear TP cost per multiplier point above x1")
                .defineInRange("linear_power_tp_cost", 1_000, 0, 1_000_000);
        CUSTOM_FORMS_QUADRATIC_POWER_TP_COST = builder.comment("Quadratic TP cost per squared multiplier point above x1")
                .defineInRange("quadratic_power_tp_cost", 1_150, 0, 1_000_000);
        CUSTOM_FORMS_MIN_TP_COST = builder.comment("Minimum final TP price after Ki-drain balancing")
                .defineInRange("minimum_tp_cost", 1_500, 0, 1_000_000);
        CUSTOM_FORMS_MAX_TP_COST = builder.comment("Maximum final TP price")
                .defineInRange("maximum_tp_cost", 350_000, 0, 10_000_000);
        builder.pop();

        SPEC = builder.build();
    }

    private UnofficialDMZConfig() {}
}
