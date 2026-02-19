package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class DMZRuntimeAccess {

    public static final String OMEN_FORM_GROUP = UltraInstinctDefinitions.GROUP_NAME;
    public static final String OMEN_FORM_NAME = UltraInstinctDefinitions.FORM_SIGN;

    private DMZRuntimeAccess() {
    }

    public static Optional<UltraInstinctState> getUltraInstinctState(Player player) {
        return getStatsData(player).flatMap(data -> {
            var character = data.getCharacter();
            if (!character.hasActiveForm()) {
                return Optional.empty();
            }

            String activeGroup = character.getActiveFormGroup();
            String activeForm = character.getActiveForm();
            if (!OMEN_FORM_GROUP.equalsIgnoreCase(activeGroup)) {
                return Optional.empty();
            }
            if (!UltraInstinctDefinitions.isUltraInstinctForm(activeForm)) {
                return Optional.empty();
            }

            double mastery = character.getFormMasteries().getMastery(activeGroup, activeForm);
            int tier = UltraInstinctDefinitions.tierForForm(activeForm);
            return Optional.of(new UltraInstinctState(data, activeForm, tier, mastery));
        });
    }

    public static Optional<UltraInstinctState> getOmenState(Player player) {
        return getUltraInstinctState(player);
    }

    public static Optional<TransformChargeState> getTransformChargeState(Player player) {
        return getStatsData(player).map(TransformChargeState::new);
    }

    private static Optional<StatsData> getStatsData(Player player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .filter(data -> data.getStatus().hasCreatedCharacter());
    }

    public static final class UltraInstinctState {
        private final StatsData data;
        private final String activeForm;
        private final int tier;
        private final double mastery;

        private UltraInstinctState(StatsData data, String activeForm, int tier, double mastery) {
            this.data = data;
            this.activeForm = activeForm;
            this.tier = tier;
            this.mastery = mastery;
        }

        public String activeForm() {
            return activeForm;
        }

        public int tier() {
            return tier;
        }

        public double mastery() {
            return mastery;
        }

        public int maxEnergy() {
            return data.getMaxEnergy();
        }

        public int currentEnergy() {
            return data.getResources().getCurrentEnergy();
        }

        public boolean consumeEnergy(int amount) {
            if (amount <= 0) {
                return true;
            }
            if (currentEnergy() < amount) {
                return false;
            }
            data.getResources().removeEnergy(amount);
            return true;
        }
    }

    public static final class TransformChargeState {
        private final StatsData data;

        private TransformChargeState(StatsData data) {
            this.data = data;
        }

        public boolean hasActiveForm() {
            return data.getCharacter().hasActiveForm();
        }

        public String activeFormGroup() {
            return data.getCharacter().getActiveFormGroup();
        }

        public String activeFormName() {
            return data.getCharacter().getActiveForm();
        }

        public String selectedFormGroup() {
            return data.getCharacter().getSelectedFormGroup();
        }

        public boolean isUltraInstinctGroupSelected() {
            return OMEN_FORM_GROUP.equalsIgnoreCase(selectedFormGroup());
        }

        public boolean isFormActionCharging() {
            return data.getStatus().isActionCharging() && data.getStatus().getSelectedAction() == ActionMode.FORM;
        }

        public String raceName() {
            return data.getCharacter().getRaceName();
        }

        public boolean isActiveFormInUltraInstinctGroup() {
            return hasActiveForm() && OMEN_FORM_GROUP.equalsIgnoreCase(activeFormGroup());
        }

        public boolean hasActiveUltraInstinctForm() {
            return isActiveFormInUltraInstinctGroup() && UltraInstinctDefinitions.isUltraInstinctForm(activeFormName());
        }

        public String nextUltraInstinctTargetForm() {
            if (hasActiveForm()) {
                if (!isActiveFormInUltraInstinctGroup()) {
                    return null;
                }
                return UltraInstinctDefinitions.nextForm(activeFormName());
            }
            if (!isUltraInstinctGroupSelected()) {
                return null;
            }
            return UltraInstinctDefinitions.FORM_SIGN;
        }

        public String nextTargetFormName() {
            var nextForm = TransformationsHelper.getNextAvailableForm(data);
            return nextForm != null ? nextForm.getName() : null;
        }

        public String targetFormGroup() {
            return hasActiveForm() ? activeFormGroup() : selectedFormGroup();
        }

        public int currentEnergy() {
            return data.getResources().getCurrentEnergy();
        }

        public int maxEnergy() {
            return data.getMaxEnergy();
        }

        public double mastery(String groupName, String formName) {
            return data.getCharacter().getFormMasteries().getMastery(groupName, formName);
        }

        public boolean hasActiveOmen() {
            return hasActiveUltraInstinctForm()
                    && OMEN_FORM_NAME.equalsIgnoreCase(activeFormName());
        }

        public void resetActionCharge() {
            data.getResources().setActionCharge(0);
        }

        public void stopActionCharging() {
            data.getStatus().setActionCharging(false);
        }
    }
}
