package org.unofficial.unofficialdmzaddon.technique;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** One staged Hellzone grenade: it hangs in its assigned constellation, then converges. */
public final class HellzoneOrbEntity extends Entity {
    private static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(HellzoneOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Y = SynchedEntityData.defineId(HellzoneOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(HellzoneOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> WAIT = SynchedEntityData.defineId(HellzoneOrbEntity.class, EntityDataSerializers.INT);

    public HellzoneOrbEntity(EntityType<? extends HellzoneOrbEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData() {
        entityData.define(TARGET_X, 0F); entityData.define(TARGET_Y, 0F); entityData.define(TARGET_Z, 0F);
        entityData.define(WAIT, 40);
    }

    public void setup(Vec3 position, Vec3 target, int waitTicks) {
        setPos(position.x, position.y, position.z);
        entityData.set(TARGET_X, (float) target.x); entityData.set(TARGET_Y, (float) target.y); entityData.set(TARGET_Z, (float) target.z);
        entityData.set(WAIT, waitTicks);
    }

    public Vec3 target() { return new Vec3(entityData.get(TARGET_X), entityData.get(TARGET_Y), entityData.get(TARGET_Z)); }

    @Override public void tick() {
        super.tick();
        if (tickCount <= entityData.get(WAIT)) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 delta = target().subtract(position());
        if (delta.lengthSqr() < 0.36D) {
            if (!level().isClientSide) discard();
            return;
        }
        setDeltaMovement(delta.normalize().scale(1.15D));
        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TARGET_X, tag.getFloat("TargetX")); entityData.set(TARGET_Y, tag.getFloat("TargetY"));
        entityData.set(TARGET_Z, tag.getFloat("TargetZ")); entityData.set(WAIT, tag.getInt("Wait"));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("TargetX", entityData.get(TARGET_X)); tag.putFloat("TargetY", entityData.get(TARGET_Y));
        tag.putFloat("TargetZ", entityData.get(TARGET_Z)); tag.putInt("Wait", entityData.get(WAIT));
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
