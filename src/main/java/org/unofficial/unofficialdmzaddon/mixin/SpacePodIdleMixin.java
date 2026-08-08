package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.init.entities.SpacePodEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.space.SpaceDimension;

/** Prevents DMZ's unoccupied-pod fallback travel from applying downward motion in space. */
@Mixin(value = SpacePodEntity.class, remap = false)
public abstract class SpacePodIdleMixin {
    @Inject(method = {"travel(Lnet/minecraft/world/phys/Vec3;)V", "m_7023_(Lnet/minecraft/world/phys/Vec3;)V"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void unofficialdmzaddon$holdUnoccupiedPodInSpace(Vec3 travelVector, CallbackInfo ci) {
        SpacePodEntity pod = (SpacePodEntity) (Object) this;
        if (!SpaceDimension.isSpace(pod.level().dimension()) || pod.getControllingPassenger() != null) return;

        pod.setNoGravity(true);
        pod.setDeltaMovement(Vec3.ZERO);
        pod.fallDistance = 0.0F;
        ci.cancel();
    }
}
