package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Idempotent saved-player migrations. Add new schema steps instead of resetting old data. */
public final class AddonPlayerMigrationHandler {
    private static final String SCHEMA_KEY = "unofficialdmzaddonMigrationSchema";
    private static final int CURRENT_SCHEMA = 1;

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var persistent = player.getPersistentData();
        if (persistent.getInt(SCHEMA_KEY) >= CURRENT_SCHEMA) return;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if ("saiyan".equalsIgnoreCase(data.getCharacter().getRaceName())) {
                int legacyLevel = data.getSkills().getSkillLevel("superforms");
                int migratedLevel = legacyLevel >= 6 ? 2 : legacyLevel >= 5 ? 1 : 0;
                if (data.getSkills().getSkillLevel(UniversalDivineAndSaiyanInstaller.GOD_FORMS_GROUP) < migratedLevel) {
                    data.getSkills().setSkillLevel(UniversalDivineAndSaiyanInstaller.GOD_FORMS_GROUP, migratedLevel);
                }
            }
            persistent.putInt(SCHEMA_KEY, CURRENT_SCHEMA);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
    }
}