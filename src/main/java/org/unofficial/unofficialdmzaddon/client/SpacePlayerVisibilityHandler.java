package org.unofficial.unofficialdmzaddon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;
import org.unofficial.unofficialdmzaddon.space.SpaceDimension;

/** Prevents forced space flight from making the local DMZ race model disappear. */
@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, value = Dist.CLIENT)
public final class SpacePlayerVisibilityHandler {
    private SpacePlayerVisibilityHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var player = Minecraft.getInstance().player;
        if (player == null || !player.level().dimension().equals(SpaceDimension.KEY)
                || !org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_VISIBILITY_RECOVERY.get()) return;
        if (player.isInvisible() && !player.hasEffect(MobEffects.INVISIBILITY)) {
            player.setInvisible(false);
        }
    }
}
