package org.unofficial.unofficialdmzaddon.space;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
    private static final double SPACE_POD_SPEED_MULTIPLIER = 3.0D;

    private final Set<UUID> weightlessPlayers = new HashSet<>();
    private final Map<UUID, FlightAbilities> previousFlightAbilities = new HashMap<>();
    private final Map<UUID, long[]> destroyedPlanets = new HashMap<>();
    private final Map<UUID, Long> lastTravel = new HashMap<>();
    private final Map<UUID, ResourceLocation> pendingSpaceEntries = new HashMap<>();

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
            positionNearSourcePlanet(player);
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

            if (player.getVehicle() instanceof SpacePodEntity ridingPod) {
                keepPodAboveFloor(ridingPod);
                if (player.getY() < SPACE_FLOOR_Y) {
                    player.setPos(ridingPod.getX(), Math.max(SPACE_FLOOR_Y, ridingPod.getY()) + 0.4D, ridingPod.getZ());
                }
            } else if (player.getY() <= SPACE_FLOOR_Y) {
                if (player.getY() < SPACE_FLOOR_Y) player.teleportTo(player.getX(), SPACE_FLOOR_Y, player.getZ());
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
            if (entity instanceof SpacePodEntity pod) {
                keepPodAboveFloor(pod);
                if (pod.getControllingPassenger() instanceof Player) {
                    pod.setOpenNave(false);
                    Vec3 bonusMovement = pod.getDeltaMovement().scale(SPACE_POD_SPEED_MULTIPLIER - 1.0D);
                    if (bonusMovement.lengthSqr() > 1.0E-7D) pod.move(MoverType.SELF, bonusMovement);
                    // The speed boost runs after the pod's normal movement, so enforce the floor again afterwards.
                    keepPodAboveFloor(pod);
                }
            } else if (entity instanceof AbstractKiProjectile projectile) {
                projectiles.add(projectile);
            }
        }

        for (ServerPlayer player : List.copyOf(level.players())) {
            processPlanets(level, player, projectiles, gameTime);
        }
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTo().equals(SpaceDimension.KEY)) {
            pendingSpaceEntries.put(player.getUUID(), event.getFrom().location());
        }
    }

    private void positionNearSourcePlanet(ServerPlayer player) {
        ResourceLocation sourceDimension = pendingSpaceEntries.remove(player.getUUID());
        if (sourceDimension == null) return;

        SpacePlanetSystem.PlanetPlacement sourcePlanet = SpacePlanetSystem.layout(player.getUUID(), player.position())
                .stream()
                .filter(placement -> placement.definition().dimension().equals(sourceDimension))
                .findFirst()
                .orElse(null);

        Vec3 entryPosition;
        if (sourcePlanet != null) {
            // Spawn directly above the source planet. This is deterministic, clear of its cubic bounds,
            // and cannot point back through the sun like an impact-relative horizontal offset can.
            double clearance = sourcePlanet.definition().radius() + 14.0D;
            entryPosition = sourcePlanet.position().add(0.0D, clearance, 0.0D);
        } else {
            // Dimensions without a planet no longer inherit arbitrary coordinates that may be inside a body.
            Vec3 sun = SpacePlanetSystem.sunPosition(player.position());
            entryPosition = sun.add(0.0D, SpacePlanetSystem.SUN_RADIUS + 20.0D, 0.0D);
        }
        entryPosition = new Vec3(entryPosition.x, Math.max(SPACE_FLOOR_Y, entryPosition.y), entryPosition.z);

        Entity vehicle = player.getVehicle();
        SpacePodEntity pod;
        if (vehicle instanceof SpacePodEntity existingPod) {
            pod = existingPod;
        } else {
            pod = new SpacePodEntity(MainEntities.SPACE_POD.get(), player.serverLevel());
            if (!player.serverLevel().addFreshEntity(pod)) return;
        }
        pod.setOpenNave(false);
        pod.setPos(entryPosition.x, entryPosition.y, entryPosition.z);
        pod.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(entryPosition.x, entryPosition.y, entryPosition.z);
        player.setDeltaMovement(Vec3.ZERO);
        if (!player.isPassenger()) player.startRiding(pod);
    }
    private static void keepPodAboveFloor(SpacePodEntity pod) {
        if (pod.getY() < SPACE_FLOOR_Y) pod.setPos(pod.getX(), SPACE_FLOOR_Y, pod.getZ());
        if (pod.getY() <= SPACE_FLOOR_Y) {
            Vec3 movement = pod.getDeltaMovement();
            if (movement.y < 0.0D) pod.setDeltaMovement(movement.x, 0.0D, movement.z);
            pod.fallDistance = 0.0F;
        }
    }
    private void processPlanets(ServerLevel space, ServerPlayer player, List<AbstractKiProjectile> projectiles,
                                long gameTime) {
        UUID playerId = player.getUUID();
        long[] destroyed = destroyedPlanets.computeIfAbsent(playerId, ignored -> freshDestroyedArray());
        Vec3 travelPosition = player.getVehicle() instanceof SpacePodEntity pod ? pod.position() : player.position();
        List<SpacePlanetSystem.PlanetPlacement> placements = SpacePlanetSystem.layout(playerId, travelPosition);

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
                    && travelPosition.distanceToSqr(placement.position())
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
        boolean travellingWithPod = vehicle instanceof SpacePodEntity;
        if (vehicle instanceof SpacePodEntity ownPod) {
            ownPod.ejectPassengers();
            ownPod.remove(Entity.RemovalReason.DISCARDED);
        } else {
            player.stopRiding();
        }

        restorePlayer(player);
        weightlessPlayers.remove(player.getUUID());
        player.teleportTo(target, targetPosition.x, targetPosition.y, targetPosition.z,
                player.getYRot(), player.getXRot());

        if (travellingWithPod) {
            SpacePodEntity newPod = new SpacePodEntity(MainEntities.SPACE_POD.get(), target);
            newPod.setOpenNave(false);
            newPod.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
            newPod.setYRot(player.getYRot());
            newPod.setDeltaMovement(Vec3.ZERO);
            if (target.addFreshEntity(newPod)) player.startRiding(newPod);
        }
    }

    private Vec3 resolveArrival(ServerLevel target, String planetId) {
        BlockPos spawn = target.getSharedSpawnPos();
        int baseX = spawn.getX();
        int baseZ = spawn.getZ();
        for (int radius = 0; radius <= 32; radius += 2) {
            int step = Math.max(1, radius);
            for (int dx = -radius; dx <= radius; dx += step) {
                for (int dz = -radius; dz <= radius; dz += step) {
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos feet = new BlockPos(x, Math.max(y, target.getMinBuildHeight() + 2), z);
                    var floor = target.getBlockState(feet.below());
                    if (floor.isCollisionShapeFullBlock(target, feet.below()) && !floor.is(Blocks.MAGMA_BLOCK)
                            && target.getBlockState(feet).getCollisionShape(target, feet).isEmpty()
                            && target.getBlockState(feet.above()).getCollisionShape(target, feet.above()).isEmpty()
                            && target.getFluidState(feet).isEmpty() && target.getFluidState(feet.below()).isEmpty()) {
                        return new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                    }
                }
            }
        }
        int fallbackY = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX, baseZ);
        return new Vec3(baseX + 0.5D, Math.max(fallbackY, target.getMinBuildHeight() + 2), baseZ + 0.5D);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        if (weightlessPlayers.remove(playerId)) restorePlayer(event.getEntity());
        destroyedPlanets.remove(playerId);
        lastTravel.remove(playerId);
        pendingSpaceEntries.remove(playerId);
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