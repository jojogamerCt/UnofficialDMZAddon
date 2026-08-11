package org.unofficial.unofficialdmzaddon.space;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
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
    private static final String SPACE_MIGRATION_VERSION_TAG = "unofficialdmzaddonSpaceMigrationVersion";
    private static final String PENDING_SPACE_ENTRY_TAG = "unofficialdmzaddonPendingSpaceEntry";
    private static final String PENDING_SPACE_SOURCE_TAG = "SourceDimension";
    private static final String PENDING_SPACE_WITH_POD_TAG = "WithPod";
    private static final int CURRENT_SPACE_MIGRATION_VERSION = 1;

    private final Set<UUID> weightlessPlayers = new HashSet<>();
    private final Map<UUID, FlightAbilities> previousFlightAbilities = new HashMap<>();
    private static final Map<UUID, long[]> DESTROYED_PLANETS = new HashMap<>();
    private final Map<UUID, Long> lastTravel = new HashMap<>();
    private static final Map<UUID, SpaceEntry> PENDING_SPACE_ENTRIES = new HashMap<>();
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

        if (!SpaceDimension.isSpace(player.level().dimension())) {
            positionAtSavedPlanetReturn(player);
            rememberPlanetPosition(player);
            recentlyRidingPod.put(player.getUUID(), player.getVehicle() instanceof SpacePodEntity);
        }

        UUID playerId = player.getUUID();
        if (SpaceDimension.isSpace(player.level().dimension())
                && org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_ENABLED.get()) {
            positionNearSourcePlanet(player);
            if (migrateLegacySpacePlayer(player)) return;
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
                || !SpaceDimension.isSpace(level.dimension())) return;

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
                enforceSpaceBoundary(level, pod);
            } else if (entity instanceof AbstractKiProjectile projectile) {
                projectiles.add(projectile);
            }
        }

        for (ServerPlayer player : List.copyOf(level.players())) {
            if (!(player.getVehicle() instanceof SpacePodEntity)) {
                enforceSpaceBoundary(level, player);
            }
            processPlanets(level, player, projectiles, gameTime);
        }
    }

    /** Server-authoritative hard boundaries. Solar space has only floor/roof; deeper domains also have X/Z walls. */
    private static void enforceSpaceBoundary(ServerLevel level, Entity entity) {
        SpaceCelestialSystem.SpaceDomain domain = SpaceDimension.domain(level.dimension());
        Vec3 center = SpaceCelestialSystem.center(domain);
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        double safeX = x;
        double safeY = y;
        double safeZ = z;

        boolean verticalBoundaries = org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_VERTICAL_BOUNDARIES.get();
        double floor = org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get();
        double roof = Math.max(floor + 8.0D,
                org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_ROOF_HEIGHT.get());
        if (verticalBoundaries) safeY = Math.max(floor, Math.min(roof, y));

        boolean horizontalBoundaries = org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_DEEP_SPACE_WALLS.get()
                && domain != SpaceCelestialSystem.SpaceDomain.SOLAR_SYSTEM
                && (domain != SpaceCelestialSystem.SpaceDomain.GALAXY
                    || org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_GALAXY_WALL_ENABLED.get())
                && (domain != SpaceCelestialSystem.SpaceDomain.UNIVERSE_7
                    || org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_UNIVERSE_WALL_ENABLED.get());
        if (horizontalBoundaries) {
            double distance = domain == SpaceCelestialSystem.SpaceDomain.GALAXY
                    ? org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_GALAXY_WALL_RADIUS.get()
                    : org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_UNIVERSE_WALL_RADIUS.get();
            double inset = Math.max(1.0D, entity.getBbWidth() * 0.5D + 0.05D);
            safeX = Math.max(center.x - distance + inset, Math.min(center.x + distance - inset, x));
            safeZ = Math.max(center.z - distance + inset, Math.min(center.z + distance - inset, z));
        }

        if (safeX == x && safeY == y && safeZ == z) return;
        entity.teleportTo(safeX, safeY, safeZ);
        Vec3 movement = entity.getDeltaMovement();
        double motionX = safeX != x && Math.signum(movement.x) == Math.signum(x - safeX) ? 0.0D : movement.x;
        double motionY = safeY != y && Math.signum(movement.y) == Math.signum(y - safeY) ? 0.0D : movement.y;
        double motionZ = safeZ != z && Math.signum(movement.z) == Math.signum(z - safeZ) ? 0.0D : movement.z;
        entity.setDeltaMovement(motionX, motionY, motionZ);
        entity.hurtMarked = true;
        entity.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getDimension().equals(SpaceDimension.KEY)
                && !SpaceDimension.isSpace(player.level().dimension())) {
            rememberPlanetPosition(player);
            recentlyRidingPod.put(player.getUUID(), player.getVehicle() instanceof SpacePodEntity);
        }
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            boolean fromSpace = SpaceDimension.isSpace(event.getFrom());
            boolean toSpace = SpaceDimension.isSpace(event.getTo());
            if (event.getTo().equals(SpaceDimension.KEY) && !fromSpace) {
                SpaceEntry fallback = new SpaceEntry(event.getFrom().location(),
                        recentlyRidingPod.getOrDefault(player.getUUID(), false));
                SpaceEntry entry = PENDING_SPACE_ENTRIES.putIfAbsent(player.getUUID(), fallback);
                if (entry == null) {
                    writePendingSpaceEntry(player, fallback);
                }
            } else if (fromSpace && !toSpace) {
                pendingPlanetEntries.put(player.getUUID(), event.getTo().location());
            }
        }
    }

    /**
     * Records an authoritative per-trip handoff before DragonMineZ removes the source pod. The NBT
     * copy survives dimension-event ordering and a disconnect during the loading screen.
     */
    public static void queueSpaceEntry(ServerPlayer player, ResourceLocation sourceDimension, boolean withPod) {
        SpaceEntry entry = new SpaceEntry(sourceDimension, withPod);
        PENDING_SPACE_ENTRIES.put(player.getUUID(), entry);
        writePendingSpaceEntry(player, entry);
    }

    private static void writePendingSpaceEntry(ServerPlayer player, SpaceEntry entry) {
        CompoundTag pending = new CompoundTag();
        pending.putString(PENDING_SPACE_SOURCE_TAG, entry.sourceDimension().toString());
        pending.putBoolean(PENDING_SPACE_WITH_POD_TAG, entry.withPod());
        player.getPersistentData().put(PENDING_SPACE_ENTRY_TAG, pending);
    }

    private static SpaceEntry readPendingSpaceEntry(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(PENDING_SPACE_ENTRY_TAG, Tag.TAG_COMPOUND)) return null;
        CompoundTag pending = persistent.getCompound(PENDING_SPACE_ENTRY_TAG);
        ResourceLocation sourceDimension = ResourceLocation.tryParse(
                pending.getString(PENDING_SPACE_SOURCE_TAG));
        return sourceDimension == null ? null : new SpaceEntry(sourceDimension,
                pending.getBoolean(PENDING_SPACE_WITH_POD_TAG));
    }

    private static void clearPendingSpaceEntry(ServerPlayer player) {
        PENDING_SPACE_ENTRIES.remove(player.getUUID());
        player.getPersistentData().remove(PENDING_SPACE_ENTRY_TAG);
    }

    public static Vec3 spaceEntryPosition(ServerPlayer player, ResourceLocation sourceDimension) {
        SpacePlanetSystem.PlanetPlacement sourcePlanet = SpacePlanetSystem.layout(Vec3.ZERO,
                        player.serverLevel().getGameTime())
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
        SpaceEntry entry = PENDING_SPACE_ENTRIES.get(player.getUUID());
        if (entry == null) {
            entry = readPendingSpaceEntry(player);
            if (entry != null) PENDING_SPACE_ENTRIES.put(player.getUUID(), entry);
        }
        if (entry == null) return;
        ResourceLocation sourceDimension = entry.sourceDimension();

        SpacePlanetSystem.PlanetPlacement sourcePlanet = SpacePlanetSystem.layout(Vec3.ZERO,
                        player.serverLevel().getGameTime())
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
        if (player.position().distanceToSqr(entryPosition) > 4.0D) return;

        // Consume the handoff only after the target position is authoritative. Until then the
        // persistent marker lets the next server tick retry instead of leaving the player in void.
        clearPendingSpaceEntry(player);
        player.getPersistentData().putString("unofficialdmzaddonSpaceDomain",
                SpaceCelestialSystem.SpaceDomain.SOLAR_SYSTEM.name());
        player.getPersistentData().putInt(SPACE_MIGRATION_VERSION_TAG, CURRENT_SPACE_MIGRATION_VERSION);
        lastTravel.put(player.getUUID(), player.serverLevel().getGameTime());
        previousTravelPositions.put(player.getUUID(), entryPosition);
    }

    /**
     * Worlds saved before the hierarchy used real dimensions can contain players at obsolete
     * coordinates or in a newly-created blank child dimension. Migrate them once to Earth orbit.
     */
    private boolean migrateLegacySpacePlayer(ServerPlayer player) {
        if (player.getPersistentData().getInt(SPACE_MIGRATION_VERSION_TAG)
                >= CURRENT_SPACE_MIGRATION_VERSION) return false;

        ServerLevel solarSpace = player.server.getLevel(SpaceDimension.KEY);
        if (solarSpace == null) return false;
        Vec3 earthArrival = earthOrbitPosition(solarSpace);
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        Entity sourceVehicle = player.getVehicle();
        boolean travellingWithPod = sourceVehicle instanceof SpacePodEntity;

        if (player.serverLevel() == solarSpace) {
            if (sourceVehicle instanceof SpacePodEntity pod) {
                movePodAndPassenger(pod, player, earthArrival, yRot, xRot);
            } else {
                player.teleportTo(earthArrival.x, earthArrival.y, earthArrival.z);
                player.setDeltaMovement(Vec3.ZERO);
            }
        } else {
            player.stopRiding();
            if (sourceVehicle instanceof SpacePodEntity sourcePod) sourcePod.discard();
            player.teleportTo(solarSpace, earthArrival.x, earthArrival.y, earthArrival.z, yRot, xRot);
            player.setDeltaMovement(Vec3.ZERO);
            if (travellingWithPod) {
                SpacePodEntity destinationPod = new SpacePodEntity(MainEntities.SPACE_POD.get(), solarSpace);
                destinationPod.setOpenNave(false);
                destinationPod.setPos(earthArrival.x, earthArrival.y, earthArrival.z);
                destinationPod.setYRot(yRot);
                destinationPod.setDeltaMovement(Vec3.ZERO);
                if (solarSpace.addFreshEntity(destinationPod)) player.startRiding(destinationPod);
            }
        }

        player.getPersistentData().putInt(SPACE_MIGRATION_VERSION_TAG, CURRENT_SPACE_MIGRATION_VERSION);
        player.getPersistentData().putString("unofficialdmzaddonSpaceDomain",
                SpaceCelestialSystem.SpaceDomain.SOLAR_SYSTEM.name());
        lastTravel.put(player.getUUID(), solarSpace.getGameTime());
        previousTravelPositions.put(player.getUUID(), earthArrival);
        return true;
    }

    private static Vec3 earthOrbitPosition(ServerLevel solarSpace) {
        SpacePlanetSystem.PlanetPlacement earth = SpacePlanetSystem.layout(Vec3.ZERO,
                        solarSpace.getGameTime()).stream()
                .filter(placement -> "earth".equals(placement.definition().id()))
                .findFirst().orElse(null);
        if (earth == null) {
            return SpaceCelestialSystem.SOLAR_CENTER.add(0.0D,
                    SpacePlanetSystem.SUN_RADIUS + 20.0D, 0.0D);
        }
        double clearance = earth.definition().radius()
                + org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_SPAWN_CLEARANCE.get();
        Vec3 position = earth.position().add(0.0D, clearance, 0.0D);
        return new Vec3(position.x,
                Math.max(org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_FLOOR_HEIGHT.get(),
                        position.y), position.z);
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
        long[] destroyed = DESTROYED_PLANETS.computeIfAbsent(playerId, ignored -> freshDestroyedArray());
        Vec3 travelPosition = player.getVehicle() instanceof SpacePodEntity pod ? pod.position() : player.position();
        Vec3 previousTravelPosition = previousTravelPositions.getOrDefault(playerId, travelPosition);
        if (SpaceDimension.isSolarSpace(space.dimension())) {
            Entity traveller = player.getVehicle() instanceof SpacePodEntity pod ? pod : player;
            double collisionMargin = Math.max(0.5D,
                    Math.max(traveller.getBbWidth(), traveller.getBbHeight()) * 0.5D);
            if (SpacePlanetSystem.segmentIntersectsCube(previousTravelPosition, travelPosition,
                    SpacePlanetSystem.sunPosition(Vec3.ZERO),
                    SpacePlanetSystem.SUN_RADIUS + collisionMargin)) {
                // ServerPlayer.kill() bypasses Creative invulnerability, matching /kill semantics.
                if (player.isAlive()) player.kill();
                previousTravelPositions.remove(playerId);
                return;
            }
        }
        if (processCelestialTravel(space, player, previousTravelPosition, travelPosition, gameTime)) return;
        if (!SpaceDimension.isSolarSpace(space.dimension())) {
            previousTravelPositions.put(playerId, travelPosition);
            return;
        }
        List<SpacePlanetSystem.PlanetPlacement> placements = SpacePlanetSystem.layout(travelPosition, gameTime);

        for (SpacePlanetSystem.PlanetPlacement placement : placements) {
            int index = placement.index();
            if (destroyed[index] >= 0L && gameTime - destroyed[index] >= SpacePlanetSystem.RESPAWN_TICKS) {
                destroyed[index] = -1L;
            }
            // Physical planets obey the same progression rules as DragonMineZ's destination menu.
            // Locked worlds remain visible to the client, but cannot be destroyed or entered.
            if (!isPlanetUnlocked(player, placement.definition())) continue;
            if (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_PLANET_DESTRUCTION.get()
                    && !SpacePlanetSystem.isDestroyed(gameTime, destroyed[index])) {
                for (AbstractKiProjectile projectile : projectiles) {
                    if (!playerId.equals(projectile.getOwnerUUID())) continue;
                    if (!projectile.isFiring() || projectile.tickCount < 2) continue;
                    Vec3 previous = projectile.position().subtract(projectile.getDeltaMovement());
                    if (SpacePlanetSystem.segmentIntersectsRotatedCube(previous, projectile.position(),
                            placement.position(), placement.definition().radius(), placement.spinAngle())) {
                        destroyed[index] = gameTime;
                        putOnEvilPath(player);
                        break;
                    }
                }
            }

            if (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_PLANET_TRAVEL.get()
                    && !SpacePlanetSystem.isDestroyed(gameTime, destroyed[index])
                    && SpacePlanetSystem.segmentIntersectsRotatedCube(previousTravelPosition, travelPosition,
                    placement.position(), placement.definition().radius() + org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_PLANET_ENTRY_MARGIN.get(),
                    placement.spinAngle())
                    && gameTime - lastTravel.getOrDefault(playerId, Long.MIN_VALUE / 2L) > TRAVEL_COOLDOWN_TICKS) {
                travelToPlanet(space, player, placement.definition());
                lastTravel.put(playerId, gameTime);
                return;
            }
        }
        previousTravelPositions.put(playerId, travelPosition);
    }

    private boolean processCelestialTravel(ServerLevel sourceLevel, ServerPlayer player,
                                           Vec3 previousPosition, Vec3 travelPosition, long gameTime) {
        if (!org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_CELESTIAL_TRAVEL.get()
                || gameTime - lastTravel.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2L)
                <= TRAVEL_COOLDOWN_TICKS) return false;
        SpaceCelestialSystem.SpaceDomain destination = SpaceCelestialSystem.travelTarget(
                sourceLevel.dimension(), previousPosition, travelPosition);
        if (destination == null) return false;
        ServerLevel targetLevel = sourceLevel.getServer().getLevel(SpaceDimension.key(destination));
        if (targetLevel == null || targetLevel == sourceLevel) return false;

        Vec3 arrival = destination == SpaceCelestialSystem.SpaceDomain.SOLAR_SYSTEM
                ? earthOrbitPosition(targetLevel)
                : SpaceCelestialSystem.center(destination);
        Entity sourceVehicle = player.getVehicle();
        boolean travellingWithPod = sourceVehicle instanceof SpacePodEntity;
        player.stopRiding();
        if (sourceVehicle instanceof SpacePodEntity sourcePod) sourcePod.discard();
        player.teleportTo(targetLevel, arrival.x, arrival.y, arrival.z,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        if (travellingWithPod) {
            SpacePodEntity destinationPod = new SpacePodEntity(MainEntities.SPACE_POD.get(), targetLevel);
            destinationPod.setOpenNave(false);
            destinationPod.setPos(arrival.x, arrival.y, arrival.z);
            destinationPod.setYRot(player.getYRot());
            destinationPod.setDeltaMovement(Vec3.ZERO);
            if (targetLevel.addFreshEntity(destinationPod)) player.startRiding(destinationPod);
        }
        player.getPersistentData().putString("unofficialdmzaddonSpaceDomain", destination.name());
        lastTravel.put(player.getUUID(), gameTime);
        previousTravelPositions.put(player.getUUID(), arrival);
        return true;
    }

    private static boolean isPlanetUnlocked(ServerPlayer player, SpacePlanetDefinition planet) {
        String dimension = planet.dimension().toString();
        return SpacePodDestinationRegistry.getServerDestinations().stream()
                .filter(destination -> destination.dimension().equals(dimension))
                .findFirst()
                .map(destination -> destination.unlockRules().test(player))
                .orElse(true);
    }

    /** Server-authoritative destination guard shared with the native space-pod packet handler. */
    public static boolean isPlanetDestroyed(ServerPlayer player, ResourceLocation dimension) {
        long[] destroyed = DESTROYED_PLANETS.get(player.getUUID());
        if (destroyed == null) return false;
        long gameTime = player.server.overworld().getGameTime();
        for (int index = 0; index < SpacePlanetSystem.PLANETS.size(); index++) {
            if (!SpacePlanetSystem.PLANETS.get(index).dimension().equals(dimension)) continue;
            if (destroyed[index] >= 0L
                    && gameTime - destroyed[index] >= SpacePlanetSystem.RESPAWN_TICKS) {
                destroyed[index] = -1L;
                return false;
            }
            return SpacePlanetSystem.isDestroyed(gameTime, destroyed[index]);
        }
        return false;
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
        if (SpaceDimension.isSpace(player.level().dimension())) return;
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
        DESTROYED_PLANETS.remove(playerId);
        lastTravel.remove(playerId);
        PENDING_SPACE_ENTRIES.remove(playerId);
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
