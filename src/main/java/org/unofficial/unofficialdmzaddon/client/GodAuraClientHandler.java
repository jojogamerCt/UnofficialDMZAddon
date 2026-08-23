package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.particles.AuraParticle;
import com.dragonminez.common.init.particles.DivineParticle;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import org.unofficial.unofficialdmzaddon.dmz.DivineAuraHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, value = Dist.CLIENT)
public final class GodAuraClientHandler {
    private GodAuraClientHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        level.players().forEach(player -> StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (AddonAuraPolicy.shouldShowAddonAura(data)
                    && DivineAuraHelper.isUltraInstinct(data)
                    && !DivineAuraHelper.isTrueUltraInstinct(data)) {
                spawnUltraInstinctHeatHaze(player, data);
            }
        }));
    }

    private static void spawnUltraInstinctHeatHaze(Player player, StatsData data) {
        if (player.getRandom().nextFloat() > 0.58F) return;
        boolean mastered = data.getCharacter().getActiveForm() != null
                && data.getCharacter().getActiveForm().toLowerCase().contains("mastered");
        int color = mastered ? 0xEAFBFF : 0xC8D8DF;
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;
        double radius = 0.28 + player.getRandom().nextDouble() * 0.42;
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
        double x = player.getX() + Math.cos(angle) * radius;
        double z = player.getZ() + Math.sin(angle) * radius;
        double y = player.getY() + 0.1D + player.getRandom().nextDouble() * player.getBbHeight();
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(
                MainParticles.DIVINE.get(), x, y, z, r, g, b);
        if (particle instanceof DivineParticle divine) {
            divine.resize(mastered ? 0.58F : 0.45F);
            divine.setParticleSpeed(Math.cos(angle) * 0.003D,
                    0.018D + player.getRandom().nextDouble() * 0.014D, Math.sin(angle) * 0.003D);
        }
    }

    public static void spawnUltraEgoBurst(UUID playerId) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        Player player = level.getPlayerByUUID(playerId);
        if (player == null) return;
        for (int i = 0; i < 36; i++) {
            double angle = Math.PI * 2.0D * i / 36.0D + player.getRandom().nextDouble() * 0.18D;
            double height = player.getRandom().nextDouble() * player.getBbHeight();
            double radius = 0.18D + player.getRandom().nextDouble() * 0.32D;
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + height;
            double z = player.getZ() + Math.sin(angle) * radius;
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(
                    i % 3 == 0 ? MainParticles.DIVINE.get() : MainParticles.AURA.get(),
                    x, y, z, 0.94D, 0.22D, 1.0D);
            double outward = 0.055D + player.getRandom().nextDouble() * 0.07D;
            if (particle instanceof DivineParticle divine) {
                divine.resize(1.25F);
                divine.setParticleSpeed(Math.cos(angle) * outward, 0.05D, Math.sin(angle) * outward);
            } else if (particle instanceof AuraParticle aura) {
                aura.resize(1.55F);
                aura.setParticleSpeed(Math.cos(angle) * outward,
                        0.04D + player.getRandom().nextDouble() * 0.08D, Math.sin(angle) * outward);
            }
        }
    }
}
