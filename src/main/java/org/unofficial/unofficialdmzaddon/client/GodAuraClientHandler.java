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

@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, value = Dist.CLIENT)
public final class GodAuraClientHandler {
    private GodAuraClientHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        level.players().forEach(player -> StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if ("ultraego".equalsIgnoreCase(data.getCharacter().getActiveFormGroup())) {
                spawnDivineSignature(player, data.getCharacter().getActiveFormGroup(), data.getCharacter().getActiveForm());
            }
        }));
    }

    private static void spawnDivineSignature(Player player, String group, String form) {
        if (group == null || player.getRandom().nextFloat() > 0.72f) return;
        boolean ue = "ultraego".equalsIgnoreCase(group);
        if (!ue) return;
        int tier = 2;
        int color = 0xF04CFF;
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;
        double radius = 0.18 + player.getRandom().nextDouble() * 0.45;
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
        double x = player.getX() + Math.cos(angle) * radius;
        double z = player.getZ() + Math.sin(angle) * radius;
        double y = player.getY() + player.getRandom().nextDouble() * player.getBbHeight();
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(
                MainParticles.AURA.get(), x, y, z, r, g, b);
        if (particle instanceof DivineParticle divine) {
            divine.resize(0.85f + tier * 0.18f);
            divine.setParticleSpeed(0, 0.025 + player.getRandom().nextDouble() * 0.025, 0);
        } else if (particle instanceof AuraParticle aura) {
            aura.resize(0.95f + tier * 0.25f);
            aura.setParticleSpeed(Math.cos(angle) * 0.018, 0.045 + player.getRandom().nextDouble() * 0.045,
                    Math.sin(angle) * 0.018);
        }
    }
}