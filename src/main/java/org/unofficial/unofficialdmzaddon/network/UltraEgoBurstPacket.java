package org.unofficial.unofficialdmzaddon.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.unofficial.unofficialdmzaddon.client.GodAuraClientHandler;

public record UltraEgoBurstPacket(UUID playerId) {
    static void encode(UltraEgoBurstPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
    }

    static UltraEgoBurstPacket decode(FriendlyByteBuf buffer) {
        return new UltraEgoBurstPacket(buffer.readUUID());
    }

    static void handle(UltraEgoBurstPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> GodAuraClientHandler.spawnUltraEgoBurst(packet.playerId)));
        context.setPacketHandled(true);
    }
}
