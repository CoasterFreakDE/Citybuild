package net.minecraft.network.protocol.login;

import com.mojang.datafixers.util.Either;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;
import javax.crypto.SecretKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.SignatureUpdater;
import net.minecraft.world.entity.player.ProfilePublicKey;

public class ServerboundKeyPacket implements Packet<ServerLoginPacketListener> {
    private final byte[] keybytes;
    private final Either<byte[], Crypt.SaltSignaturePair> nonceOrSaltSignature;

    public ServerboundKeyPacket(SecretKey secretKey, PublicKey publicKey, byte[] nonce) throws CryptException {
        this.keybytes = Crypt.encryptUsingKey(publicKey, secretKey.getEncoded());
        this.nonceOrSaltSignature = Either.left(Crypt.encryptUsingKey(publicKey, nonce));
    }

    public ServerboundKeyPacket(SecretKey secretKey, PublicKey publicKey, long seed, byte[] signature) throws CryptException {
        this.keybytes = Crypt.encryptUsingKey(publicKey, secretKey.getEncoded());
        this.nonceOrSaltSignature = Either.right(new Crypt.SaltSignaturePair(seed, signature));
    }

    public ServerboundKeyPacket(FriendlyByteBuf buf) {
        this.keybytes = buf.readByteArray();
        this.nonceOrSaltSignature = buf.readEither(FriendlyByteBuf::readByteArray, Crypt.SaltSignaturePair::new);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByteArray(this.keybytes);
        buf.writeEither(this.nonceOrSaltSignature, FriendlyByteBuf::writeByteArray, Crypt.SaltSignaturePair::write);
    }

    @Override
    public void handle(ServerLoginPacketListener listener) {
        listener.handleKey(this);
    }

    public SecretKey getSecretKey(PrivateKey privateKey) throws CryptException {
        return Crypt.decryptByteToSecretKey(privateKey, this.keybytes);
    }

    public boolean isChallengeSignatureValid(byte[] nonce, ProfilePublicKey publicKeyInfo) {
        return this.nonceOrSaltSignature.map((encrypted) -> {
            return false;
        }, (signature) -> {
            return publicKeyInfo.createSignatureValidator().validate((SignatureUpdater)((updater) -> {
                updater.update(nonce);
                updater.update(signature.saltAsBytes());
            }), signature.signature());
        });
    }

    public boolean isNonceValid(byte[] nonce, PrivateKey privateKey) {
        Optional<byte[]> optional = this.nonceOrSaltSignature.left();

        try {
            return optional.isPresent() && Arrays.equals(nonce, Crypt.decryptUsingKey(privateKey, optional.get()));
        } catch (CryptException var5) {
            return false;
        }
    }
}
