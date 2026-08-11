package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.CategoryNode;
import com.dragonminez.client.gui.radial.nodes.FormSelectNode;
import com.dragonminez.client.gui.radial.nodes.MoreNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormDefinition;

import java.util.ArrayList;
import java.util.List;

/** A compact owner-only root for player-created forms in the Utility Menu. */
public final class AddonCustomFormsNode extends CategoryNode {
    private static final int RADIAL_LIMIT = 5;

    public AddonCustomFormsNode() {
        super(Component.translatable("gui.unofficialdmzaddon.radial.custom_forms"), CustomFormsIcon.glyph());
    }

    @Override
    protected List<RadialNode> buildChildren(StatsData stats) {
        if (stats == null || stats.getPlayer() == null || stats.getCharacter() == null) return List.of();
        String race = stats.getCharacter().getRaceName();
        String group = CustomFormDefinition.group(stats.getPlayer().getUUID());
        List<RadialNode> all = new ArrayList<>();
        for (CustomFormDefinition form : CustomFormsClientState.forRace(race)) {
            all.add(new FormSelectNode(race, group, form.id(), false));
        }
        if (all.size() <= RADIAL_LIMIT) return all;
        List<RadialNode> visible = new ArrayList<>(all.subList(0, RADIAL_LIMIT - 1));
        visible.add(new MoreNode("customforms", all));
        return visible;
    }
}
