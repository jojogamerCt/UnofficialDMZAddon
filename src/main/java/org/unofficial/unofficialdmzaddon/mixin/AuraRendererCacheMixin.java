package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.client.AddonAuraPolicy;

import java.util.Map;
import java.util.Set;

/** Immediately drops stale cached addon auras after the preference is disabled. */
@Mixin(value = AuraRenderer.class, remap = false)
public abstract class AuraRendererCacheMixin {
    @Shadow @Final private static Map<Integer, ?> AURA_CACHE;

    @Inject(method = "processGhostAuras", at = @At("HEAD"))
    private static void unofficialdmzaddon$clearDisabledAddonGhosts(
            Minecraft minecraft, PoseStack poseStack, Matrix4f projectionMatrix,
            float partialTick, Set<Integer> currentFramePlayers, CallbackInfo callback) {
        if (minecraft.level == null) return;
        AURA_CACHE.keySet().removeIf(entityId -> {
            if (!(minecraft.level.getEntity(entityId) instanceof Player player)) return false;
            return StatsProvider.get(StatsCapability.INSTANCE, player)
                    .map(data -> AddonAuraPolicy.isAddonAuraForm(data)
                            && !AddonAuraPolicy.shouldShowAddonAura(data))
                    .orElse(false);
        });
    }
}
