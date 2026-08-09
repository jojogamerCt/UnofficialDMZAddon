package org.unofficial.unofficialdmzaddon.mixin;

import com.dragonminez.common.network.C2S.TrainingRewardC2S;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;
import org.unofficial.unofficialdmzaddon.dmz.SnakeUnlocks;

import java.util.function.Supplier;

@Mixin(value = TrainingRewardC2S.class, remap = false)
public abstract class TrainingRewardSnakeMixin {
    @Shadow @Final private String minigameId;

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void unofficialdmzaddon$validateSnakeReward(Supplier<NetworkEvent.Context> supplier, CallbackInfo ci) {
        if (!"snake".equals(minigameId)) return;
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        boolean allowed = UnofficialDMZConfig.SNAKE_MINIGAME_ENABLED.get()
                && player != null
                && StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(SnakeUnlocks::hasUnlockedFirstRaceForm)
                .orElse(false);
        if (!allowed) {
            context.setPacketHandled(true);
            ci.cancel();
        }
    }
}
