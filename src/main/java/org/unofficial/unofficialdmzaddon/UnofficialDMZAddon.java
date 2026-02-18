package org.unofficial.unofficialdmzaddon;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.unofficial.unofficialdmzaddon.dmz.UltraInstinctOmenCombatHandler;
import org.unofficial.unofficialdmzaddon.dmz.UltraInstinctOmenInstaller;

@Mod(UnofficialDMZAddon.MODID)
public final class UnofficialDMZAddon {

    public static final String MODID = "unofficialdmzaddon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UnofficialDMZAddon() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(new UltraInstinctOmenCombatHandler());
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(UltraInstinctOmenInstaller::install);
    }
}
