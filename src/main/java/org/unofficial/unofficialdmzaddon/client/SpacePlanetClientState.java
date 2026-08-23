package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;
import org.unofficial.unofficialdmzaddon.space.SpaceDimension;
import org.unofficial.unofficialdmzaddon.space.SpacePlanetSystem;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, value = Dist.CLIENT)
public final class SpacePlanetClientState {
    private static final long[] DESTROYED_AT = new long[SpacePlanetSystem.PLANETS.size()];

    static {
        Arrays.fill(DESTROYED_AT, -1L);
    }

    private SpacePlanetClientState() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !SpaceDimension.isSolarSpace(player.level().dimension())) return;

        long gameTime = player.level().getGameTime();
        List<SpacePlanetSystem.PlanetPlacement> placements = SpacePlanetSystem.layout(player.position(), gameTime);
        List<AbstractKiProjectile> projectiles = player.level().getEntitiesOfClass(AbstractKiProjectile.class,
                player.getBoundingBox().inflate(320.0D));

        for (SpacePlanetSystem.PlanetPlacement placement : placements) {
            int index = placement.index();
            if (DESTROYED_AT[index] >= 0L && gameTime - DESTROYED_AT[index] >= SpacePlanetSystem.RESPAWN_TICKS) {
                DESTROYED_AT[index] = -1L;
            }
            if (!isUnlocked(player, placement)) continue;
            if (SpacePlanetSystem.isDestroyed(gameTime, DESTROYED_AT[index])) continue;

            for (AbstractKiProjectile projectile : projectiles) {
                if (!player.getUUID().equals(projectile.getOwnerUUID())) continue;
                if (!projectile.isFiring() || projectile.tickCount < 2) continue;
                Vec3 previous = projectile.position().subtract(projectile.getDeltaMovement());
                if (SpacePlanetSystem.segmentIntersectsRotatedCube(previous, projectile.position(),
                        placement.position(), placement.definition().radius(), placement.spinAngle())) {
                    DESTROYED_AT[index] = gameTime;
                    break;
                }
            }
        }
    }

    public static long destroyedAt(int index) {
        return DESTROYED_AT[index];
    }

    public static boolean isUnlocked(Player player, SpacePlanetSystem.PlanetPlacement placement) {
        String dimension = placement.definition().dimension().toString();
        return SpacePodDestinationRegistry.getClientDestinations().stream()
                .filter(destination -> destination.dimension().equals(dimension))
                .findFirst()
                .map(destination -> destination.unlockRules().test(player))
                .orElse(true);
    }
}
