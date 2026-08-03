package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skill;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AuraRenderer.class, remap = false)
public abstract class SearchFlightAuraMixin {
    @Inject(
            method = "renderShaderAura",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/render/effects/AuraRenderer;executeAuraShaderDraw(Lnet/minecraft/world/entity/player/Player;Lcom/dragonminez/client/render/effects/AuraRenderer$CachedAuraData;Lcom/dragonminez/client/render/effects/AuraRenderer$AuraLayer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Minecraft;Lorg/joml/Matrix4f;FFZ)V"
            )
    )
    private static void unofficialdmzaddon$turnSearchFlightAuraSideways(
            PlayerEffectQueue.AuraRenderEntry entry,
            PoseStack poseStack,
            Minecraft minecraft,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        Player player = entry.player();
        if (player == null || player.getDeltaMovement().lengthSqr() < 0.01D) return;

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            Skill fly = data.getSkills().getSkill("fly");
            if (fly == null || !fly.isActive() || data.getStatus().getFlightMode() != Status.FLIGHT_SEARCH) return;

            float yaw = Mth.rotLerp(entry.partialTick(), player.yRotO, player.getYRot());
            float pitch = Mth.clamp(Mth.lerp(entry.partialTick(), player.xRotO, player.getXRot()), -75.0F, 75.0F);

            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F + pitch));
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.translate(0.0D, -0.45D, -0.20D);
        });
    }
}
