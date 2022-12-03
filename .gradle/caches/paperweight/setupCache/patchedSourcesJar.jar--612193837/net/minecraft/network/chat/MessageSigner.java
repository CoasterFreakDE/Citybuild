package net.minecraft.network.chat;

import java.time.Instant;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Crypt;

public record MessageSigner(UUID profileId, Instant timeStamp, long salt) {
    public MessageSigner(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readInstant(), buf.readLong());
    }

    public static MessageSigner create(UUID sender) {
        return new MessageSigner(sender, Instant.now(), Crypt.SaltSupplier.getLong());
    }

    public static MessageSigner system() {
        return create(Util.NIL_UUID);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.profileId);
        buf.writeInstant(this.timeStamp);
        buf.writeLong(this.salt);
    }

    public boolean isSystem() {
        return this.profileId.equals(Util.NIL_UUID);
    }
}
