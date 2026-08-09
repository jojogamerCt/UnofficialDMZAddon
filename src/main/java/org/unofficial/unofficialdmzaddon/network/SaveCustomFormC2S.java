package org.unofficial.unofficialdmzaddon.network;

import com.google.gson.Gson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormDefinition;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormManager;

import java.util.function.Supplier;

public record SaveCustomFormC2S(CustomFormDefinition form) {
    private static final Gson GSON = new Gson();
    public static void encode(SaveCustomFormC2S packet, FriendlyByteBuf buffer) { buffer.writeUtf(GSON.toJson(packet.form), 4096); }
    public static SaveCustomFormC2S decode(FriendlyByteBuf buffer) { return new SaveCustomFormC2S(GSON.fromJson(buffer.readUtf(4096), CustomFormDefinition.class)); }
    public static void handle(SaveCustomFormC2S packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> { ServerPlayer player = context.get().getSender(); if (player != null && packet.form != null) CustomFormManager.saveForm(player, packet.form); });
        context.get().setPacketHandled(true);
    }
}
