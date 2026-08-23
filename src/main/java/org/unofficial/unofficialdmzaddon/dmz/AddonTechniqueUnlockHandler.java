package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AddonTechniqueUnlockHandler {
    private static final String QUEST_TIEN_TRAINING = "tien_mountain_training";
    private static final String QUEST_KID_BUU = "buu_saga:34";

    private AddonTechniqueUnlockHandler() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AddonTechniqueInstaller.reinstallAfterConfigSync();
        reconcileCompletedProgression(player);
    }

    @SubscribeEvent
    public static void onQuestCompleted(DMZEvent.QuestCompletedEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;
        AddonTechniqueInstaller.reinstallAfterConfigSync();
        String questKey = normalize(event.getQuestKey());
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            boolean changed = switch (questKey) {
                case QUEST_TIEN_TRAINING -> unlock(data, AddonTechniqueInstaller.TRI_BEAM);
                case QUEST_KID_BUU -> unlock(data, AddonTechniqueInstaller.PLANET_BURST);
                default -> false;
            };
            if (changed) sync(player);
        });
    }

    private static void reconcileCompletedProgression(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            PlayerQuestData quests = data.getPlayerQuestData();
            boolean changed = AddonTechniqueInstaller.refreshOwnedTechniques(data);
            if (quests != null) {
                if (quests.isQuestCompleted(QUEST_TIEN_TRAINING)) changed |= unlock(data, AddonTechniqueInstaller.TRI_BEAM);
                if (quests.isQuestCompleted(QUEST_KID_BUU)) changed |= unlock(data, AddonTechniqueInstaller.PLANET_BURST);
            }
            if (changed) sync(player);
        });
    }

    private static boolean unlock(StatsData data, String techniqueId) {
        if (owns(data, techniqueId)) return false;
        TechniqueData clone = cloneTemplate(techniqueId);
        if (clone == null) {
            UnofficialDMZAddon.LOGGER.warn("[Unofficial DMZ Addon] Could not unlock missing Ki technique template '{}'.", techniqueId);
            return false;
        }
        data.getTechniques().unlockTechnique(clone);
        return owns(data, techniqueId);
    }

    private static TechniqueData cloneTemplate(String techniqueId) {
        KiAttackData template = PredefinedTechniques.REGISTRY.get(techniqueId);
        if (template == null) return null;
        KiAttackData clone = new KiAttackData();
        clone.load(template.save());
        return clone;
    }

    private static boolean owns(StatsData data, String techniqueId) {
        return data.getTechniques().getUnlockedTechniques().containsKey(techniqueId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static void sync(ServerPlayer player) {
        NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
    }
}
