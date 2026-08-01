package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.UtilityMenuScreen;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.MoreFormsNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.client.AddonGodFormNode;

import java.util.List;

@Mixin(value = UtilityMenuScreen.class, remap = false)
public abstract class UtilityMenuScreenMixin {
    @Shadow @Final private List<RadialNode> baseNodes;

    @Inject(method = "buildBaseNodes", at = @At("RETURN"))
    private void unofficialdmzaddon$installGodFormsSection(CallbackInfo ci) {
        if (baseNodes.size() < 3) return;
        baseNodes.set(1, new AddonGodFormNode());
        baseNodes.set(2, new MoreFormsNode());
    }
}
