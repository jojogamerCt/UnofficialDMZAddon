package org.unofficial.unofficialdmzaddon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormManager;
import java.util.function.Supplier;

public record SelectCustomFormC2S(String id) {
    public static void encode(SelectCustomFormC2S packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.id, 32); }
    public static SelectCustomFormC2S decode(FriendlyByteBuf buffer) { return new SelectCustomFormC2S(buffer.readUtf(32)); }
    public static void handle(SelectCustomFormC2S packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> { ServerPlayer player = context.get().getSender(); if (player != null) CustomFormManager.selectForm(player, packet.id); });
        context.get().setPacketHandled(true);
    }
}
