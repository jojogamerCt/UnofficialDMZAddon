package org.unofficial.unofficialdmzaddon;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.unofficial.unofficialdmzaddon.dmz.AddonFormCommand;
import org.unofficial.unofficialdmzaddon.dmz.AddonPlayerMigrationHandler;
import org.unofficial.unofficialdmzaddon.dmz.AddonClassInstaller;
import org.unofficial.unofficialdmzaddon.dmz.AlienCharacterSanitizer;
import org.unofficial.unofficialdmzaddon.dmz.AlienRacialPassiveHandler;
import org.unofficial.unofficialdmzaddon.dmz.FormSpecialBuffHandler;
import org.unofficial.unofficialdmzaddon.dmz.LegacyRaceCleanup;
import org.unofficial.unofficialdmzaddon.dmz.UniversalDivineAndSaiyanInstaller;
import org.unofficial.unofficialdmzaddon.dmz.AlienRaceInstaller;
import org.unofficial.unofficialdmzaddon.dmz.TransformationInstaller;
import org.unofficial.unofficialdmzaddon.dmz.UltraInstinctCombatHandler;
import org.unofficial.unofficialdmzaddon.network.AddonNetwork;
import org.unofficial.unofficialdmzaddon.space.SpaceEnvironmentHandler;

@Mod(UnofficialDMZAddon.MODID)
public final class UnofficialDMZAddon {

    public static final String MODID = "unofficialdmzaddon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UnofficialDMZAddon() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);

        // Register the config before anything else reads it
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, UnofficialDMZConfig.SPEC,
                MODID + "-common.toml");

        MinecraftForge.EVENT_BUS.addListener(AddonFormCommand::register);
        MinecraftForge.EVENT_BUS.register(new UltraInstinctCombatHandler());
        MinecraftForge.EVENT_BUS.register(new AlienCharacterSanitizer());
        MinecraftForge.EVENT_BUS.register(new AlienRacialPassiveHandler());
        MinecraftForge.EVENT_BUS.register(new FormSpecialBuffHandler());
        MinecraftForge.EVENT_BUS.register(new AddonPlayerMigrationHandler());
        MinecraftForge.EVENT_BUS.register(new SpaceEnvironmentHandler());
        // PlayerHudRenderer auto-registers via @Mod.EventBusSubscriber(value = Dist.CLIENT)
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(AddonNetwork::register);
        event.enqueueWork(LegacyRaceCleanup::removeLegacyConfigs);
        event.enqueueWork(AlienRaceInstaller::install);
        event.enqueueWork(AddonClassInstaller::install);
        event.enqueueWork(TransformationInstaller::install);
        event.enqueueWork(UniversalDivineAndSaiyanInstaller::install);
    }
}
