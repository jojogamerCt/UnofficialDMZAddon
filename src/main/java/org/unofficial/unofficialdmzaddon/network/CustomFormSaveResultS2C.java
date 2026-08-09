package org.unofficial.unofficialdmzaddon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.unofficial.unofficialdmzaddon.client.CustomFormsClientState;

import java.util.function.Supplier;

public record CustomFormSaveResultS2C(boolean success, String id, int chargedTp, String reason) {
    public static void encode(CustomFormSaveResultS2C packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.success);
        buffer.writeUtf(packet.id == null ? "" : packet.id, 32);
        buffer.writeVarInt(Math.max(0, packet.chargedTp));
        buffer.writeUtf(packet.reason == null ? "" : packet.reason, 64);
    }

    public static CustomFormSaveResultS2C decode(FriendlyByteBuf buffer) {
        return new CustomFormSaveResultS2C(buffer.readBoolean(), buffer.readUtf(32), buffer.readVarInt(), buffer.readUtf(64));
    }

    public static void handle(CustomFormSaveResultS2C packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CustomFormsClientState.acceptSaveResult(packet)));
        context.get().setPacketHandled(true);
    }
}
