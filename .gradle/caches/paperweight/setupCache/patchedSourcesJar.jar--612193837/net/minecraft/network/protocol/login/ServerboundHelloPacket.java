package net.minecraft.network.protocol.login;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.ProfilePublicKey;

public record ServerboundHelloPacket(String name, Optional<ProfilePublicKey.Data> publicKey, Optional<UUID> profileId) implements Packet<ServerLoginPacketListener> {
    public ServerboundHelloPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(16), buf.readOptional(ProfilePublicKey.Data::new), buf.readOptional(FriendlyByteBuf::readUUID));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.name, 16);
        buf.writeOptional(this.publicKey, (buf2, publicKey) -> {
            publicKey.write(buf);
        });
        buf.writeOptional(this.profileId, FriendlyByteBuf::writeUUID);
    }

    @Override
    public void handle(ServerLoginPacketListener listener) {
        listener.handleHello(this);
    }
}
