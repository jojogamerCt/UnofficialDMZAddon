package org.unofficial.unofficialdmzaddon.space;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
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
    private static final long TRAVEL_COOLDOWN_TICKS = 60L;
    public static final String LAST_PLANET_POSITIONS_TAG = "unofficialdmzaddonLastPlanetPositions";

    private final Set<UUID> weightlessPlayers = new HashSet<>();
    private final Map<UUID, FlightAbilities> previousFlightAbilities = new HashMap<>();
    private final Map<UUID, long[]> destroyedPlanets = new HashMap<>();
    private final Map<UUID, Long> lastTravel = new HashMap<>();
    private final Map<UUID, SpaceEntry> pendingSpaceEntries = new HashMap<>();
    private final Map<UUID, ResourceLocation> pendingPlanetEntries = new HashMap<>();
    private final Map<UUID, Boolean> recentlyRidingPod = new HashMap<>();
    private final Map<UUID, Vec3> previousTravelPositions = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player tickingPlayer = event.player;
        boolean hasFlySkill = StatsProvider.get(StatsCapability.INSTANCE, tickingPlayer)
                .map(data -> data.getSkills().getSkillLevel("fly") > 0)
                .orElse(false);
        if (hasFlySkill || !org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_REQUIRE_FLY.get()) tickingPlayer.addTag(FLY_UNLOCK_TAG);
        else tickingPlayer.removeTag(FLY_UNLOCK_TAG);

        if (tickingPlayer.level().isClientSide || !(tickingPlayer instanceof ServerPlayer player)) return;

        if (!player.level().dimension().equals(SpaceDimension.KEY)) {
            positionAtSavedPlanetReturn(player);
            rememberPlanetPosition(player);
            recentlyRidingPod.put(player.getUUID(), player.getVehicle() instanceof SpacePodEntity);
        }

        UUID playerId = player.getUUID();
        if (player.level().dimension().equals(SpaceDimension.KEY)
                && org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_ENABLED.get()) {
            positionNearSourcePlanet(player);
            weightlessPlayers.add(playerId);
            previousFlightAbilities.putIfAbsent(playerId,
                    new FlightAbilities(player.getAbilities().mayfly, player.getAbilities().flying));
            if (!player.getAbilities().mayfly || !player.getAbilities().flying) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            if (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_VISIBILITY_RECOVERY.get()
                    && player.isInvisible() && !player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) {
                player.setInvisible(false);
            }
            player.setNoGravity(true);
            player.setOnGround(false);
            player.fallDistance = 0.0F;

            if (player.getVehicle() instanceof SpacePodEntity ridingPod) {
                keepPodAboveFloor(ridingPod);
                if (player.getY() < org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get()) {
                    player.setPos(ridingPod.getX(), Math.max(org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get(), ridingPod.getY()) + 0.4D, ridingPod.getZ());
                }
            } else if (player.getY() <= org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get()) {
                if (player.getY() < org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get()) player.teleportTo(player.getX(), org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get(), player.getZ());
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
                pod.setNoGravity(true);
                keepPodAboveFloor(pod);
                if (pod.getControllingPassenger() instanceof Player) {
                    pod.setOpenNave(false);
                    Vec3 movement = pod.getDeltaMovement();
                    if (movement.y < 0.0D && Math.abs(movement.y) < 0.12D) pod.setDeltaMovement(movement.x, 0.0D, movement.z);
                    Vec3 bonusMovement = pod.getDeltaMovement().scale(org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_POD_SPEED_MULTIPLIER.get() - 1.0D);
                    if (bonusMovement.lengthSqr() > 1.0E-7D) pod.move(MoverType.SELF, bonusMovement);
                    keepPodAboveFloor(pod);
                } else {
                    pod.setDeltaMovement(Vec3.ZERO);
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
    public void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getDimension().equals(SpaceDimension.KEY)
                && !player.level().dimension().equals(SpaceDimension.KEY)) {
            rememberPlanetPosition(player);
            recentlyRidingPod.put(player.getUUID(), player.getVehicle() instanceof SpacePodEntity);
        }
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (event.getTo().equals(SpaceDimension.KEY)) {
                pendingSpaceEntries.put(player.getUUID(), new SpaceEntry(event.getFrom().location(),
                        recentlyRidingPod.getOrDefault(player.getUUID(), false)));
            } else if (event.getFrom().equals(SpaceDimension.KEY)) {
                pendingPlanetEntries.put(player.getUUID(), event.getTo().location());
            }
        }
    }

    public static Vec3 spaceEntryPosition(ServerPlayer player, ResourceLocation sourceDimension) {
        SpacePlanetSystem.PlanetPlacement sourcePlanet = SpacePlanetSystem.layout(player.getUUID(), Vec3.ZERO)
                .stream()
                .filter(placement -> placement.definition().dimension().equals(sourceDimension))
                .findFirst()
                .orElse(null);
        Vec3 entryPosition;
        if (sourcePlanet != null) {
            double clearance = sourcePlanet.definition().radius()
                    + org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_SPAWN_CLEARANCE.get();
            entryPosition = sourcePlanet.position().add(0.0D, clearance, 0.0D);
        } else {
            Vec3 sun = SpacePlanetSystem.sunPosition(Vec3.ZERO);
            entryPosition = sun.add(0.0D, SpacePlanetSystem.SUN_RADIUS + 20.0D, 0.0D);
        }
        return new Vec3(entryPosition.x,
                Math.max(org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get(), entryPosition.y),
                entryPosition.z);
    }
    private void positionNearSourcePlanet(ServerPlayer player) {
        SpaceEntry entry = pendingSpaceEntries.remove(player.getUUID());
        if (entry == null) return;
        ResourceLocation sourceDimension = entry.sourceDimension();

        SpacePlanetSystem.PlanetPlacement sourcePlanet = SpacePlanetSystem.layout(player.getUUID(), Vec3.ZERO)
                .stream()
                .filter(placement -> placement.definition().dimension().equals(sourceDimension))
                .findFirst()
                .orElse(null);

        Vec3 entryPosition;
        if (sourcePlanet != null) {
            // Spawn directly above the source planet. This is deterministic, clear of its cubic bounds,
            // and cannot point back through the sun like an impact-relative horizontal offset can.
            double clearance = sourcePlanet.definition().radius() + org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_SPAWN_CLEARANCE.get();
            entryPosition = sourcePlanet.position().add(0.0D, clearance, 0.0D);
        } else {
            // Dimensions without a planet no longer inherit arbitrary coordinates that may be inside a body.
            Vec3 sun = SpacePlanetSystem.sunPosition(Vec3.ZERO);
            entryPosition = sun.add(0.0D, SpacePlanetSystem.SUN_RADIUS + 20.0D, 0.0D);
        }
        entryPosition = new Vec3(entryPosition.x, Math.max(org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get(), entryPosition.y), entryPosition.z);

        Entity arrivalVehicle = player.getVehicle();
        if (arrivalVehicle instanceof SpacePodEntity packetPod) {
            movePodAndPassenger(packetPod, player, entryPosition, player.getYRot(), player.getXRot());
        } else {
            player.teleportTo(entryPosition.x, entryPosition.y, entryPosition.z);
            player.setDeltaMovement(Vec3.ZERO);
        }
        if (entry.withPod() && !(player.getVehicle() instanceof SpacePodEntity)) {
            SpacePodEntity pod = new SpacePodEntity(MainEntities.SPACE_POD.get(), player.serverLevel());
            pod.setOpenNave(false);
            pod.setPos(entryPosition.x, entryPosition.y, entryPosition.z);
            pod.setDeltaMovement(Vec3.ZERO);
            if (player.serverLevel().addFreshEntity(pod)) player.startRiding(pod);
        }
        lastTravel.put(player.getUUID(), player.serverLevel().getGameTime());
        previousTravelPositions.put(player.getUUID(), entryPosition);
    }
    private static void keepPodAboveFloor(SpacePodEntity pod) {
        if (pod.getY() < org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get()) pod.setPos(pod.getX(), org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get(), pod.getZ());
        if (pod.getY() <= org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get()) {
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
        Vec3 previousTravelPosition = previousTravelPositions.getOrDefault(playerId, travelPosition);
        List<SpacePlanetSystem.PlanetPlacement> placements = SpacePlanetSystem.layout(playerId, travelPosition);

        for (SpacePlanetSystem.PlanetPlacement placement : placements) {
            int index = placement.index();
            if (destroyed[index] >= 0L && gameTime - destroyed[index] >= SpacePlanetSystem.RESPAWN_TICKS) {
                destroyed[index] = -1L;
            }
            if (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_PLANET_DESTRUCTION.get()
                    && !SpacePlanetSystem.isDestroyed(gameTime, destroyed[index])) {
                for (AbstractKiProjectile projectile : projectiles) {
                    if (!playerId.equals(projectile.getOwnerUUID())) continue;
                    if (!projectile.isFiring() || projectile.tickCount < 2) continue;
                    Vec3 previous = projectile.position().subtract(projectile.getDeltaMovement());
                    if (SpacePlanetSystem.segmentIntersectsCube(previous, projectile.position(), placement.position(),
                            placement.definition().radius())) {
                        destroyed[index] = gameTime;
                        putOnEvilPath(player);
                        break;
                    }
                }
            }

            if (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_PLANET_TRAVEL.get()
                    && !SpacePlanetSystem.isDestroyed(gameTime, destroyed[index])
                    && SpacePlanetSystem.segmentIntersectsCube(previousTravelPosition, travelPosition,
                    placement.position(), placement.definition().radius() + org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_PLANET_ENTRY_MARGIN.get())
                    && gameTime - lastTravel.getOrDefault(playerId, Long.MIN_VALUE / 2L) > TRAVEL_COOLDOWN_TICKS) {
                travelToPlanet(space, player, placement.definition());
                lastTravel.put(playerId, gameTime);
                return;
            }
        }
        previousTravelPositions.put(playerId, travelPosition);
    }

    private static void putOnEvilPath(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (data.getResources().getAlignment() <= 40) return;
            data.getResources().setAlignment(0);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
    }

    private void travelToPlanet(ServerLevel space, ServerPlayer player, SpacePlanetDefinition planet) {
        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, planet.dimension());
        ServerLevel target = space.getServer().getLevel(targetKey);
        if (target == null || target == space) return;

        SavedPosition savedPosition = savedPlanetPosition(player, planet.dimension());
        Vec3 targetPosition = savedPosition != null ? savedPosition.position() : resolveArrival(target, planet.id());
        float targetYRot = savedPosition != null ? savedPosition.yRot() : player.getYRot();
        float targetXRot = savedPosition != null ? savedPosition.xRot() : player.getXRot();
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
                targetYRot, targetXRot);

        if (travellingWithPod) {
            SpacePodEntity newPod = new SpacePodEntity(MainEntities.SPACE_POD.get(), target);
            newPod.setOpenNave(false);
            newPod.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
            newPod.setYRot(targetYRot);
            newPod.setDeltaMovement(Vec3.ZERO);
            if (target.addFreshEntity(newPod)) player.startRiding(newPod);
        }
    }

    private void positionAtSavedPlanetReturn(ServerPlayer player) {
        ResourceLocation dimension = pendingPlanetEntries.remove(player.getUUID());
        if (dimension == null) return;
        SavedPosition saved = savedPlanetPosition(player, dimension);
        if (saved == null) return;

        if (player.getVehicle() instanceof SpacePodEntity pod) {
            movePodAndPassenger(pod, player, saved.position(), saved.yRot(), saved.xRot());
        } else {
            player.teleportTo(saved.position().x, saved.position().y, saved.position().z);
            player.setYRot(saved.yRot());
            player.setXRot(saved.xRot());
            player.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void movePodAndPassenger(SpacePodEntity pod, ServerPlayer player, Vec3 position,
                                            float yRot, float xRot) {
        pod.setOpenNave(false);
        pod.setPos(position.x, position.y, position.z);
        pod.setYRot(yRot);
        pod.setDeltaMovement(Vec3.ZERO);
        player.setPos(position.x, position.y, position.z);
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.setDeltaMovement(Vec3.ZERO);
        if (!player.isPassenger()) player.startRiding(pod);
    }

    public static void rememberPlanetPosition(ServerPlayer player) {
        if (player.level().dimension().equals(SpaceDimension.KEY)) return;
        CompoundTag positions = player.getPersistentData().getCompound(LAST_PLANET_POSITIONS_TAG);
        CompoundTag position = new CompoundTag();
        position.putDouble("x", player.getX());
        position.putDouble("y", player.getY());
        position.putDouble("z", player.getZ());
        position.putFloat("yRot", player.getYRot());
        position.putFloat("xRot", player.getXRot());
        positions.put(player.level().dimension().location().toString(), position);
        player.getPersistentData().put(LAST_PLANET_POSITIONS_TAG, positions);
    }

    public static SavedPosition savedPlanetPosition(ServerPlayer player, ResourceLocation dimension) {
        CompoundTag positions = player.getPersistentData().getCompound(LAST_PLANET_POSITIONS_TAG);
        if (!positions.contains(dimension.toString(), Tag.TAG_COMPOUND)) return null;
        CompoundTag position = positions.getCompound(dimension.toString());
        double x = position.getDouble("x");
        double y = position.getDouble("y");
        double z = position.getDouble("z");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return null;
        return new SavedPosition(new Vec3(x, y, z), position.getFloat("yRot"), position.getFloat("xRot"));
    }
    public static Vec3 resolveArrival(ServerLevel target, String planetId) {
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
        pendingPlanetEntries.remove(playerId);
        recentlyRidingPod.remove(playerId);
        previousTravelPositions.remove(playerId);
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

    private record SpaceEntry(ResourceLocation sourceDimension, boolean withPod) {
    }

    public record SavedPosition(Vec3 position, float yRot, float xRot) {
    }

    private record FlightAbilities(boolean mayfly, boolean flying) {
    }
}
