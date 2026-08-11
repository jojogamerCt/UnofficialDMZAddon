package org.unofficial.unofficialdmzaddon.technique;

import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.MainDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/** A widening, triangular Ki cannon whose rendered length is its real travelled distance. */
public final class TriBeamEntity extends Entity {
    private static final EntityDataAccessor<Float> ORIGIN_X = SynchedEntityData.defineId(TriBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORIGIN_Y = SynchedEntityData.defineId(TriBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORIGIN_Z = SynchedEntityData.defineId(TriBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(TriBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(TriBeamEntity.class, EntityDataSerializers.INT);
    private UUID ownerUuid;

    public TriBeamEntity(EntityType<? extends TriBeamEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ORIGIN_X, 0.0F);
        entityData.define(ORIGIN_Y, 0.0F);
        entityData.define(ORIGIN_Z, 0.0F);
        entityData.define(DAMAGE, 1.0F);
        entityData.define(OWNER_ID, -1);
    }

    public void setup(LivingEntity owner, Vec3 origin, Vec3 velocity, float damage) {
        ownerUuid = owner.getUUID();
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(ORIGIN_X, (float) origin.x);
        entityData.set(ORIGIN_Y, (float) origin.y);
        entityData.set(ORIGIN_Z, (float) origin.z);
        entityData.set(DAMAGE, damage);
        setPos(origin);
        setDeltaMovement(velocity);
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
    }

    public Vec3 getOrigin() {
        return new Vec3(entityData.get(ORIGIN_X), entityData.get(ORIGIN_Y), entityData.get(ORIGIN_Z));
    }

    public float getVisualWidth() {
        return Mth.clamp(0.55F + (float) getOrigin().distanceTo(position()) * 0.028F, 0.55F, 2.35F);
    }

    private Entity owner() {
        Entity byId = level().getEntity(entityData.get(OWNER_ID));
        if (byId != null) return byId;
        return level() instanceof ServerLevel server && ownerUuid != null ? server.getEntity(ownerUuid) : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount > 80 || getOrigin().distanceToSqr(position()) > 128.0D * 128.0D) {
            discard();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, candidate -> {
            Entity resolved = TargetHelper.resolveHittable(candidate);
            Entity owner = owner();
            if (!(resolved instanceof LivingEntity living) || !living.isAlive() || resolved == owner) return false;
            return !(owner instanceof Player player) || TargetHelper.canAttack(player, resolved, 128.0D);
        });
        if (hit.getType() != HitResult.Type.MISS) {
            if (hit instanceof EntityHitResult entityHit) {
                Entity target = TargetHelper.resolveHittable(entityHit.getEntity());
                Entity owner = owner();
                target.hurt(MainDamageTypes.kiblast(level(), this, owner), entityData.get(DAMAGE));
                if (owner instanceof Player player) TargetHelper.onSuccessfulAttack(player, target, TargetHelper.getRelation(player, target));
            }
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
            return;
        }
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 18; i++) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        getX(), getY(), getZ(),
                        (random.nextDouble() - 0.5D) * 0.45D,
                        (random.nextDouble() - 0.5D) * 0.45D,
                        (random.nextDouble() - 0.5D) * 0.45D);
            }
        } else super.handleEntityEvent(id);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
        entityData.set(ORIGIN_X, tag.getFloat("OriginX"));
        entityData.set(ORIGIN_Y, tag.getFloat("OriginY"));
        entityData.set(ORIGIN_Z, tag.getFloat("OriginZ"));
        entityData.set(DAMAGE, tag.getFloat("Damage"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
        tag.putFloat("OriginX", entityData.get(ORIGIN_X));
        tag.putFloat("OriginY", entityData.get(ORIGIN_Y));
        tag.putFloat("OriginZ", entityData.get(ORIGIN_Z));
        tag.putFloat("Damage", entityData.get(DAMAGE));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
