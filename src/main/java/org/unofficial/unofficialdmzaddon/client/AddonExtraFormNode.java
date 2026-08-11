package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.CategoryNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import org.unofficial.unofficialdmzaddon.mixin.RadialFormsAccessor;

import java.util.List;

/** DragonMineZ's Extra Forms category without the dedicated custom-form type. */
public final class AddonExtraFormNode extends CategoryNode {
    public AddonExtraFormNode() {
        super(Component.translatable("gui.dragonminez.radial.extraforms"), icon("godforms"));
    }

    @Override
    protected List<RadialNode> buildChildren(StatsData stats) {
        return RadialFormsAccessor.unofficialdmzaddon$forms(stats, "moreforms", type ->
                !type.contains("super")
                        && !type.contains("legendary")
                        && !type.contains("android")
                        && !type.contains("god")
                        && !type.contains("custom"));
    }
}
