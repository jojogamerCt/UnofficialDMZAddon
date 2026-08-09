package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.init.sounds.AuraLoopSound;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Status;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.unofficial.unofficialdmzaddon.client.AddonAuraPolicy;

/** Keeps the native loop alive while a passive addon aura is actually visible. */
@Mixin(value = AuraLoopSound.class, remap = false)
public abstract class AuraLoopSoundMixin {
    @Shadow @Final private Player player;

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/common/stats/character/Status;isPermanentAura()Z"))
    private boolean unofficialdmzaddon$keepConstantAddonSoundAlive(Status status) {
        return status.isPermanentAura() || StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(AddonAuraPolicy::hasConstantAddonAura)
                .orElse(false);
    }
}
