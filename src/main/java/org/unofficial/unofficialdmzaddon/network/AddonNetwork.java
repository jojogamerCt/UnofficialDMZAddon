package org.unofficial.unofficialdmzaddon.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

public final class AddonNetwork {
    private static final String PROTOCOL = "6";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private AddonNetwork() { }
    public static void register() {
        CHANNEL.registerMessage(0, UltraInstinctDodgePacket.class,
                UltraInstinctDodgePacket::encode, UltraInstinctDodgePacket::decode,
                UltraInstinctDodgePacket::handle);
        CHANNEL.registerMessage(1, UltraEgoBurstPacket.class,
                UltraEgoBurstPacket::encode, UltraEgoBurstPacket::decode,
                UltraEgoBurstPacket::handle);
        CHANNEL.registerMessage(2, CustomFormsSyncS2C.class,
                CustomFormsSyncS2C::encode, CustomFormsSyncS2C::decode, CustomFormsSyncS2C::handle);
        CHANNEL.registerMessage(3, SaveCustomFormC2S.class,
                SaveCustomFormC2S::encode, SaveCustomFormC2S::decode, SaveCustomFormC2S::handle);
        CHANNEL.registerMessage(4, DeleteCustomFormC2S.class,
                DeleteCustomFormC2S::encode, DeleteCustomFormC2S::decode, DeleteCustomFormC2S::handle);
        CHANNEL.registerMessage(5, SelectCustomFormC2S.class,
                SelectCustomFormC2S::encode, SelectCustomFormC2S::decode, SelectCustomFormC2S::handle);
        CHANNEL.registerMessage(6, CustomFormSaveResultS2C.class,
                CustomFormSaveResultS2C::encode, CustomFormSaveResultS2C::decode, CustomFormSaveResultS2C::handle);
    }
    public static void sendDodge(ServerPlayer player, boolean leanRight) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new UltraInstinctDodgePacket(player.getUUID(), leanRight));
    }
    public static void sendUltraEgoBurst(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new UltraEgoBurstPacket(player.getUUID()));
    }
    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    public static void sendToAll(net.minecraft.server.MinecraftServer server, Object packet) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sendTo(player, packet);
    }
    public static void sendToServer(Object packet) { CHANNEL.sendToServer(packet); }
}
