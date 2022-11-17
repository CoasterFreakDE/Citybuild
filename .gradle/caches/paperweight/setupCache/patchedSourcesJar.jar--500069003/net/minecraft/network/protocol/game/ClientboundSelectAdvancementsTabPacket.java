package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ClientboundSelectAdvancementsTabPacket implements Packet<ClientGamePacketListener> {
    @Nullable
    private final ResourceLocation tab;

    public ClientboundSelectAdvancementsTabPacket(@Nullable ResourceLocation tabId) {
        this.tab = tabId;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSelectAdvancementsTab(this);
    }

    public ClientboundSelectAdvancementsTabPacket(FriendlyByteBuf buf) {
        this.tab = buf.readNullable(FriendlyByteBuf::readResourceLocation);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNullable(this.tab, FriendlyByteBuf::writeResourceLocation);
    }

    @Nullable
    public ResourceLocation getTab() {
        return this.tab;
    }
}
