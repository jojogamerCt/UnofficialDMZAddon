package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.FormConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public record CustomFormDefinition(String id, String name, String race, String hairType,
                                   String hairColor, String eye1Color, String eye2Color,
                                   String auraColor, String bodyColor, String tailColor,
                                   double multiplier, double energyDrain) {
    private static final Pattern HEX = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9_]{1,32}");

    public static String group(UUID owner) {
        return "customforms_" + owner.toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    public static CustomFormDefinition validated(CustomFormDefinition input, String serverRace) {
        String id = input.id() != null && SAFE_ID.matcher(input.id()).matches()
                ? input.id() : "form_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String name = input.name() == null ? "Custom Form" : input.name().replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty()) name = "Custom Form";
        if (name.length() > 24) name = name.substring(0, 24);
        String hair = switch (input.hairType() == null ? "base" : input.hairType().toLowerCase(Locale.ROOT)) {
            case "ssj", "ssj2", "ssj3", "base" -> input.hairType().toLowerCase(Locale.ROOT);
            default -> "base";
        };
        String eye1 = color(input.eye1Color(), "#00FFFF");
        return new CustomFormDefinition(id, name, serverRace.toLowerCase(Locale.ROOT), hair,
                color(input.hairColor(), "#FFFFFF"), eye1, color(input.eye2Color(), eye1),
                color(input.auraColor(), "#FFFFFF"), color(input.bodyColor(), "#FFFFFF"),
                color(input.tailColor(), "#572117"),
                Mth.clamp(input.multiplier(), 1.0D, UnofficialDMZConfig.CUSTOM_FORMS_MAX_MULTIPLIER.get()),
                Mth.clamp(input.energyDrain(), 0.0D, UnofficialDMZConfig.CUSTOM_FORMS_MAX_ENERGY_DRAIN.get()));
    }

    private static String color(String value, String fallback) {
        return value != null && HEX.matcher(value).matches() ? value.toUpperCase(Locale.ROOT) : fallback;
    }

    /**
     * Power is expensive; accepting a meaningful drain lowers the price without ever making
     * high multipliers cheap. This same calculation is used for the UI estimate and server charge.
     */
    public int creationCost() {
        if (!UnofficialDMZConfig.CUSTOM_FORMS_TP_COSTS_ENABLED.get()) return 0;
        double power = Math.max(0.0D, multiplier - 1.0D);
        double rawPowerCost = UnofficialDMZConfig.CUSTOM_FORMS_BASE_TP_COST.get()
                + UnofficialDMZConfig.CUSTOM_FORMS_LINEAR_POWER_TP_COST.get() * power
                + UnofficialDMZConfig.CUSTOM_FORMS_QUADRATIC_POWER_TP_COST.get() * power * power;
        double efficiencyFactor = Mth.clamp(0.35D + 0.08D / (energyDrain + 0.025D), 0.5D, 3.5D);
        int maximum = UnofficialDMZConfig.CUSTOM_FORMS_MAX_TP_COST.get();
        int minimum = Math.min(UnofficialDMZConfig.CUSTOM_FORMS_MIN_TP_COST.get(), maximum);
        return Mth.clamp((int) Math.round(rawPowerCost * efficiencyFactor), minimum, maximum);
    }

    public FormConfig.FormData toFormData() {
        FormConfig.FormData form = new FormConfig.FormData();
        form.setName(id);
        form.setUnlockOnSkillLevel(0);
        form.setHairType(hairType);
        form.setHairColor(hairColor);
        form.setEye1Color(eye1Color);
        form.setEye2Color(eye2Color);
        form.setAuraColor(auraColor);
        form.setBodyColor1(bodyColor);
        // DragonMineZ renders transformed Saiyan tails from bodyColor2. Other races
        // do not expose a tail color and retain the selected body color in that slot.
        form.setBodyColor2("saiyan".equalsIgnoreCase(race) ? tailColor : bodyColor);
        form.setBodyColor3(bodyColor);
        form.setKeepBaseFormHeadBones(true);
        form.setFormStackable(false);
        form.setStrMultiplier(multiplier);
        form.setSkpMultiplier(multiplier);
        form.setDefMultiplier(Math.max(1.0D, multiplier * 0.82D));
        form.setPwrMultiplier(multiplier);
        form.setSpeedMultiplier(Math.min(2.0D, 1.0D + (multiplier - 1.0D) * 0.2D));
        form.setEnergyDrain(energyDrain);
        form.setMaxStatsMultiplier(multiplier);
        form.setMaxCostMultiplier(Math.max(0.2D, 1.0D / multiplier));
        form.setTransformationAnimation("transf.generic");
        return form;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id); tag.putString("Name", name); tag.putString("Race", race);
        tag.putString("HairType", hairType); tag.putString("HairColor", hairColor);
        tag.putString("Eye1Color", eye1Color); tag.putString("Eye2Color", eye2Color);
        tag.putString("AuraColor", auraColor); tag.putString("BodyColor", bodyColor); tag.putString("TailColor", tailColor);
        tag.putDouble("Multiplier", multiplier); tag.putDouble("EnergyDrain", energyDrain);
        return tag;
    }

    public static CustomFormDefinition load(CompoundTag tag) {
        String legacyEye = tag.contains("EyeColor") ? tag.getString("EyeColor") : "#00FFFF";
        String eye1 = tag.contains("Eye1Color") ? tag.getString("Eye1Color") : legacyEye;
        String eye2 = tag.contains("Eye2Color") ? tag.getString("Eye2Color") : legacyEye;
        String tail = tag.contains("TailColor") ? tag.getString("TailColor") :
                (tag.contains("BodyColor") ? tag.getString("BodyColor") : "#572117");
        return new CustomFormDefinition(tag.getString("Id"), tag.getString("Name"), tag.getString("Race"),
                tag.getString("HairType"), tag.getString("HairColor"), eye1, eye2,
                tag.getString("AuraColor"), tag.getString("BodyColor"), tail,
                tag.getDouble("Multiplier"), tag.getDouble("EnergyDrain"));
    }
}
