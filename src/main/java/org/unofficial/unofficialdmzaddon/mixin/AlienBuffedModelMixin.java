package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.model.DMZPlayerModel;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.dmz.SpecialRaceFormsDefinitions;

/** Uses Broly's muscular geometry for Alien while preserving Alien texture-layer resolution. */
@Mixin(value = DMZPlayerModel.class, remap = false)
public abstract class AlienBuffedModelMixin {
    private static final ResourceLocation BROLY_BUFFED_MODEL =
            ResourceLocation.fromNamespaceAndPath("dragonminez", "geo/entity/races/hbuffed.geo.json");

    @Inject(method = "getModelResource", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$useBrolyBodyForAlien(AbstractClientPlayer player,
                                                          CallbackInfoReturnable<ResourceLocation> cir) {
        if (player.hasEffect(MainEffects.CANDY.get())) {
            return;
        }
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (SpecialRaceFormsDefinitions.ALIEN_RACE.equalsIgnoreCase(data.getCharacter().getRaceName())) {
                cir.setReturnValue(BROLY_BUFFED_MODEL);
            }
        });
    }
}