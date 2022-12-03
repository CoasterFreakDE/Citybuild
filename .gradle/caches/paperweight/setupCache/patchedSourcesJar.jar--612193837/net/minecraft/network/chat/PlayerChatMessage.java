package net.minecraft.network.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SignatureValidator;
import net.minecraft.world.entity.player.ProfilePublicKey;

public record PlayerChatMessage(SignedMessageHeader signedHeader, MessageSignature headerSignature, SignedMessageBody signedBody, Optional<Component> unsignedContent, FilterMask filterMask) {
    public static final Duration MESSAGE_EXPIRES_AFTER_SERVER = Duration.ofMinutes(5L);
    public static final Duration MESSAGE_EXPIRES_AFTER_CLIENT = MESSAGE_EXPIRES_AFTER_SERVER.plus(Duration.ofMinutes(2L));

    public PlayerChatMessage(FriendlyByteBuf buf) {
        this(new SignedMessageHeader(buf), new MessageSignature(buf), new SignedMessageBody(buf), buf.readOptional(FriendlyByteBuf::readComponent), FilterMask.read(buf));
    }

    public static PlayerChatMessage system(ChatMessageContent content) {
        return unsigned(MessageSigner.system(), content);
    }

    public static PlayerChatMessage unsigned(MessageSigner metadata, ChatMessageContent content) {
        SignedMessageBody signedMessageBody = new SignedMessageBody(content, metadata.timeStamp(), metadata.salt(), LastSeenMessages.EMPTY);
        SignedMessageHeader signedMessageHeader = new SignedMessageHeader((MessageSignature)null, metadata.profileId());
        return new PlayerChatMessage(signedMessageHeader, MessageSignature.EMPTY, signedMessageBody, Optional.empty(), FilterMask.PASS_THROUGH);
    }

    public void write(FriendlyByteBuf buf) {
        this.signedHeader.write(buf);
        this.headerSignature.write(buf);
        this.signedBody.write(buf);
        buf.writeOptional(this.unsignedContent, FriendlyByteBuf::writeComponent);
        FilterMask.write(buf, this.filterMask);
    }

    public PlayerChatMessage withUnsignedContent(Component unsignedContent) {
        Optional<Component> optional = !this.signedContent().decorated().equals(unsignedContent) ? Optional.of(unsignedContent) : Optional.empty();
        return new PlayerChatMessage(this.signedHeader, this.headerSignature, this.signedBody, optional, this.filterMask);
    }

    public PlayerChatMessage removeUnsignedContent() {
        return this.unsignedContent.isPresent() ? new PlayerChatMessage(this.signedHeader, this.headerSignature, this.signedBody, Optional.empty(), this.filterMask) : this;
    }

    public PlayerChatMessage filter(FilterMask filterMask) {
        return this.filterMask.equals(filterMask) ? this : new PlayerChatMessage(this.signedHeader, this.headerSignature, this.signedBody, this.unsignedContent, filterMask);
    }

    public PlayerChatMessage filter(boolean enabled) {
        return this.filter(enabled ? this.filterMask : FilterMask.PASS_THROUGH);
    }

    public boolean verify(SignatureValidator verifier) {
        return this.headerSignature.verify(verifier, this.signedHeader, this.signedBody);
    }

    public boolean verify(ProfilePublicKey key) {
        SignatureValidator signatureValidator = key.createSignatureValidator();
        return this.verify(signatureValidator);
    }

    public boolean verify(ChatSender profile) {
        ProfilePublicKey profilePublicKey = profile.profilePublicKey();
        return profilePublicKey != null && this.verify(profilePublicKey);
    }

    public ChatMessageContent signedContent() {
        return this.signedBody.content();
    }

    public Component serverContent() {
        return this.unsignedContent().orElse(this.signedContent().decorated());
    }

    public Instant timeStamp() {
        return this.signedBody.timeStamp();
    }

    public long salt() {
        return this.signedBody.salt();
    }

    public boolean hasExpiredServer(Instant currentTime) {
        return currentTime.isAfter(this.timeStamp().plus(MESSAGE_EXPIRES_AFTER_SERVER));
    }

    public boolean hasExpiredClient(Instant currentTime) {
        return currentTime.isAfter(this.timeStamp().plus(MESSAGE_EXPIRES_AFTER_CLIENT));
    }

    public MessageSigner signer() {
        return new MessageSigner(this.signedHeader.sender(), this.timeStamp(), this.salt());
    }

    @Nullable
    public LastSeenMessages.Entry toLastSeenEntry() {
        MessageSigner messageSigner = this.signer();
        return !this.headerSignature.isEmpty() && !messageSigner.isSystem() ? new LastSeenMessages.Entry(messageSigner.profileId(), this.headerSignature) : null;
    }

    public boolean hasSignatureFrom(UUID sender) {
        return !this.headerSignature.isEmpty() && this.signedHeader.sender().equals(sender);
    }

    public boolean isFullyFiltered() {
        return this.filterMask.isFullyFiltered();
    }
}
