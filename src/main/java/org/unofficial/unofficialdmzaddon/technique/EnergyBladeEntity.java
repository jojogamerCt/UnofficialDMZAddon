package org.unofficial.unofficialdmzaddon.technique;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Short-lived, programmatically rendered Ki blade attached to Spirit Sword Rush. */
public final class EnergyBladeEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(EnergyBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(EnergyBladeEntity.class, EntityDataSerializers.INT);

    public EnergyBladeEntity(EntityType<? extends EnergyBladeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
        entityData.define(LIFE, 36);
    }

    public void setup(LivingEntity owner, int life) {
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(LIFE, life);
        follow(owner);
    }

    private void follow(Entity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize().scale(-0.34D);
        setPos(owner.getEyePosition().add(right).add(0, -0.48D, 0).add(look.scale(0.35D)));
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
    }

    @Override public void tick() {
        super.tick();
        Entity owner = level().getEntity(entityData.get(OWNER_ID));
        if (owner == null || !owner.isAlive() || tickCount >= entityData.get(LIFE)) {
            if (!level().isClientSide) discard();
            return;
        }
        follow(owner);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        entityData.set(LIFE, tag.getInt("Life"));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putInt("Life", entityData.get(LIFE));
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
