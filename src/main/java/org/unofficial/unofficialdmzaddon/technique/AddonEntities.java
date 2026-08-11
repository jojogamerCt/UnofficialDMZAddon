package org.unofficial.unofficialdmzaddon.technique;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

public final class AddonEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, UnofficialDMZAddon.MODID);

    public static final RegistryObject<EntityType<TriBeamEntity>> TRI_BEAM = ENTITIES.register("tri_beam",
            () -> EntityType.Builder.<TriBeamEntity>of(TriBeamEntity::new, MobCategory.MISC)
                    .sized(0.55F, 0.55F).clientTrackingRange(128).updateInterval(1)
                    .build(UnofficialDMZAddon.MODID + ":tri_beam"));

    public static final RegistryObject<EntityType<EnergyBladeEntity>> ENERGY_BLADE = ENTITIES.register("energy_blade",
            () -> EntityType.Builder.<EnergyBladeEntity>of(EnergyBladeEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).clientTrackingRange(96).updateInterval(1)
                    .build(UnofficialDMZAddon.MODID + ":energy_blade"));

    public static final RegistryObject<EntityType<HellzoneOrbEntity>> HELLZONE_ORB = ENTITIES.register("hellzone_orb",
            () -> EntityType.Builder.<HellzoneOrbEntity>of(HellzoneOrbEntity::new, MobCategory.MISC)
                    .sized(0.45F, 0.45F).clientTrackingRange(128).updateInterval(1)
                    .build(UnofficialDMZAddon.MODID + ":hellzone_orb"));

    private AddonEntities() {}
}
