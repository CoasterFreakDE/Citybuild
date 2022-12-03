package net.minecraft.network.chat;

import java.security.SignatureException;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SignatureUpdater;

public record SignedMessageHeader(@Nullable MessageSignature previousSignature, UUID sender) {
    public SignedMessageHeader(FriendlyByteBuf buf) {
        this(buf.readNullable(MessageSignature::new), buf.readUUID());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNullable(this.previousSignature, (buf2, precedingSignature) -> {
            precedingSignature.write(buf2);
        });
        buf.writeUUID(this.sender);
    }

    public void updateSignature(SignatureUpdater.Output updater, byte[] bodyDigest) throws SignatureException {
        if (this.previousSignature != null) {
            updater.update(this.previousSignature.bytes());
        }

        updater.update(UUIDUtil.uuidToByteArray(this.sender));
        updater.update(bodyDigest);
    }
}
