package org.unofficial.unofficialdmzaddon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormManager;
import java.util.function.Supplier;

public record DeleteCustomFormC2S(String id) {
    public static void encode(DeleteCustomFormC2S packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.id, 32); }
    public static DeleteCustomFormC2S decode(FriendlyByteBuf buffer) { return new DeleteCustomFormC2S(buffer.readUtf(32)); }
    public static void handle(DeleteCustomFormC2S packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> { ServerPlayer player = context.get().getSender(); if (player != null) CustomFormManager.deleteForm(player, packet.id); });
        context.get().setPacketHandled(true);
    }
}
