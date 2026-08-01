package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.RadialForms;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.function.Predicate;

@Mixin(value = RadialForms.class, remap = false)
public interface RadialFormsAccessor {
    @Invoker("forms")
    static List<RadialNode> unofficialdmzaddon$forms(StatsData stats, String category, Predicate<String> filter) {
        throw new AssertionError();
    }
}
