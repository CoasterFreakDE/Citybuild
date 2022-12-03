package net.minecraft.network.protocol.login;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundCustomQueryPacket implements Packet<ServerLoginPacketListener> {
    private static final int MAX_PAYLOAD_SIZE = 1048576;
    private final int transactionId;
    @Nullable
    private final FriendlyByteBuf data;

    public ServerboundCustomQueryPacket(int queryId, @Nullable FriendlyByteBuf response) {
        this.transactionId = queryId;
        this.data = response;
    }

    public ServerboundCustomQueryPacket(FriendlyByteBuf buf) {
        this.transactionId = buf.readVarInt();
        this.data = buf.readNullable((buf2) -> {
            int i = buf2.readableBytes();
            if (i >= 0 && i <= 1048576) {
                return new FriendlyByteBuf(buf2.readBytes(i));
            } else {
                throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
            }
        });
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.transactionId);
        buf.writeNullable(this.data, (buf2, response) -> {
            buf2.writeBytes(response.slice());
        });
    }

    @Override
    public void handle(ServerLoginPacketListener listener) {
        listener.handleCustomQueryPacket(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    @Nullable
    public FriendlyByteBuf getData() {
        return this.data;
    }
}
