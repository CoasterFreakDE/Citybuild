package net.minecraft.network.chat;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record LastSeenMessages(List<LastSeenMessages.Entry> entries) {
    public static LastSeenMessages EMPTY = new LastSeenMessages(List.of());
    public static final int LAST_SEEN_MESSAGES_MAX_LENGTH = 5;

    public LastSeenMessages(FriendlyByteBuf buf) {
        this(buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 5), LastSeenMessages.Entry::new));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(this.entries, (buf2, entries) -> {
            entries.write(buf2);
        });
    }

    public void updateHash(DataOutput output) throws IOException {
        for(LastSeenMessages.Entry entry : this.entries) {
            UUID uUID = entry.profileId();
            MessageSignature messageSignature = entry.lastSignature();
            output.writeByte(70);
            output.writeLong(uUID.getMostSignificantBits());
            output.writeLong(uUID.getLeastSignificantBits());
            output.write(messageSignature.bytes());
        }

    }

    public static record Entry(UUID profileId, MessageSignature lastSignature) {
        public Entry(FriendlyByteBuf buf) {
            this(buf.readUUID(), new MessageSignature(buf));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(this.profileId);
            this.lastSignature.write(buf);
        }
    }

    public static record Update(LastSeenMessages lastSeen, Optional<LastSeenMessages.Entry> lastReceived) {
        public Update(FriendlyByteBuf buf) {
            this(new LastSeenMessages(buf), buf.readOptional(LastSeenMessages.Entry::new));
        }

        public void write(FriendlyByteBuf buf) {
            this.lastSeen.write(buf);
            buf.writeOptional(this.lastReceived, (buf2, lastReceived) -> {
                lastReceived.write(buf2);
            });
        }
    }
}
