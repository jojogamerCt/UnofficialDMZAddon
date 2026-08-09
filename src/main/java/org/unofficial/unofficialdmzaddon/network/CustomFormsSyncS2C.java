package org.unofficial.unofficialdmzaddon.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.unofficial.unofficialdmzaddon.client.CustomFormsClientState;
import org.unofficial.unofficialdmzaddon.dmz.CustomFormDefinition;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record CustomFormsSyncS2C(UUID owner, List<CustomFormDefinition> forms) {
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<CustomFormDefinition>>() {}.getType();

    public static void encode(CustomFormsSyncS2C packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.owner); buffer.writeUtf(GSON.toJson(packet.forms), 32767);
    }
    public static CustomFormsSyncS2C decode(FriendlyByteBuf buffer) {
        UUID owner = buffer.readUUID();
        List<CustomFormDefinition> forms = GSON.fromJson(buffer.readUtf(32767), LIST_TYPE);
        return new CustomFormsSyncS2C(owner, forms == null ? List.of() : forms);
    }
    public static void handle(CustomFormsSyncS2C packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CustomFormsClientState.accept(packet.owner, packet.forms)));
        context.get().setPacketHandled(true);
    }
}
