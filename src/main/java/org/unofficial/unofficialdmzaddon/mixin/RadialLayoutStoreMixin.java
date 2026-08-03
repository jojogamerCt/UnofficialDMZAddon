package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.radial.RadialLayoutStore;
import com.dragonminez.common.util.lists.StackForms;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(value = RadialLayoutStore.class, remap = false)
public abstract class RadialLayoutStoreMixin {
    @Shadow @Final private static Map<String, List<String>> ORDER;
    private static boolean unofficialdmzaddon$migrating;

    @Inject(method = "ensureLoaded", at = @At("RETURN"))
    private static void unofficialdmzaddon$migrateAndPruneLayout(CallbackInfo ci) {
        if (unofficialdmzaddon$migrating) return;

        boolean changed = migrateLegacyGodLayout();
        if (!UnofficialDMZConfig.ULTRA_INSTINCT_ENABLED.get()) {
            changed |= removeDisabledFormGroup(StackForms.GROUP_ULTRAINSTINCT);
        }
        if (!UnofficialDMZConfig.ULTRA_EGO_ENABLED.get()) {
            changed |= removeDisabledFormGroup(StackForms.GROUP_ULTRAEGO);
        }
        if (!changed) return;

        unofficialdmzaddon$migrating = true;
        try {
            RadialLayoutStore.save();
        } finally {
            unofficialdmzaddon$migrating = false;
        }
    }

    private static boolean migrateLegacyGodLayout() {
        List<String> legacy = ORDER.get("superforms:supersaiyan");
        if (legacy == null) legacy = new ArrayList<>();
        boolean changed = false;
        List<String> god = new ArrayList<>(ORDER.getOrDefault("godforms:godforms", Collections.emptyList()));
        for (String form : List.of("super_saiyan_god", "super_saiyan_blue", "super_saiyan_rose", "super_saiyan_blue_evolved")) {
            String oldKey = "form:supersaiyan:" + form;
            String newKey = "form:godforms:" + form;
            if (legacy.remove(oldKey)) {
                if (!god.contains(newKey)) god.add(newKey);
                changed = true;
            }
        }
        if (changed) ORDER.put("godforms:godforms", god);
        return changed;
    }

    private static boolean removeDisabledFormGroup(String group) {
        boolean changed = ORDER.entrySet().removeIf(entry ->
                entry.getKey().equals(group)
                        || entry.getKey().endsWith(":" + group)
                        || entry.getKey().startsWith(group + ":"));

        String groupKey = "group:" + group;
        String stackGroupKey = "group:stack:" + group;
        String formPrefix = "form:" + group + ":";
        for (List<String> values : ORDER.values()) {
            changed |= values.removeIf(key ->
                    key.equals(groupKey) || key.equals(stackGroupKey) || key.startsWith(formPrefix));
        }
        return changed;
    }
}
