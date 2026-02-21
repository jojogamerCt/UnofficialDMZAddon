package org.unofficial.unofficialdmzaddon;

import net.minecraftforge.common.ForgeConfigSpec;

public final class UnofficialDMZConfig {

    public static final ForgeConfigSpec SPEC;

    // ── Ultra Instinct (Saiyan) ───────────────────────────────────────────────
    public static final ForgeConfigSpec.BooleanValue ULTRA_INSTINCT_ENABLED;

    // ── Saiyan extra forms ────────────────────────────────────────────────────
    public static final ForgeConfigSpec.BooleanValue SAIYAN_BEAST_FORM;

    // ── Namekian extra forms ──────────────────────────────────────────────────
    public static final ForgeConfigSpec.BooleanValue NAMEKIAN_ORANGE_FORM;

    // ── Frost Demon extra forms ───────────────────────────────────────────────
    public static final ForgeConfigSpec.BooleanValue FROST_DEMON_GOLDEN_FORM;
    public static final ForgeConfigSpec.BooleanValue FROST_DEMON_BLACK_FORM;

    // ── Alien extra forms ─────────────────────────────────────────────────────
    public static final ForgeConfigSpec.BooleanValue ALIEN_FULL_POWER_FORM;

    // ── Rendering ─────────────────────────────────────────────────────────────
    public static final ForgeConfigSpec.BooleanValue FIRST_PERSON_RACE_MODEL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Unofficial DMZ Addon — rendering options").push("rendering");
        FIRST_PERSON_RACE_MODEL = builder
                .comment("Show your race model and active transformation in first person.",
                         "The full body (with race skin + form visuals) appears in front of you,",
                         "the head bone is hidden so you don't clip into it, and the model is",
                         "translated so its head aligns with your camera. Replaces vanilla hand",
                         "rendering while in first-person camera mode.")
                .define("first_person_race_model", true);
        builder.pop(); // rendering

        builder.comment("Unofficial DMZ Addon — toggle individual transformations and form groups")
               .push("transformations");

        builder.comment("Ultra Instinct progression (Saiyan race)").push("ultra_instinct");
        ULTRA_INSTINCT_ENABLED = builder
                .comment("Enable the full Ultra Instinct form chain (Sign -> Mastered -> Autonomous -> True)")
                .define("enabled", true);
        builder.pop();

        builder.comment("Saiyan extra forms").push("saiyan");
        SAIYAN_BEAST_FORM = builder
                .comment("Enable Beast Form for Saiyans")
                .define("beast_form", true);
        builder.pop();

        builder.comment("Namekian extra forms").push("namekian");
        NAMEKIAN_ORANGE_FORM = builder
                .comment("Enable Orange Form for Namekians")
                .define("orange_form", true);
        builder.pop();

        builder.comment("Frost Demon extra forms (Super Forms 2 section)").push("frost_demon");
        FROST_DEMON_GOLDEN_FORM = builder
                .comment("Enable Golden Form for Frost Demons")
                .define("golden_form", true);
        FROST_DEMON_BLACK_FORM = builder
                .comment("Enable Black Form for Frost Demons")
                .define("black_form", true);
        builder.pop();

        builder.comment("Alien extra forms").push("alien");
        ALIEN_FULL_POWER_FORM = builder
                .comment("Enable Full Power Form for Aliens")
                .define("full_power_form", true);
        builder.pop();

        builder.pop(); // transformations

        SPEC = builder.build();
    }

    private UnofficialDMZConfig() {}
}
