package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.network.C2S.TravelToPlanetC2S;
import com.dragonminez.common.spacepod.SpacePodDestinationDefinition;
import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.unofficial.unofficialdmzaddon.space.SpaceDimension;
import org.unofficial.unofficialdmzaddon.space.SpaceEnvironmentHandler;

import java.util.function.Supplier;

/** Makes DMZ pod travel atomic so source state is captured before the pod is removed. */
@Mixin(value = TravelToPlanetC2S.class, remap = false)
public abstract class SpacePodTravelMixin {
    @Shadow @Final private String destinationId;

    /**
     * @author Unofficial DMZ Addon
     * @reason DMZ discards the source pod before dimension events can identify pod travel.
     */
    @Overwrite
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            boolean dead = StatsProvider.get(StatsCapability.INSTANCE, player)
                    .map(data -> !data.getStatus().isAlive()).orElse(false);
            if (dead) return;

            ServerLevel sourceLevel = player.serverLevel();
            SpacePodDestinationDefinition destination = SpacePodDestinationRegistry.getServerDestination(destinationId);
            if (destination == null) return;
            ResourceLocation targetId = ResourceLocation.tryParse(destination.dimension());
            if (targetId == null) return;
            ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, targetId);
            boolean travellingToSpace = targetKey.equals(SpaceDimension.KEY);
            if (travellingToSpace) {
                boolean hasFlySkill = StatsProvider.get(StatsCapability.INSTANCE, player)
                        .map(data -> data.getSkills().getSkillLevel("fly") > 0)
                        .orElse(false);
                if (!org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_ENABLED.get()
                        || (org.unofficial.unofficialdmzaddon.UnofficialDMZConfig.SPACE_REQUIRE_FLY.get()
                        && !hasFlySkill)) return;

                // The scoreboard tag used by the menu can briefly be stale after a return trip.
                // Authorize repeated travel from the authoritative DMZ skill and refresh the tag.
                player.addTag(SpaceEnvironmentHandler.FLY_UNLOCK_TAG);
            } else if (!destination.unlockRules().test(player)
                    || SpaceEnvironmentHandler.isPlanetDestroyed(player, targetId)) {
                return;
            }
            ServerLevel targetLevel = player.server.getLevel(targetKey);
            if (targetLevel == null || targetLevel == sourceLevel) return;

            Entity sourceVehicle = player.getVehicle();
            boolean travellingWithPod = sourceVehicle instanceof SpacePodEntity;
            Vec3 targetPosition;
            float targetYRot = player.getYRot();
            float targetXRot = player.getXRot();

            if (travellingToSpace) {
                SpaceEnvironmentHandler.rememberPlanetPosition(player);
                targetPosition = SpaceEnvironmentHandler.spaceEntryPosition(player, sourceLevel.dimension().location());
            } else if (SpaceDimension.isSpace(sourceLevel.dimension())) {
                SpaceEnvironmentHandler.SavedPosition saved =
                        SpaceEnvironmentHandler.savedPlanetPosition(player, targetId);
                if (saved != null) {
                    targetPosition = saved.position();
                    targetYRot = saved.yRot();
                    targetXRot = saved.xRot();
                } else {
                    Vec3 requested = destination.resolvePosition(player.position());
                    targetPosition = SpaceEnvironmentHandler.resolveArrival(targetLevel, targetId.getPath());
                    if (destination.x() != null || destination.y() != null || destination.z() != null) {
                        targetPosition = requested;
                    }
                }
            } else {
                targetPosition = destination.resolvePosition(player.position());
            }

            if (travellingToSpace) {
                // Queue the handoff while the source dimension and pod are still authoritative.
                // Forge's dimension events run after DMZ begins dismantling the source vehicle.
                SpaceEnvironmentHandler.queueSpaceEntry(player,
                        sourceLevel.dimension().location(), travellingWithPod);
            }
            player.stopRiding();
            if (sourceVehicle instanceof SpacePodEntity sourcePod) sourcePod.discard();
            player.teleportTo(targetLevel, targetPosition.x, targetPosition.y, targetPosition.z,
                    targetYRot, targetXRot);
            player.setDeltaMovement(Vec3.ZERO);

            if (travellingWithPod) {
                SpacePodEntity destinationPod = new SpacePodEntity(MainEntities.SPACE_POD.get(), targetLevel);
                destinationPod.setOpenNave(false);
                destinationPod.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
                destinationPod.setYRot(targetYRot);
                destinationPod.setDeltaMovement(Vec3.ZERO);
                if (targetLevel.addFreshEntity(destinationPod)) player.startRiding(destinationPod);
            }
        });
        context.get().setPacketHandled(true);
    }
}
