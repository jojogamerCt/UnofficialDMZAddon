package org.unofficial.unofficialdmzaddon.dmz;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class UltraInstinctOmenActivationHandler {

    private static final int MESSAGE_COOLDOWN_TICKS = 30;

    private final Map<UUID, Integer> messageCooldown = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        DMZRuntimeAccess.getTransformChargeState(serverPlayer).ifPresent(state -> {
            if (!state.isFormActionCharging()) {
                return;
            }

            String targetForm = state.nextTargetFormName();
            if (targetForm == null || targetForm.isEmpty()) {
                return;
            }

            String targetGroup = state.targetFormGroup();
            String failureKey = validateRequirements(state, serverPlayer, targetGroup, targetForm);
            if (failureKey == null) {
                return;
            }

            state.resetActionCharge();
            state.stopActionCharging();
            sendMessage(serverPlayer, failureKey);
        });
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        messageCooldown.remove(event.getEntity().getUUID());
    }

    private static String validateRequirements(DMZRuntimeAccess.TransformChargeState state,
                                               ServerPlayer player,
                                               String targetGroup,
                                               String targetForm) {
        if (UltraInstinctDefinitions.GROUP_NAME.equalsIgnoreCase(targetGroup)
                && UltraInstinctDefinitions.isUltraInstinctForm(targetForm)) {
            return validateUltraInstinctRequirements(state, player, targetForm);
        }

        if (SpecialRaceFormsDefinitions.SAIYAN_RACE.equalsIgnoreCase(state.raceName())
                && SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN.equalsIgnoreCase(targetGroup)
                && SpecialRaceFormsDefinitions.SAIYAN_FORM_BEAST.equalsIgnoreCase(targetForm)) {
            return validateBeastRequirements(state, player);
        }

        if (SpecialRaceFormsDefinitions.NAMEKIAN_RACE.equalsIgnoreCase(state.raceName())
                && SpecialRaceFormsDefinitions.NAMEKIAN_GROUP_SUPERFORMS.equalsIgnoreCase(targetGroup)
                && SpecialRaceFormsDefinitions.NAMEKIAN_FORM_ORANGE.equalsIgnoreCase(targetForm)) {
            return validateOrangeRequirements(state);
        }

        if (SpecialRaceFormsDefinitions.FROST_DEMON_RACE.equalsIgnoreCase(state.raceName())
                && SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_EVOLUTION.equalsIgnoreCase(targetGroup)
                && SpecialRaceFormsDefinitions.FROST_DEMON_FORM_BLACK.equalsIgnoreCase(targetForm)) {
            return validateBlackRequirements(state, player);
        }

        return null;
    }

    private static String validateUltraInstinctRequirements(DMZRuntimeAccess.TransformChargeState state,
                                                            ServerPlayer player,
                                                            String targetForm) {
        float requiredHealthRatio = UltraInstinctDefinitions.requiredHealthRatioForTarget(targetForm);
        boolean hasRequiredHealth = player.getHealth() <= player.getMaxHealth() * requiredHealthRatio;

        if (UltraInstinctDefinitions.FORM_SIGN.equalsIgnoreCase(targetForm)) {
            return hasRequiredHealth
                    ? null
                    : "message.unofficialdmzaddon.ultra_instinct.sign.low_health_required";
        }

        String requiredPreviousForm = UltraInstinctDefinitions.requiredPreviousFormForTarget(targetForm);
        double requiredMastery = UltraInstinctDefinitions.requiredMasteryForTarget(targetForm);

        if (!state.hasActiveForm() || !state.isActiveFormInUltraInstinctGroup()) {
            return messageKeyForAdvancedStage(targetForm);
        }

        if (!requiredPreviousForm.equalsIgnoreCase(state.activeFormName())) {
            return messageKeyForAdvancedStage(targetForm);
        }

        double currentMastery = state.mastery(UltraInstinctDefinitions.GROUP_NAME, requiredPreviousForm);
        if (currentMastery < requiredMastery) {
            return messageKeyForAdvancedStage(targetForm);
        }

        return hasRequiredHealth ? null : messageKeyForAdvancedStage(targetForm);
    }

    private static String validateBeastRequirements(DMZRuntimeAccess.TransformChargeState state, ServerPlayer player) {
        if (!state.hasActiveForm()
                || !SpecialRaceFormsDefinitions.SAIYAN_GROUP_SUPERSAIYAN.equalsIgnoreCase(state.activeFormGroup())
                || !SpecialRaceFormsDefinitions.SAIYAN_REQUIRED_PREVIOUS_FORM.equalsIgnoreCase(state.activeFormName())) {
            return "message.unofficialdmzaddon.special_form.beast.requirements";
        }

        double currentMastery = state.mastery(state.activeFormGroup(), state.activeFormName());
        if (currentMastery < SpecialRaceFormsDefinitions.SAIYAN_BEAST_REQUIRED_MASTERY) {
            return "message.unofficialdmzaddon.special_form.beast.requirements";
        }

        boolean lowHealth = player.getHealth() <= player.getMaxHealth() * SpecialRaceFormsDefinitions.SAIYAN_BEAST_REQUIRED_HEALTH_RATIO;
        return lowHealth ? null : "message.unofficialdmzaddon.special_form.beast.requirements";
    }

    private static String validateOrangeRequirements(DMZRuntimeAccess.TransformChargeState state) {
        if (!state.hasActiveForm()
                || !SpecialRaceFormsDefinitions.NAMEKIAN_GROUP_SUPERFORMS.equalsIgnoreCase(state.activeFormGroup())
                || !SpecialRaceFormsDefinitions.NAMEKIAN_REQUIRED_PREVIOUS_FORM.equalsIgnoreCase(state.activeFormName())) {
            return "message.unofficialdmzaddon.special_form.orange.requirements";
        }

        double currentMastery = state.mastery(state.activeFormGroup(), state.activeFormName());
        if (currentMastery < SpecialRaceFormsDefinitions.NAMEKIAN_ORANGE_REQUIRED_MASTERY) {
            return "message.unofficialdmzaddon.special_form.orange.requirements";
        }

        if (state.maxEnergy() <= 0) {
            return "message.unofficialdmzaddon.special_form.orange.requirements";
        }

        double energyRatio = (double) state.currentEnergy() / (double) state.maxEnergy();
        return energyRatio >= SpecialRaceFormsDefinitions.NAMEKIAN_ORANGE_REQUIRED_ENERGY_RATIO
                ? null
                : "message.unofficialdmzaddon.special_form.orange.requirements";
    }

    private static String validateBlackRequirements(DMZRuntimeAccess.TransformChargeState state, ServerPlayer player) {
        if (!state.hasActiveForm()
                || !SpecialRaceFormsDefinitions.FROST_DEMON_GROUP_EVOLUTION.equalsIgnoreCase(state.activeFormGroup())
                || !SpecialRaceFormsDefinitions.FROST_DEMON_REQUIRED_PREVIOUS_FORM.equalsIgnoreCase(state.activeFormName())) {
            return "message.unofficialdmzaddon.special_form.black.requirements";
        }

        double currentMastery = state.mastery(state.activeFormGroup(), state.activeFormName());
        if (currentMastery < SpecialRaceFormsDefinitions.FROST_DEMON_BLACK_REQUIRED_MASTERY) {
            return "message.unofficialdmzaddon.special_form.black.requirements";
        }

        boolean lowHealth = player.getHealth() <= player.getMaxHealth() * SpecialRaceFormsDefinitions.FROST_DEMON_BLACK_REQUIRED_HEALTH_RATIO;
        return lowHealth ? null : "message.unofficialdmzaddon.special_form.black.requirements";
    }

    private static String messageKeyForAdvancedStage(String targetForm) {
        if (UltraInstinctDefinitions.FORM_MASTERED.equalsIgnoreCase(targetForm)) {
            return "message.unofficialdmzaddon.ultra_instinct.mastered.requirements";
        }
        if (UltraInstinctDefinitions.FORM_AUTONOMOUS.equalsIgnoreCase(targetForm)) {
            return "message.unofficialdmzaddon.ultra_instinct.autonomous.requirements";
        }
        if (UltraInstinctDefinitions.FORM_TRUE.equalsIgnoreCase(targetForm)) {
            return "message.unofficialdmzaddon.ultra_instinct.true.requirements";
        }
        return "message.unofficialdmzaddon.ultra_instinct.sign.low_health_required";
    }

    private void sendMessage(ServerPlayer player, String key) {
        int nextAllowedTick = messageCooldown.getOrDefault(player.getUUID(), 0);
        if (player.tickCount < nextAllowedTick) {
            return;
        }

        player.displayClientMessage(Component.translatable(key), true);
        messageCooldown.put(player.getUUID(), player.tickCount + MESSAGE_COOLDOWN_TICKS);
    }
}
