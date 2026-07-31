package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.StackForms;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class DMZRuntimeAccess {

    public static final String OMEN_FORM_GROUP = StackForms.GROUP_ULTRAINSTINCT;
    public static final String OMEN_FORM_NAME = StackForms.ULTRAINSTINCT_SIGN;

    private DMZRuntimeAccess() {
    }

    public static Optional<UltraInstinctState> getUltraInstinctState(Player player) {
        return getStatsData(player).flatMap(data -> {
            var character = data.getCharacter();
            boolean independent = character.hasActiveForm()
                    && OMEN_FORM_GROUP.equalsIgnoreCase(character.getActiveFormGroup());
            boolean legacyStack = character.hasActiveStackForm()
                    && OMEN_FORM_GROUP.equalsIgnoreCase(character.getActiveStackFormGroup());
            if (!independent && !legacyStack) return Optional.empty();

            String activeGroup = independent ? character.getActiveFormGroup() : character.getActiveStackFormGroup();
            String activeForm = independent ? character.getActiveForm() : character.getActiveStackForm();
            if (!OMEN_FORM_GROUP.equalsIgnoreCase(activeGroup)) {
                return Optional.empty();
            }
            if (!UltraInstinctDefinitions.isUltraInstinctForm(activeForm)) {
                return Optional.empty();
            }

            double mastery = independent
                    ? character.getFormMasteries().getMastery(activeGroup, activeForm)
                    : character.getStackFormMasteries().getMastery(activeGroup, activeForm);
            int tier = UltraInstinctDefinitions.tierForForm(activeForm);
            return Optional.of(new UltraInstinctState(data, activeForm, tier, mastery));
        });
    }

    public static Optional<UltraInstinctState> getOmenState(Player player) {
        return getUltraInstinctState(player);
    }
    private static Optional<StatsData> getStatsData(Player player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .filter(data -> data.getStatus().isHasCreatedCharacter());
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

        public float maxEnergy() {
            return data.getMaxEnergy();
        }

        public float currentEnergy() {
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


}
