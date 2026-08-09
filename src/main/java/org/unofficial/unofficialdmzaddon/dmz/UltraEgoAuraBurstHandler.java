package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.unofficial.unofficialdmzaddon.network.AddonNetwork;

public final class UltraEgoAuraBurstHandler {
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0.0F || event.getEntity().level().isClientSide
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (DivineAuraHelper.isUltraEgo(data)) AddonNetwork.sendUltraEgoBurst(player);
        });
    }
}
