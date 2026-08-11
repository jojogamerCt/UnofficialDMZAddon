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
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

import java.util.Locale;

/** Grants quest-linked addon techniques and migrates already-completed progression. */
@Mod.EventBusSubscriber(modid = UnofficialDMZAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AddonTechniqueUnlockHandler {
    private static final String QUEST_TIEN_TRAINING = "tien_mountain_training";
    private static final String QUEST_ANDROID_18 = "android_saga:8";
    private static final String QUEST_VEGETTO = "buu_saga:28";
    private static final String QUEST_KID_BUU = "buu_saga:34";
    private static final String FUSION_SKILL = "fusion";

    private AddonTechniqueUnlockHandler() {
    }

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
                case QUEST_ANDROID_18 -> unlock(data, AddonTechniqueInstaller.SADISTIC_18);
                case QUEST_KID_BUU -> unlock(data, AddonTechniqueInstaller.PLANET_BURST);
                case QUEST_VEGETTO -> hasFusion(data)
                        && unlock(data, AddonTechniqueInstaller.SPIRIT_SWORD_RUSH);
                default -> false;
            };
            if (changed) sync(player);
        });
    }

    /**
     * Fusion may be learned after Buu quest 28. DMZ exposes no skill-purchase event, so a light
     * two-second reconciliation gives the technique immediately without requiring a relog.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 40 != 0) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (owns(data, AddonTechniqueInstaller.SPIRIT_SWORD_RUSH)) return;
            PlayerQuestData quests = data.getPlayerQuestData();
            if (quests != null && quests.isQuestCompleted(QUEST_VEGETTO) && hasFusion(data)
                    && unlock(data, AddonTechniqueInstaller.SPIRIT_SWORD_RUSH)) {
                sync(player);
            }
        });
    }

    private static void reconcileCompletedProgression(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            PlayerQuestData quests = data.getPlayerQuestData();
            if (quests == null) return;

            boolean changed = false;
            changed |= AddonTechniqueInstaller.refreshOwnedTechniques(data);
            if (quests.isQuestCompleted(QUEST_TIEN_TRAINING)) {
                changed |= unlock(data, AddonTechniqueInstaller.TRI_BEAM);
            }
            if (quests.isQuestCompleted(QUEST_ANDROID_18)) {
                changed |= unlock(data, AddonTechniqueInstaller.SADISTIC_18);
            }
            if (quests.isQuestCompleted(QUEST_KID_BUU)) {
                changed |= unlock(data, AddonTechniqueInstaller.PLANET_BURST);
            }
            if (quests.isQuestCompleted(QUEST_VEGETTO) && hasFusion(data)) {
                changed |= unlock(data, AddonTechniqueInstaller.SPIRIT_SWORD_RUSH);
            }

            if (changed) sync(player);
        });
    }

    private static boolean unlock(StatsData data, String techniqueId) {
        if (owns(data, techniqueId)) return false;

        TechniqueData clone = cloneTemplate(techniqueId);
        if (clone == null) {
            UnofficialDMZAddon.LOGGER.warn(
                    "[Unofficial DMZ Addon] Could not unlock missing technique template '{}'.",
                    techniqueId
            );
            return false;
        }

        data.getTechniques().unlockTechnique(clone);
        return owns(data, techniqueId);
    }

    private static TechniqueData cloneTemplate(String techniqueId) {
        KiAttackData kiTemplate = PredefinedTechniques.REGISTRY.get(techniqueId);
        if (kiTemplate != null) {
            KiAttackData clone = new KiAttackData();
            clone.load(kiTemplate.save());
            return clone;
        }

        StrikeAttackData strikeTemplate = PredefinedTechniques.STRIKE_REGISTRY.get(techniqueId);
        if (strikeTemplate != null) {
            StrikeAttackData clone = new StrikeAttackData();
            clone.load(strikeTemplate.save());
            return clone;
        }
        return null;
    }

    private static boolean owns(StatsData data, String techniqueId) {
        return data.getTechniques().getUnlockedTechniques().containsKey(techniqueId);
    }

    private static boolean hasFusion(StatsData data) {
        return data.getSkills().getSkillLevel(FUSION_SKILL) > 0;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static void sync(ServerPlayer player) {
        // One packet per reconciliation, even if multiple legacy unlocks are restored.
        NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
    }
}
