package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.technique.CanonTechniqueHandler;

@Mixin(value = TechniqueDispatcher.class, remap = false)
public abstract class TechniqueDispatcherMixin {
    @Inject(method = "executeKiAttack", at = @At("HEAD"), cancellable = true)
    private static void unofficial$canonKi(LivingEntity owner, Level level, KiAttackData data, StatsData stats, float charge,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (CanonTechniqueHandler.executeCustomKi(owner, level, data, stats, charge)) cir.setReturnValue(true);
    }
}
