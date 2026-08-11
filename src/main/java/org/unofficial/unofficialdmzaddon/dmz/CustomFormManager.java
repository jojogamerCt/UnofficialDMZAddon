package org.unofficial.unofficialdmzaddon.dmz;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.network.AddonNetwork;
import org.unofficial.unofficialdmzaddon.network.CustomFormsSyncS2C;
import org.unofficial.unofficialdmzaddon.network.CustomFormSaveResultS2C;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomFormManager {
    private static final String DATA_KEY = "unofficialdmzaddonCustomForms";
    private static final Map<UUID, List<CustomFormDefinition>> INSTALLED_DEFINITIONS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        List<CustomFormDefinition> forms = load(player);
        install(player.getUUID(), forms);
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            ensureDisplaySkill(data);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
        // The owner can edit these definitions; other clients only need them to render that player correctly.
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            List<CustomFormDefinition> onlineForms = load(online);
            install(online.getUUID(), onlineForms);
            AddonNetwork.sendTo(player, new CustomFormsSyncS2C(online.getUUID(), onlineForms));
        }
        AddonNetwork.sendToAll(player.server, new CustomFormsSyncS2C(player.getUUID(), forms));
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        CompoundTag old = event.getOriginal().getPersistentData();
        if (old.contains(DATA_KEY, Tag.TAG_LIST)) {
            event.getEntity().getPersistentData().put(DATA_KEY, old.getList(DATA_KEY, Tag.TAG_COMPOUND).copy());
        }
    }

    public static void saveForm(ServerPlayer player, CustomFormDefinition requested) {
        if (!UnofficialDMZConfig.CUSTOM_FORMS_ENABLED.get()) {
            saveResult(player, false, "", 0, "disabled");
            return;
        }
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            ensureDisplaySkill(data);
            String race = data.getCharacter().getRaceName();
            List<CustomFormDefinition> forms = load(player);
            CustomFormDefinition form = CustomFormDefinition.validated(requested, race);
            int existing = indexOf(forms, form.id());
            if (existing < 0 && forms.size() >= unlockedFormLimit(data)) {
                saveResult(player, false, form.id(), 0, "limit");
                return;
            }
            int previousCost = existing >= 0 ? forms.get(existing).creationCost() : 0;
            int charge = Math.max(0, form.creationCost() - previousCost);
            if (data.getResources().getTrainingPoints() < charge) {
                saveResult(player, false, form.id(), charge, "tp");
                return;
            }
            if (existing >= 0) {
                CustomFormDefinition previous = forms.get(existing);
                if (!previous.race().equalsIgnoreCase(race)) {
                    saveResult(player, false, form.id(), 0, "race");
                    return;
                }
                forms.set(existing, form);
            } else forms.add(form);
            if (charge > 0) data.getResources().removeTrainingPoints(charge);
            persist(player, forms);
            install(player.getUUID(), forms);
            AddonNetwork.sendToAll(player.server, new CustomFormsSyncS2C(player.getUUID(), forms));
            NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
            saveResult(player, true, form.id(), charge, "");
        });
    }

    private static void saveResult(ServerPlayer player, boolean success, String id, int cost, String reason) {
        AddonNetwork.sendTo(player, new CustomFormSaveResultS2C(success, id, cost, reason));
    }

    public static void deleteForm(ServerPlayer player, String id) {
        if (!UnofficialDMZConfig.CUSTOM_FORMS_ENABLED.get()) return;
        List<CustomFormDefinition> forms = load(player);
        if (!forms.removeIf(form -> form.id().equals(id))) return;
        persist(player, forms);
        install(player.getUUID(), forms);
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            String group = CustomFormDefinition.group(player.getUUID());
            if (group.equalsIgnoreCase(data.getCharacter().getActiveFormGroup())
                    && id.equalsIgnoreCase(data.getCharacter().getActiveForm())) {
                data.getCharacter().clearActiveForm(player);
                player.removeEffect(MainEffects.TRANSFORMED.get());
            }
            if (group.equalsIgnoreCase(data.getCharacter().getSelectedFormGroup())
                    && id.equalsIgnoreCase(data.getCharacter().getSelectedForm())) {
                data.getCharacter().setSelectedFormGroup(TransformationsHelper.getGroupWithFirstAvailableForm(data));
                data.getCharacter().setSelectedForm(TransformationsHelper.getFirstAvailableForm(data));
            }
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
        AddonNetwork.sendToAll(player.server, new CustomFormsSyncS2C(player.getUUID(), forms));
    }

    public static void selectForm(ServerPlayer player, String id) {
        if (!UnofficialDMZConfig.CUSTOM_FORMS_ENABLED.get()) return;
        List<CustomFormDefinition> forms = load(player);
        CustomFormDefinition selected = forms.stream().filter(form -> form.id().equals(id)).findFirst().orElse(null);
        if (selected == null) return;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            ensureDisplaySkill(data);
            if (!selected.race().equalsIgnoreCase(data.getCharacter().getRaceName())) return;
            String group = CustomFormDefinition.group(player.getUUID());
            data.getCharacter().setSelectedFormGroup(group);
            data.getCharacter().setSelectedForm(selected.id());
            data.getStatus().setSelectedAction(ActionMode.FORM);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
    }

    public static List<CustomFormDefinition> load(ServerPlayer player) {
        List<CustomFormDefinition> forms = new ArrayList<>();
        ListTag list = player.getPersistentData().getList(DATA_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) forms.add(CustomFormDefinition.load(list.getCompound(i)));
        return forms;
    }

    private static void persist(ServerPlayer player, List<CustomFormDefinition> forms) {
        ListTag list = new ListTag();
        forms.forEach(form -> list.add(form.save()));
        player.getPersistentData().put(DATA_KEY, list);
    }

    public static void install(UUID owner, List<CustomFormDefinition> definitions) {
        List<CustomFormDefinition> installed = List.copyOf(definitions == null ? List.of() : definitions);
        INSTALLED_DEFINITIONS.put(owner, installed);
        String groupName = CustomFormDefinition.group(owner);
        for (Map<String, FormConfig> raceForms : ConfigManager.getAllForms().values()) raceForms.remove(groupName);
        Map<String, List<CustomFormDefinition>> byRace = new LinkedHashMap<>();
        installed.forEach(form -> byRace.computeIfAbsent(form.race().toLowerCase(), ignored -> new ArrayList<>()).add(form));
        byRace.forEach((race, forms) -> {
            Map<String, FormConfig> raceForms = ConfigManager.getAllFormsForRace(race);
            if (raceForms == null) return;
            FormConfig group = new FormConfig();
            group.setConfigVersion(FormConfig.CURRENT_VERSION);
            group.setGroupName(groupName);
            group.setFormType("customforms");
            LinkedHashMap<String, FormConfig.FormData> data = new LinkedHashMap<>();
            forms.forEach(form -> data.put(form.id(), form.toFormData()));
            group.setForms(data);
            raceForms.put(groupName, group);
        });
    }

    public static boolean ownsGroup(UUID player, String group) {
        return group != null && group.equals(CustomFormDefinition.group(player));
    }

    public static Optional<CustomFormDefinition> getActiveDefinition(StatsData data) {
        if (data == null || data.getPlayer() == null || data.getCharacter() == null) return Optional.empty();
        UUID owner = data.getPlayer().getUUID();
        if (!ownsGroup(owner, data.getCharacter().getActiveFormGroup())) return Optional.empty();
        String activeForm = data.getCharacter().getActiveForm();
        if (activeForm == null || activeForm.isEmpty()) return Optional.empty();
        return INSTALLED_DEFINITIONS.getOrDefault(owner, List.of()).stream()
                .filter(form -> form.id().equalsIgnoreCase(activeForm))
                .findFirst();
    }

    public static boolean hasActiveCustomForm(StatsData data) {
        return getActiveDefinition(data).isPresent();
    }

    private static void ensureDisplaySkill(StatsData data) {
        if (!data.getSkills().hasSkill("customforms")) data.getSkills().setSkillLevel("customforms", 0);
    }

    /** Applies the configured base slots and completed-saga milestone progression. */
    public static int unlockedFormLimit(StatsData data) {
        int completedSagas = 0;
        PlayerQuestData progress = data.getPlayerQuestData();
        if (progress != null) {
            for (Saga saga : QuestRegistry.getAllSagas().values()) {
                if (saga.getQuests().isEmpty()) continue;
                boolean complete = saga.getQuests().stream().allMatch(quest -> progress.isQuestCompleted(
                        PlayerQuestData.sagaQuestKey(saga.getId(), quest.getId())));
                if (complete) completedSagas++;
            }
        }
        int unlocked = UnofficialDMZConfig.CUSTOM_FORMS_STARTING_SLOTS.get();
        if (UnofficialDMZConfig.CUSTOM_FORMS_SAGA_SLOT_UNLOCKS.get()) {
            int milestones = completedSagas / UnofficialDMZConfig.CUSTOM_FORMS_SAGAS_PER_SLOT_MILESTONE.get();
            unlocked += milestones * UnofficialDMZConfig.CUSTOM_FORMS_SLOTS_PER_SAGA_MILESTONE.get();
        }
        return Math.min(UnofficialDMZConfig.CUSTOM_FORMS_MAX_PER_PLAYER.get(), unlocked);
    }

    private static int indexOf(List<CustomFormDefinition> forms, String id) {
        for (int i = 0; i < forms.size(); i++) if (forms.get(i).id().equals(id)) return i;
        return -1;
    }
}
