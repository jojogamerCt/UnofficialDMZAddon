package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MapItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

/**
 * Forces DMZ's first-person race-model renderer on for all players that have a
 * DMZ character, regardless of DMZ's own "isFirstPersonAnimated" user setting.
 *
 * When this returns {@code true}, DMZ's pipeline takes over:
 *   - {@code MixinInjectFirstPersonRendering} cancels the vanilla hand render
 *   - {@code DMZPOVPlayerRenderer} renders the full body with race skin + form
 *   - The head bone is hidden so the player does not clip into their own head
 *   - The model is translated by {@code FirstPersonManager.offsetFirstPersonView()}
 *     so its head aligns with the camera position
 */
@Mixin(value = FirstPersonManager.class, remap = false)
public abstract class FirstPersonManagerMixin {

    @Inject(method = "shouldRenderFirstPerson", at = @At("RETURN"), cancellable = true)
    private static void unofficialdmzaddon$enableForDMZPlayers(
            Player player, CallbackInfoReturnable<Boolean> cir) {

        // Already returning true — DMZ's own setting or a previous mixin enabled it
        if (cir.getReturnValue()) return;

        // Opt-out via our config
        if (!UnofficialDMZConfig.FIRST_PERSON_RACE_MODEL.get()) return;

        Minecraft mc = Minecraft.getInstance();

        // Must be the local client player
        if (player != mc.player) return;

        // Must be in first-person camera mode
        if (!mc.options.getCameraType().isFirstPerson()) return;

        // Never show while holding a map (vanilla hide-body rule DMZ respects)
        if (player.getMainHandItem().getItem() instanceof MapItem) return;
        if (player.getOffhandItem().getItem() instanceof MapItem) return;

        // Allow during chat but not other open screens (mirrors DMZ logic)
        if (mc.screen != null && !(mc.screen instanceof ChatScreen)) return;

        // Only enable for players that actually have a DMZ character
        boolean hasCharacter = StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> data.getStatus().hasCreatedCharacter())
                .orElse(false);
        if (!hasCharacter) return;

        cir.setReturnValue(true);
    }
}
