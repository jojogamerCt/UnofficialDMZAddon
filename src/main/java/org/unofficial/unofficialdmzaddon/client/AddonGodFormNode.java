package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.CategoryNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import org.unofficial.unofficialdmzaddon.mixin.RadialFormsAccessor;

import java.util.List;

public final class AddonGodFormNode extends CategoryNode {
    public AddonGodFormNode() {
        super(Component.translatable("gui.dragonminez.radial.godforms"), icon("godforms"));
    }

    @Override
    protected List<RadialNode> buildChildren(StatsData stats) {
        return RadialFormsAccessor.unofficialdmzaddon$forms(stats, "godforms", type -> type.contains("god"));
    }
}
