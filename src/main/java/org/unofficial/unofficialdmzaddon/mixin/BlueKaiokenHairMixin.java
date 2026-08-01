package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.render.layer.DMZHairLayer;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.lists.StackForms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DMZHairLayer.class, remap = false)
public abstract class BlueKaiokenHairMixin {
    @Redirect(method = "renderHair", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/client/render/layer/DMZSkinLayer;resolveTintForm(Lcom/dragonminez/common/stats/StatsData;)Lcom/dragonminez/common/config/FormConfig$FormData;"))
    private FormConfig.FormData unofficialdmzaddon$preserveBlueHair(StatsData stats) {
        var character = stats.getCharacter();
        if ("godforms".equalsIgnoreCase(character.getActiveFormGroup())
                && "super_saiyan_blue".equalsIgnoreCase(character.getActiveForm())
                && StackForms.GROUP_KAIOKEN.equalsIgnoreCase(character.getActiveStackFormGroup())) {
            return null;
        }
        return DMZSkinLayer.resolveTintForm(stats);
    }
}
