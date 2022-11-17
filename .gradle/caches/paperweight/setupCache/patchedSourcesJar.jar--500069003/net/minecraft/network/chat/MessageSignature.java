package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.bytes.ByteArrays;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SignatureUpdater;
import net.minecraft.util.SignatureValidator;

public record MessageSignature(byte[] bytes) {
    public static final MessageSignature EMPTY = new MessageSignature(ByteArrays.EMPTY_ARRAY);

    public MessageSignature(FriendlyByteBuf buf) {
        this(buf.readByteArray());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByteArray(this.bytes);
    }

    public boolean verify(SignatureValidator verifier, SignedMessageHeader header, SignedMessageBody body) {
        if (!this.isEmpty()) {
            byte[] bs = body.hash().asBytes();
            return verifier.validate((SignatureUpdater)((updatable) -> {
                header.updateSignature(updatable, bs);
            }), this.bytes);
        } else {
            return false;
        }
    }

    public boolean verify(SignatureValidator verifier, SignedMessageHeader header, byte[] bodyDigest) {
        return !this.isEmpty() ? verifier.validate((SignatureUpdater)((updatable) -> {
            header.updateSignature(updatable, bodyDigest);
        }), this.bytes) : false;
    }

    public boolean isEmpty() {
        return this.bytes.length == 0;
    }

    @Nullable
    public ByteBuffer asByteBuffer() {
        return !this.isEmpty() ? ByteBuffer.wrap(this.bytes) : null;
    }

    @Override
    public boolean equals(Object object) {
        if (this != object) {
            if (object instanceof MessageSignature) {
                MessageSignature messageSignature = (MessageSignature)object;
                if (Arrays.equals(this.bytes, messageSignature.bytes)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    @Override
    public String toString() {
        return !this.isEmpty() ? Base64.getEncoder().encodeToString(this.bytes) : "empty";
    }
}
