package org.unofficial.unofficialdmzaddon;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.unofficial.unofficialdmzaddon.dmz.AlienCharacterSanitizer;
import org.unofficial.unofficialdmzaddon.dmz.AlienRaceInstaller;
import org.unofficial.unofficialdmzaddon.dmz.TransformationActivationHandler;
import org.unofficial.unofficialdmzaddon.dmz.TransformationInstaller;
import org.unofficial.unofficialdmzaddon.dmz.UltraInstinctCombatHandler;

@Mod(UnofficialDMZAddon.MODID)
public final class UnofficialDMZAddon {

    public static final String MODID = "unofficialdmzaddon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UnofficialDMZAddon() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(new TransformationActivationHandler());
        MinecraftForge.EVENT_BUS.register(new UltraInstinctCombatHandler());
        MinecraftForge.EVENT_BUS.register(new AlienCharacterSanitizer());
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(AlienRaceInstaller::install);
        event.enqueueWork(TransformationInstaller::install);
    }
}
