package net.minecraft.network.protocol.login;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundGameProfilePacket implements Packet<ClientLoginPacketListener> {
    private final GameProfile gameProfile;

    public ClientboundGameProfilePacket(GameProfile profile) {
        this.gameProfile = profile;
    }

    public ClientboundGameProfilePacket(FriendlyByteBuf buf) {
        this.gameProfile = buf.readGameProfile();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeGameProfile(this.gameProfile);
    }

    @Override
    public void handle(ClientLoginPacketListener listener) {
        listener.handleGameProfile(this);
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }
}
