package org.unofficial.unofficialdmzaddon.mixin.accessor;

import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.Skills;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = Skills.class, remap = false)
public interface SkillsAccessor {

    @Accessor("skillMap")
    Map<String, Skill> unofficialdmzaddon$getSkillMap();
}
