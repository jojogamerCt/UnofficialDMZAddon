package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.dmz.UltraInstinctDefinitions;
import org.unofficial.unofficialdmzaddon.mixin.accessor.SkillsAccessor;

import java.util.Map;

@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataMixin {

    @Inject(method = "updateTransformationSkillLimits", at = @At("TAIL"))
    private void unofficialdmzaddon$ensureUltraInstinctSkill(String raceName, CallbackInfo ci) {
        StatsData self = (StatsData) (Object) this;
        Map<String, Skill> skillMap = ((SkillsAccessor) self.getSkills()).unofficialdmzaddon$getSkillMap();

        String skillId = UltraInstinctDefinitions.SKILL_ID;
        if (!"saiyan".equalsIgnoreCase(raceName)) {
            skillMap.remove(skillId);
            return;
        }

        Skill existing = skillMap.get(skillId);
        int level = existing == null ? 0 : Math.min(existing.getLevel(), UltraInstinctDefinitions.maxSkillLevel());
        boolean active = existing != null && existing.isActive();

        skillMap.put(skillId, new Skill(skillId, level, active, UltraInstinctDefinitions.maxSkillLevel()));
    }
}
