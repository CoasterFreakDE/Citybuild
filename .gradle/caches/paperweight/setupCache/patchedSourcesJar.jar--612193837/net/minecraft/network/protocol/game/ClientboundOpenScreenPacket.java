package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.MenuType;

public class ClientboundOpenScreenPacket implements Packet<ClientGamePacketListener> {
    private final int containerId;
    private final MenuType<?> type;
    private final Component title;

    public ClientboundOpenScreenPacket(int syncId, MenuType<?> type, Component name) {
        this.containerId = syncId;
        this.type = type;
        this.title = name;
    }

    public ClientboundOpenScreenPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.type = buf.readById(Registry.MENU);
        this.title = buf.readComponent();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.containerId);
        buf.writeId(Registry.MENU, this.type);
        buf.writeComponent(this.title);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleOpenScreen(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    @Nullable
    public MenuType<?> getType() {
        return this.type;
    }

    public Component getTitle() {
        return this.title;
    }
}
