package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.layer.DMZAuraLayer;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.unofficial.unofficialdmzaddon.dmz.DivineAuraHelper;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/** Queues UI/UE aura rendering without mutating DragonMineZ's Aura Status. */
@Mixin(value = DMZAuraLayer.class, remap = false)
public abstract class PersistentDivineAuraMixin {
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 0)
    private boolean unofficialdmzaddon$renderIndependentDivineAura(
            boolean original, PoseStack poseStack, AbstractClientPlayer player,
            BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource) {
        if (original) return true;
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(DivineAuraHelper::hasPersistentAura)
                .orElse(false);
    }
}
