package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.RadialForms;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = RadialForms.class, remap = false)
public abstract class RadialFormsMixin {
    @Inject(method = "moreForms", at = @At("HEAD"), cancellable = true)
    private static void unofficialdmzaddon$removeGodFormsFromExtras(StatsData stats,
            CallbackInfoReturnable<List<RadialNode>> cir) {
        cir.setReturnValue(RadialFormsAccessor.unofficialdmzaddon$forms(stats, "moreforms",
                type -> !type.contains("super") && !type.contains("legendary")
                        && !type.contains("android") && !type.contains("god")));
    }
}
