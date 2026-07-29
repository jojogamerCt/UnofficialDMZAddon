package org.unofficial.unofficialdmzaddon.space;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Runs zero gravity, the invisible flight plane, planets, ki destruction, and planet travel. */
public final class SpaceEnvironmentHandler {
    public static final String FLY_UNLOCK_TAG = "unofficialdmzaddon_has_fly";
    private static final double SPACE_FLOOR_Y = 128.0D;
    private static final long TRAVEL_COOLDOWN_TICKS = 60L;

    private final Set<UUID> weightlessPlayers = new HashSet<>();
    private final Map<UUID, FlightAbilities> previousFlightAbilities = new HashMap<>();
    private final Map<UUID, long[]> destroyedPlanets = new HashMap<>();
    private final Map<UUID, Long> lastTravel = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player tickingPlayer = event.player;
        boolean hasFlySkill = StatsProvider.get(StatsCapability.INSTANCE, tickingPlayer)
                .map(data -> data.getSkills().getSkillLevel("fly") > 0)
                .orElse(false);
        if (hasFlySkill) tickingPlayer.addTag(FLY_UNLOCK_TAG);
        else tickingPlayer.removeTag(FLY_UNLOCK_TAG);

        if (tickingPlayer.level().isClientSide || !(tickingPlayer instanceof ServerPlayer player)) return;

        UUID playerId = player.getUUID();
        if (player.level().dimension().equals(SpaceDimension.KEY)) {
            weightlessPlayers.add(playerId);
            previousFlightAbilities.putIfAbsent(playerId,
                    new FlightAbilities(player.getAbilities().mayfly, player.getAbilities().flying));
            if (!player.getAbilities().mayfly || !player.getAbilities().flying) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            player.setNoGravity(true);
            player.setOnGround(false);
            player.fallDistance = 0.0F;

            if (player.getY() < SPACE_FLOOR_Y) {
                player.teleportTo(player.getX(), SPACE_FLOOR_Y, player.getZ());
                Vec3 movement = player.getDeltaMovement();
                player.setDeltaMovement(movement.x, Math.max(0.0D, movement.y), movement.z);
            }
            if (!player.isPassenger()) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(0.96D, 0.96D, 0.96D));
            }
        } else if (weightlessPlayers.remove(playerId)) {
            restorePlayer(player);
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !level.dimension().equals(SpaceDimension.KEY)) return;

        long gameTime = level.getGameTime();
        List<AbstractKiProjectile> projectiles = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof SpacePodEntity pod && pod.getY() < SPACE_FLOOR_Y) {
                pod.setPos(pod.getX(), SPACE_FLOOR_Y, pod.getZ());
                Vec3 movement = pod.getDeltaMovement();
                pod.setDeltaMovement(movement.x, Math.max(0.0D, movement.y), movement.z);
                pod.fallDistance = 0.0F;
            } else if (entity instanceof AbstractKiProjectile projectile) {
                projectiles.add(projectile);
            }
        }

        for (ServerPlayer player : level.players()) {
            processPlanets(level, player, projectiles, gameTime);
        }
    }

    private void processPlanets(ServerLevel space, ServerPlayer player, List<AbstractKiProjectile> projectiles,
                                long gameTime) {
        UUID playerId = player.getUUID();
        long[] destroyed = destroyedPlanets.computeIfAbsent(playerId, ignored -> freshDestroyedArray());
        List<SpacePlanetSystem.PlanetPlacement> placements = SpacePlanetSystem.layout(playerId, player.position());

        for (SpacePlanetSystem.PlanetPlacement placement : placements) {
            int index = placement.index();
            if (destroyed[index] >= 0L && gameTime - destroyed[index] >= SpacePlanetSystem.RESPAWN_TICKS) {
                destroyed[index] = -1L;
            }
            if (!SpacePlanetSystem.isDestroyed(gameTime, destroyed[index])) {
                for (AbstractKiProjectile projectile : projectiles) {
                    if (!playerId.equals(projectile.getOwnerUUID())) continue;
                    Vec3 previous = new Vec3(projectile.xo, projectile.yo, projectile.zo);
                    if (SpacePlanetSystem.segmentIntersects(previous, projectile.position(), placement.position(),
                            placement.definition().radius())) {
                        destroyed[index] = gameTime;
                        break;
                    }
                }
            }

            if (!SpacePlanetSystem.isDestroyed(gameTime, destroyed[index])
                    && player.position().distanceToSqr(placement.position())
                    <= square(placement.definition().radius() + 1.5D)
                    && gameTime - lastTravel.getOrDefault(playerId, Long.MIN_VALUE / 2L) > TRAVEL_COOLDOWN_TICKS) {
                travelToPlanet(space, player, placement.definition());
                lastTravel.put(playerId, gameTime);
                return;
            }
        }
    }

    private void travelToPlanet(ServerLevel space, ServerPlayer player, SpacePlanetDefinition planet) {
        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, planet.dimension());
        ServerLevel target = space.getServer().getLevel(targetKey);
        if (target == null || target == space) return;

        Vec3 targetPosition = resolveArrival(target, planet.id());
        Entity vehicle = player.getVehicle();
        player.stopRiding();
        if (vehicle instanceof SpacePodEntity ownPod) ownPod.discard();

        restorePlayer(player);
        weightlessPlayers.remove(player.getUUID());
        player.teleportTo(target, targetPosition.x, targetPosition.y, targetPosition.z,
                player.getYRot(), player.getXRot());

        SpacePodEntity newPod = new SpacePodEntity(MainEntities.SPACE_POD.get(), target);
        newPod.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
        if (target.addFreshEntity(newPod)) player.startRiding(newPod);
    }

    private Vec3 resolveArrival(ServerLevel target, String planetId) {
        if ("otherworld".equals(planetId)) return new Vec3(54.0D, 210.0D, 1082.0D);
        if ("time_chamber".equals(planetId)) return new Vec3(0.5D, 130.0D, 0.5D);
        BlockPos spawn = target.getSharedSpawnPos();
        int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ()) + 1;
        return new Vec3(spawn.getX() + 0.5D, Math.max(y, target.getMinBuildHeight() + 2), spawn.getZ() + 0.5D);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        if (weightlessPlayers.remove(playerId)) restorePlayer(event.getEntity());
        destroyedPlanets.remove(playerId);
        lastTravel.remove(playerId);
    }

    private void restorePlayer(Player player) {
        player.setNoGravity(false);
        FlightAbilities previous = previousFlightAbilities.remove(player.getUUID());
        if (previous != null) {
            player.getAbilities().mayfly = previous.mayfly();
            player.getAbilities().flying = previous.flying();
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.onUpdateAbilities();
        }
    }

    private static long[] freshDestroyedArray() {
        long[] values = new long[SpacePlanetSystem.PLANETS.size()];
        java.util.Arrays.fill(values, -1L);
        return values;
    }

    private static double square(double value) {
        return value * value;
    }

    private record FlightAbilities(boolean mayfly, boolean flying) {
    }
}