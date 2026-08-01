package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.lists.StackForms;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;

@Mixin(value = AuraRenderer.class, remap = false)
public abstract class BlueKaiokenAuraMixin {
    @Inject(method = "getAuraLayers", at = @At("RETURN"))
    private static void unofficialdmzaddon$keepBlueAndKaiokenLayers(Player player, StatsData stats, float partialTick,
            CallbackInfoReturnable<List<AuraRenderer.AuraLayer>> cir) {
        var character = stats.getCharacter();
        if (!"godforms".equalsIgnoreCase(character.getActiveFormGroup())
                || !"super_saiyan_blue".equalsIgnoreCase(character.getActiveForm())
                || !StackForms.GROUP_KAIOKEN.equalsIgnoreCase(character.getActiveStackFormGroup())
                || character.getActiveFormData() == null || character.getActiveStackFormData() == null) return;

        var blue = character.getActiveFormData();
        var kaioken = character.getActiveStackFormData();
        int blueLayer = blue.getAuraLayer() == null ? 1 : blue.getAuraLayer();
        int kaiokenLayer = kaioken.getAuraLayer() == null ? 3 : kaioken.getAuraLayer();
        if (kaiokenLayer == blueLayer) kaiokenLayer = blueLayer == 3 ? 4 : 3;

        List<AuraRenderer.AuraLayer> layers = cir.getReturnValue();
        int finalKaiokenLayer = kaiokenLayer;
        layers.removeIf(layer -> layer.layerId == blueLayer || layer.layerId == finalKaiokenLayer);
        layers.add(new AuraRenderer.AuraLayer(blue.getAuraType(), blueLayer, blue.getRgbAuraColor()));
        layers.add(new AuraRenderer.AuraLayer(kaioken.getAuraType(), kaiokenLayer, kaioken.getRgbAuraColor()));
        layers.sort(Comparator.comparingInt(layer -> layer.layerId));
    }
}
