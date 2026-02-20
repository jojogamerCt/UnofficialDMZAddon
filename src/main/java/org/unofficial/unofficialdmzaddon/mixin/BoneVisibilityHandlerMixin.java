package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.util.BoneVisibilityHandler;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.dmz.SpecialRaceFormsDefinitions;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;

@Mixin(value = BoneVisibilityHandler.class, remap = false)
public abstract class BoneVisibilityHandlerMixin {

    /**
     * After DMZ sets bone visibility, hide the tail for the Alien race.
     * DMZ only explicitly hides tail1 for human/namekian/majin/saiyan —
     * every other race (including alien) would show it by default.
     */
    @Inject(method = "updateVisibility", at = @At("RETURN"))
    private static void unofficialdmzaddon$hideAlienTail(
            BakedGeoModel model,
            AbstractClientPlayer player,
            CallbackInfo ci) {

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
            String race = stats.getCharacter().getRaceName();
            if (SpecialRaceFormsDefinitions.ALIEN_RACE.equals(race)) {
                model.getBone("tail1").ifPresent(bone -> unofficialdmzaddon$setHiddenRecursive(bone, true));
            }
        });
    }

    @Unique
    private static void unofficialdmzaddon$setHiddenRecursive(software.bernie.geckolib.cache.object.GeoBone bone, boolean hide) {
        bone.setHidden(hide);
        for (var child : bone.getChildBones()) {
            unofficialdmzaddon$setHiddenRecursive(child, hide);
        }
    }
}
