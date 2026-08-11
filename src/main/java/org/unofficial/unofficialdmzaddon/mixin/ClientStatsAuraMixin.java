package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.events.ClientStatsEvents;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.unofficial.unofficialdmzaddon.client.AddonAuraPolicy;

/** Replaces DMZ's unconditional God-form rubble with the player's addon-aura preference. */
@Mixin(value = ClientStatsEvents.class, remap = false)
public abstract class ClientStatsAuraMixin {
    @Redirect(method = "onClientTick", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/common/util/TransformationsHelper;hasGodFormActive(Lcom/dragonminez/common/stats/StatsData;)Z"),
            require = 0)
    private static boolean unofficialdmzaddon$gatePassiveParticles(StatsData data) {
        return AddonAuraPolicy.shouldSpawnPassiveDivineParticles(data);
    }
}
