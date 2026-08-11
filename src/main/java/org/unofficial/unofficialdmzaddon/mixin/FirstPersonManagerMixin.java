package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.common.init.entities.SpacePodEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

/**
 * Applies addon-specific safety opt-outs without overriding DragonMineZ's own
 * first-person model setting.
 */
@Mixin(value = FirstPersonManager.class, remap = false)
public abstract class FirstPersonManagerMixin {

    @Inject(method = "shouldRenderFirstPerson", at = @At("RETURN"), cancellable = true)
    private static void unofficialdmzaddon$respectFirstPersonSettings(
            Player player, CallbackInfoReturnable<Boolean> cir) {
        // A scaled race model fills the camera while the passenger is seated inside the pod.
        if (player.getVehicle() instanceof SpacePodEntity) {
            cir.setReturnValue(false);
            return;
        }

        // This addon option remains a second opt-out, never an override of DMZ's user setting.
        if (!UnofficialDMZConfig.FIRST_PERSON_RACE_MODEL.get()) cir.setReturnValue(false);
    }
}
