package org.unofficial.unofficialdmzaddon.dmz;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

public final class TransformationActivationHandler {

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

    private static String validateRequirements(DMZRuntimeAccess.TransformChargeState state,
                                               ServerPlayer player,
                                               String targetGroup,
                                               String targetForm) {
        if (UltraInstinctDefinitions.GROUP_NAME.equalsIgnoreCase(targetGroup)
                && UltraInstinctDefinitions.isUltraInstinctForm(targetForm)
                && UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()) {
            return validateUltraInstinctRequirements(state, player, targetForm);
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
        // Action bar feedback intentionally removed – requirements are visible in the UI.
    }
}
