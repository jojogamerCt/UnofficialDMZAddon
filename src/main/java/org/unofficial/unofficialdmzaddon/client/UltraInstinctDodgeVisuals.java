package org.unofficial.unofficialdmzaddon.client;

import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, value = Dist.CLIENT)
public final class UltraInstinctDodgeVisuals {
    private static final long DODGE_DURATION_MS = 360L;
    private static final float MAX_LEAN_DEGREES = 45.0F;
    private static final Map<UUID, DodgeAnimation> ACTIVE_DODGES = new HashMap<>();
    private UltraInstinctDodgeVisuals() { }
    public static void trigger(UUID playerId, boolean leanRight) {
        ACTIVE_DODGES.put(playerId,
                new DodgeAnimation(System.currentTimeMillis(), leanRight ? 1.0F : -1.0F));
    }
    @SubscribeEvent
    public static void beforePlayerRender(RenderPlayerEvent.Pre event) {
        DodgeAnimation animation = ACTIVE_DODGES.get(event.getEntity().getUUID());
        if (animation == null) return;
        long elapsed = System.currentTimeMillis() - animation.startedAt();
        if (elapsed >= DODGE_DURATION_MS) {
            ACTIVE_DODGES.remove(event.getEntity().getUUID());
            return;
        }
        float progress = elapsed / (float) DODGE_DURATION_MS;
        float easedLean = (float) Math.sin(progress * Math.PI);
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(
                animation.direction() * MAX_LEAN_DEGREES * easedLean));
    }
    private record DodgeAnimation(long startedAt, float direction) { }
}