package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.client.animation.IPlayerAnimatable;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, value = Dist.CLIENT)
public final class UltraInstinctDodgeVisuals {
    private UltraInstinctDodgeVisuals() { }

    public static void trigger(UUID playerId, boolean leanRight) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        if (level.getPlayerByUUID(playerId) instanceof AbstractClientPlayer player
                && player instanceof IPlayerAnimatable animatable) {
            animatable.dragonminez$triggerEvasion();
        }
    }
}
