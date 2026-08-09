package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.events.SoundClientHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.config.FormConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.unofficial.unofficialdmzaddon.client.AddonAuraPolicy;

/** Starts DMZ's native loop sound for passive addon auras, including True Ultra Instinct. */
@Mixin(value = SoundClientHandler.class, remap = false)
public abstract class AuraSoundMixin {
    @ModifyVariable(method = "updatePlayerAuraSound", at = @At("STORE"), ordinal = 0)
    private static boolean unofficialdmzaddon$includeConstantAddonAura(
            boolean original, Player player, Minecraft minecraft) {
        return original || StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(AddonAuraPolicy::hasConstantAddonAura)
                .orElse(false);
    }

    @Redirect(method = "updatePlayerAuraSound", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/common/config/FormConfig$FormData;getHasLightnings()Ljava/lang/Boolean;"))
    private static Boolean unofficialdmzaddon$gatePassiveLightningSound(
            FormConfig.FormData form, Player player, Minecraft minecraft) {
        if (!form.getHasLightnings()) return false;
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(AddonAuraPolicy::shouldQueueSparks)
                .orElse(true);
    }
}
