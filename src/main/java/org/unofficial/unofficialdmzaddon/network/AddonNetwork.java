package org.unofficial.unofficialdmzaddon.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.unofficial.unofficialdmzaddon.UnofficialDMZAddon;

public final class AddonNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(UnofficialDMZAddon.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private AddonNetwork() { }
    public static void register() {
        CHANNEL.registerMessage(0, UltraInstinctDodgePacket.class,
                UltraInstinctDodgePacket::encode, UltraInstinctDodgePacket::decode,
                UltraInstinctDodgePacket::handle);
    }
    public static void sendDodge(ServerPlayer player, boolean leanRight) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new UltraInstinctDodgePacket(player.getUUID(), leanRight));
    }
}