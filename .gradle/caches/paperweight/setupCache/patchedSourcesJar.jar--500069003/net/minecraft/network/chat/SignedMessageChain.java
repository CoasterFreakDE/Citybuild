package net.minecraft.network.chat;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.util.SignatureUpdater;
import net.minecraft.util.Signer;

public class SignedMessageChain {
    @Nullable
    private MessageSignature previousSignature;

    private SignedMessageChain.Link pack(Signer signer, MessageSigner metadata, ChatMessageContent contents, LastSeenMessages lastSeenMessages) {
        MessageSignature messageSignature = pack(signer, metadata, this.previousSignature, contents, lastSeenMessages);
        this.previousSignature = messageSignature;
        return new SignedMessageChain.Link(messageSignature);
    }

    private static MessageSignature pack(Signer signer, MessageSigner metadata, @Nullable MessageSignature precedingSignature, ChatMessageContent contents, LastSeenMessages lastSeenMessages) {
        SignedMessageHeader signedMessageHeader = new SignedMessageHeader(precedingSignature, metadata.profileId());
        SignedMessageBody signedMessageBody = new SignedMessageBody(contents, metadata.timeStamp(), metadata.salt(), lastSeenMessages);
        byte[] bs = signedMessageBody.hash().asBytes();
        return new MessageSignature(signer.sign((SignatureUpdater)((updatable) -> {
            signedMessageHeader.updateSignature(updatable, bs);
        })));
    }

    private PlayerChatMessage unpack(SignedMessageChain.Link signature, MessageSigner metadata, ChatMessageContent contents, LastSeenMessages lastSeenMessages) {
        PlayerChatMessage playerChatMessage = unpack(signature, this.previousSignature, metadata, contents, lastSeenMessages);
        this.previousSignature = signature.signature;
        return playerChatMessage;
    }

    private static PlayerChatMessage unpack(SignedMessageChain.Link signature, @Nullable MessageSignature precedingSignature, MessageSigner metadata, ChatMessageContent contents, LastSeenMessages lastSeenMessage) {
        SignedMessageHeader signedMessageHeader = new SignedMessageHeader(precedingSignature, metadata.profileId());
        SignedMessageBody signedMessageBody = new SignedMessageBody(contents, metadata.timeStamp(), metadata.salt(), lastSeenMessage);
        return new PlayerChatMessage(signedMessageHeader, signature.signature, signedMessageBody, Optional.empty(), FilterMask.PASS_THROUGH);
    }

    public SignedMessageChain.Decoder decoder() {
        return this::unpack;
    }

    public SignedMessageChain.Encoder encoder() {
        return this::pack;
    }

    @FunctionalInterface
    public interface Decoder {
        SignedMessageChain.Decoder UNSIGNED = (signature, metadata, content, lastSeenMessages) -> {
            return PlayerChatMessage.unsigned(metadata, content);
        };

        PlayerChatMessage unpack(SignedMessageChain.Link signature, MessageSigner metadata, ChatMessageContent content, LastSeenMessages lastSeenMessages);
    }

    @FunctionalInterface
    public interface Encoder {
        SignedMessageChain.Link pack(Signer signer, MessageSigner metadata, ChatMessageContent contents, LastSeenMessages lastSeenMessages);
    }

    public static record Link(MessageSignature signature) {
    }
}
