package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.radial.RadialLayoutStore;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(value = RadialLayoutStore.class, remap = false)
public abstract class RadialLayoutStoreMixin {
    @Shadow @Final private static Map<String, List<String>> ORDER;
    private static boolean unofficialdmzaddon$migrating;

    @Inject(method = "ensureLoaded", at = @At("RETURN"))
    private static void unofficialdmzaddon$migrateLegacyGodLayout(CallbackInfo ci) {
        if (unofficialdmzaddon$migrating) return;
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
        if (!changed) return;
        ORDER.put("godforms:godforms", god);
        unofficialdmzaddon$migrating = true;
        try {
            RadialLayoutStore.save();
        } finally {
            unofficialdmzaddon$migrating = false;
        }
    }
}
