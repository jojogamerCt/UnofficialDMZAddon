package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.client.gui.character.MinigamesScreen;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.client.SnakeGameScreen;
import org.unofficial.unofficialdmzaddon.dmz.SnakeUnlocks;

import java.util.Arrays;

@Mixin(value = MinigamesScreen.class, remap = false)
public abstract class MinigamesScreenMixin {
    @Shadow @Final @Mutable private static String[] MINIGAMES;
    @Shadow private int selectedIndex;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void unofficialdmzaddon$registerSnake(CallbackInfo ci) {
        if (!UnofficialDMZConfig.SNAKE_MINIGAME_ENABLED.get()
                || Arrays.asList(MINIGAMES).contains("snake")) return;
        String[] expanded = Arrays.copyOf(MINIGAMES, MINIGAMES.length + 1);
        expanded[expanded.length - 1] = "snake";
        MINIGAMES = expanded;
    }

    @Inject(method = "hasAccess", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$snakeAccess(int index, CallbackInfoReturnable<Boolean> cir) {
        if (!unofficialdmzaddon$isSnake(index)) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean unlocked = minecraft.player != null && StatsProvider.get(StatsCapability.INSTANCE, minecraft.player)
                .map(SnakeUnlocks::hasUnlockedFirstRaceForm)
                .orElse(false);
        cir.setReturnValue(UnofficialDMZConfig.SNAKE_MINIGAME_ENABLED.get() && unlocked);
    }

    @Inject(method = "playSelected", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$playSnake(CallbackInfo ci) {
        if (!unofficialdmzaddon$isSnake(selectedIndex)) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean unlocked = minecraft.player != null && StatsProvider.get(StatsCapability.INSTANCE, minecraft.player)
                .map(SnakeUnlocks::hasUnlockedFirstRaceForm)
                .orElse(false);
        if (UnofficialDMZConfig.SNAKE_MINIGAME_ENABLED.get() && unlocked) {
            minecraft.setScreen(new SnakeGameScreen());
        }
        ci.cancel();
    }

    @Inject(method = "master", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$snakeRequirement(int index, CallbackInfoReturnable<String> cir) {
        if (unofficialdmzaddon$isSnake(index)) cir.setReturnValue("snake_requirement");
    }

    private static boolean unofficialdmzaddon$isSnake(int index) {
        return index >= 0 && index < MINIGAMES.length && "snake".equals(MINIGAMES[index]);
    }
}
