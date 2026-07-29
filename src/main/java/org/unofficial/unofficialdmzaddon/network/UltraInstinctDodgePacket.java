package org.unofficial.unofficialdmzaddon.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.unofficial.unofficialdmzaddon.client.UltraInstinctDodgeVisuals;

public record UltraInstinctDodgePacket(UUID playerId, boolean leanRight) {
    static void encode(UltraInstinctDodgePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.leanRight);
    }
    static UltraInstinctDodgePacket decode(FriendlyByteBuf buffer) {
        return new UltraInstinctDodgePacket(buffer.readUUID(), buffer.readBoolean());
    }
    static void handle(UltraInstinctDodgePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> UltraInstinctDodgeVisuals.trigger(packet.playerId, packet.leanRight)));
        context.setPacketHandled(true);
    }
}