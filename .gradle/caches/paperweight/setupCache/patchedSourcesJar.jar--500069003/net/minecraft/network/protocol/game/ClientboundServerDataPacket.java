package net.minecraft.network.protocol.game;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundServerDataPacket implements Packet<ClientGamePacketListener> {
    private final Optional<Component> motd;
    private final Optional<String> iconBase64;
    private final boolean previewsChat;
    private final boolean enforcesSecureChat;

    public ClientboundServerDataPacket(@Nullable Component description, @Nullable String favicon, boolean previewsChat, boolean secureChatEnforced) {
        this.motd = Optional.ofNullable(description);
        this.iconBase64 = Optional.ofNullable(favicon);
        this.previewsChat = previewsChat;
        this.enforcesSecureChat = secureChatEnforced;
    }

    public ClientboundServerDataPacket(FriendlyByteBuf buf) {
        this.motd = buf.readOptional(FriendlyByteBuf::readComponent);
        this.iconBase64 = buf.readOptional(FriendlyByteBuf::readUtf);
        this.previewsChat = buf.readBoolean();
        this.enforcesSecureChat = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(this.motd, FriendlyByteBuf::writeComponent);
        buf.writeOptional(this.iconBase64, FriendlyByteBuf::writeUtf);
        buf.writeBoolean(this.previewsChat);
        buf.writeBoolean(this.enforcesSecureChat);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleServerData(this);
    }

    public Optional<Component> getMotd() {
        return this.motd;
    }

    public Optional<String> getIconBase64() {
        return this.iconBase64;
    }

    public boolean previewsChat() {
        return this.previewsChat;
    }

    public boolean enforcesSecureChat() {
        return this.enforcesSecureChat;
    }
}
