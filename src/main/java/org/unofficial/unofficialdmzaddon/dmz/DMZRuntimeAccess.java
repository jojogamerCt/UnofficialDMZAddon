package org.unofficial.unofficialdmzaddon.dmz;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class DMZRuntimeAccess {

    public static final String OMEN_FORM_GROUP = "supersaiyan";
    public static final String OMEN_FORM_NAME = "ultrainstinctomen";

    private static boolean initialized = false;
    private static boolean available = false;
    private static boolean warned = false;
    private static Capability<?> statsCapability;

    private DMZRuntimeAccess() {
    }

    public static boolean isAvailable() {
        initIfNeeded();
        return available;
    }

    public static Optional<OmenState> getOmenState(Player player) {
        initIfNeeded();
        if (!available || player == null) {
            return Optional.empty();
        }

        try {
            Object statsData = getStatsData(player);
            if (statsData == null) {
                return Optional.empty();
            }

            Object character = call(statsData, "getCharacter");
            if (character == null) {
                return Optional.empty();
            }

            boolean hasActiveForm = bool(call(character, "hasActiveForm"));
            if (!hasActiveForm) {
                return Optional.empty();
            }

            String activeGroup = string(call(character, "getActiveFormGroup"));
            String activeForm = string(call(character, "getActiveForm"));

            if (!OMEN_FORM_GROUP.equalsIgnoreCase(activeGroup) || !OMEN_FORM_NAME.equalsIgnoreCase(activeForm)) {
                return Optional.empty();
            }

            Object resources = call(statsData, "getResources");
            if (resources == null) {
                return Optional.empty();
            }

            int maxEnergy = number(call(statsData, "getMaxEnergy")).intValue();
            int currentEnergy = number(call(resources, "getCurrentEnergy")).intValue();
            double mastery = getMastery(character, activeGroup, activeForm);

            return Optional.of(new OmenState(resources, mastery, maxEnergy, currentEnergy));
        } catch (ReflectiveOperationException e) {
            warnOnce("Error while reading DMZ state: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static double getMastery(Object character, String activeGroup, String activeForm) throws ReflectiveOperationException {
        Object formMasteries = call(character, "getFormMasteries");
        if (formMasteries == null) {
            return 0.0;
        }

        Method masteryMethod = formMasteries.getClass().getMethod("getMastery", String.class, String.class);
        Object value = masteryMethod.invoke(formMasteries, activeGroup, activeForm);
        return number(value).doubleValue();
    }

    private static Object getStatsData(Player player) {
        @SuppressWarnings("unchecked")
        Capability<Object> capability = (Capability<Object>) statsCapability;
        LazyOptional<Object> opt = player.getCapability(capability);
        return opt.orElse(null);
    }

    private static void initIfNeeded() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            Class<?> statsCapabilityClass = Class.forName("com.dragonminez.common.stats.StatsCapability");
            Field capabilityField = statsCapabilityClass.getField("INSTANCE");
            Object capabilityValue = capabilityField.get(null);
            if (!(capabilityValue instanceof Capability<?> capability)) {
                warnOnce("DMZ capability field was found but had an unexpected type.");
                return;
            }

            statsCapability = capability;
            available = true;
            UnofficialDMZAddon.LOGGER.info("[Unofficial DMZ Addon] DMZ runtime bridge active.");
        } catch (ReflectiveOperationException e) {
            warnOnce("DragonMineZ was not detected. Ultra Instinct Omen add-on logic is disabled.");
        }
    }

    private static Object call(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean b && b;
    }

    private static Number number(Object value) {
        return value instanceof Number n ? n : 0;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void warnOnce(String message) {
        if (warned) {
            return;
        }
        warned = true;
        UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] {}", message);
    }

    public static final class OmenState {
        private final Object resources;
        private final double mastery;
        private final int maxEnergy;
        private int currentEnergy;

        private OmenState(Object resources, double mastery, int maxEnergy, int currentEnergy) {
            this.resources = resources;
            this.mastery = mastery;
            this.maxEnergy = maxEnergy;
            this.currentEnergy = currentEnergy;
        }

        public double mastery() {
            return mastery;
        }

        public int maxEnergy() {
            return maxEnergy;
        }

        public int currentEnergy() {
            return currentEnergy;
        }

        public boolean consumeEnergy(int amount) {
            if (amount <= 0) {
                return true;
            }
            if (currentEnergy < amount) {
                return false;
            }

            try {
                Method removeEnergy = resources.getClass().getMethod("removeEnergy", int.class);
                removeEnergy.invoke(resources, amount);
                currentEnergy -= amount;
                return true;
            } catch (ReflectiveOperationException e) {
                UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Failed to remove DMZ energy: {}", e.getMessage());
                return false;
            }
        }
    }
}
