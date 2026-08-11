package org.unofficial.unofficialdmzaddon.technique;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AddonClientEntities {
    private AddonClientEntities() {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AddonEntities.TRI_BEAM.get(), TriBeamRenderer::new);
        event.registerEntityRenderer(AddonEntities.HELLZONE_ORB.get(), HellzoneOrbRenderer::new);
        event.registerEntityRenderer(AddonEntities.ENERGY_BLADE.get(), EnergyBladeRenderer::new);
    }
}
