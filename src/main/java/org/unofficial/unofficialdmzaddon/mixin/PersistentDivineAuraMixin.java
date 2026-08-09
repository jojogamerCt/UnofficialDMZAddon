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
import org.unofficial.unofficialdmzaddon.client.AddonAuraPolicy;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/** Queues UI/UE aura rendering without mutating DragonMineZ's Aura Status. */
@Mixin(value = DMZAuraLayer.class, remap = false)
public abstract class PersistentDivineAuraMixin {
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 0)
    private boolean unofficialdmzaddon$renderIndependentDivineAura(
            boolean original, PoseStack poseStack, AbstractClientPlayer player,
            BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource) {
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> original || AddonAuraPolicy.hasConstantAddonAura(data))
                .orElse(original);
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/client/render/util/PlayerEffectQueue;addSpark(Lnet/minecraft/client/player/AbstractClientPlayer;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lcom/mojang/blaze3d/vertex/PoseStack;FI)V"))
    private void unofficialdmzaddon$gatePassiveSparks(AbstractClientPlayer player, BakedGeoModel model,
                                                       PoseStack poseStack, float partialTick, int packedLight) {
        boolean allowed = StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(AddonAuraPolicy::shouldQueueSparks)
                .orElse(true);
        if (allowed) PlayerEffectQueue.addSpark(player, model, poseStack, partialTick, packedLight);
    }
}
