package org.unofficial.unofficialdmzaddon.client;

import org.unofficial.unofficialdmzaddon.dmz.CustomFormDefinition;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.unofficial.unofficialdmzaddon.network.CustomFormSaveResultS2C;

public final class CustomFormsClientState {
    private static final Map<UUID, List<CustomFormDefinition>> FORMS = new LinkedHashMap<>();
    private static CustomFormSaveResultS2C saveResult;
    private CustomFormsClientState() {}

    public static void accept(UUID newOwner, List<CustomFormDefinition> newForms) {
        List<CustomFormDefinition> copy = List.copyOf(newForms == null ? List.of() : newForms);
        FORMS.put(newOwner, copy);
        CustomFormManager.install(newOwner, copy);
    }

    /** Reattaches custom groups after DragonMineZ replaces its client-side synced form registry. */
    public static void reinstallAll() {
        FORMS.forEach(CustomFormManager::install);
    }

    public static List<CustomFormDefinition> forRace(String race) {
        if (race == null) return List.of();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return List.of();
        List<CustomFormDefinition> forms = FORMS.getOrDefault(minecraft.player.getUUID(), List.of());
        List<CustomFormDefinition> result = new ArrayList<>();
        for (CustomFormDefinition form : forms) if (form.race().equalsIgnoreCase(race)) result.add(form);
        return result;
    }

    public static String translatedName(String key) {
        if (key == null) return null;
        String id = key.substring(key.lastIndexOf('.') + 1);
        for (Map.Entry<UUID, List<CustomFormDefinition>> entry : FORMS.entrySet()) {
            if (!key.contains(".form." + CustomFormDefinition.group(entry.getKey()) + ".")) continue;
            return entry.getValue().stream().filter(form -> form.id().equals(id)).map(CustomFormDefinition::name).findFirst().orElse(null);
        }
        return null;
    }

    public static void acceptSaveResult(CustomFormSaveResultS2C result) { saveResult = result; }

    public static CustomFormSaveResultS2C consumeSaveResult() {
        CustomFormSaveResultS2C result = saveResult;
        saveResult = null;
        return result;
    }
}
