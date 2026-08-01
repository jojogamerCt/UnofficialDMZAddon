package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class AddonFormCommand {
    private AddonFormCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dmzform")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("grant")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("form", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(allFormNames(), builder))
                                        .executes(context -> grant(context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "form")))))));
    }

    private static int grant(net.minecraft.commands.CommandSourceStack source,
                             Collection<ServerPlayer> targets, String requestedForm) {
        int changed = 0;
        for (ServerPlayer target : targets) {
            boolean granted = StatsProvider.get(StatsCapability.INSTANCE, target).map(data -> {
                String race = data.getCharacter().getRaceName();
                FormMatch match = findForm(race, requestedForm);
                if (match == null) return false;
                String skill = TransformationsHelper.getSkillNameForType(match.config().getFormType());
                int required = Math.max(1, match.form().getUnlockOnSkillLevel());
                if (data.getSkills().getSkillLevel(skill) < required) data.getSkills().setSkillLevel(skill, required);
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(target), target);
                source.sendSuccess(() -> Component.literal("Granted " + match.form().getName() + " to "
                        + target.getName().getString() + " via " + skill + " level " + required + "."), true);
                return true;
            }).orElse(false);
            if (granted) changed++;
            else source.sendFailure(Component.literal("Form '" + requestedForm + "' is not available for "
                    + target.getName().getString() + "'s race."));
        }
        return changed;
    }

    private static FormMatch findForm(String race, String requested) {
        var groups = ConfigManager.getAllFormsForRace(race);
        if (groups == null) return null;
        String normalized = canonicalAlias(normalize(requested));
        for (FormConfig config : groups.values()) {
            if (config == null) continue;
            for (FormConfig.FormData form : config.getForms().values()) {
                if (form != null && normalize(form.getName()).equals(normalized)) return new FormMatch(config, form);
            }
        }
        return null;
    }

    private static Set<String> allFormNames() {
        Set<String> result = new LinkedHashSet<>();
        for (String race : ConfigManager.getLoadedRaces()) {
            var groups = ConfigManager.getAllFormsForRace(race);
            if (groups == null) continue;
            for (FormConfig group : groups.values()) {
                if (group == null) continue;
                for (FormConfig.FormData form : group.getForms().values()) {
                    if (form != null && form.getName() != null && !form.getName().isBlank()) result.add(form.getName());
                }
            }
        }
        return result;
    }


    private static String canonicalAlias(String value) {
        return switch (value) {
            case "god", "ssjg", "super_saiyan_god" -> "super_saiyan_god";
            case "blue", "ssjb", "super_saiyan_blue" -> "super_saiyan_blue";
            case "rose", "rosé", "ssjr", "super_saiyan_rose" -> "super_saiyan_rose";
            case "blue_evolved", "ssjbe", "ssj_blue_evolved", "super_saiyan_blue_evolved" -> "super_saiyan_blue_evolved";
            case "rage", "ssjrage", "super_saiyan_rage" -> "super_saiyan_rage";
            default -> value;
        };
    }
    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private record FormMatch(FormConfig config, FormConfig.FormData form) {}
}