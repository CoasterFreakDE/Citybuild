package net.minecraft.network.chat;

import javax.annotation.Nullable;
import net.minecraft.util.SignatureValidator;
import net.minecraft.world.entity.player.ProfilePublicKey;

public interface SignedMessageValidator {
    static SignedMessageValidator create(@Nullable ProfilePublicKey publicKey, boolean secureChatEnforced) {
        return (SignedMessageValidator)(publicKey != null ? new SignedMessageValidator.KeyBased(publicKey.createSignatureValidator()) : new SignedMessageValidator.Unsigned(secureChatEnforced));
    }

    SignedMessageValidator.State validateHeader(SignedMessageHeader header, MessageSignature signature, byte[] bodyDigest);

    SignedMessageValidator.State validateMessage(PlayerChatMessage message);

    public static class KeyBased implements SignedMessageValidator {
        private final SignatureValidator validator;
        @Nullable
        private MessageSignature lastSignature;
        private boolean isChainConsistent = true;

        public KeyBased(SignatureValidator signatureVerifier) {
            this.validator = signatureVerifier;
        }

        private boolean validateChain(SignedMessageHeader header, MessageSignature signature, boolean fullMessage) {
            if (signature.isEmpty()) {
                return false;
            } else if (fullMessage && signature.equals(this.lastSignature)) {
                return true;
            } else {
                return this.lastSignature == null || this.lastSignature.equals(header.previousSignature());
            }
        }

        private boolean validateContents(SignedMessageHeader header, MessageSignature signature, byte[] bodyDigest, boolean fullMessage) {
            return this.validateChain(header, signature, fullMessage) && signature.verify(this.validator, header, bodyDigest);
        }

        private SignedMessageValidator.State updateAndValidate(SignedMessageHeader header, MessageSignature signature, byte[] bodyDigest, boolean fullMessage) {
            this.isChainConsistent = this.isChainConsistent && this.validateContents(header, signature, bodyDigest, fullMessage);
            if (!this.isChainConsistent) {
                return SignedMessageValidator.State.BROKEN_CHAIN;
            } else {
                this.lastSignature = signature;
                return SignedMessageValidator.State.SECURE;
            }
        }

        @Override
        public SignedMessageValidator.State validateHeader(SignedMessageHeader header, MessageSignature signature, byte[] bodyDigest) {
            return this.updateAndValidate(header, signature, bodyDigest, false);
        }

        @Override
        public SignedMessageValidator.State validateMessage(PlayerChatMessage message) {
            byte[] bs = message.signedBody().hash().asBytes();
            return this.updateAndValidate(message.signedHeader(), message.headerSignature(), bs, true);
        }
    }

    public static enum State {
        SECURE,
        NOT_SECURE,
        BROKEN_CHAIN;
    }

    public static class Unsigned implements SignedMessageValidator {
        private final boolean enforcesSecureChat;

        public Unsigned(boolean secureChatEnforced) {
            this.enforcesSecureChat = secureChatEnforced;
        }

        private SignedMessageValidator.State validate(MessageSignature signature) {
            if (!signature.isEmpty()) {
                return SignedMessageValidator.State.BROKEN_CHAIN;
            } else {
                return this.enforcesSecureChat ? SignedMessageValidator.State.BROKEN_CHAIN : SignedMessageValidator.State.NOT_SECURE;
            }
        }

        @Override
        public SignedMessageValidator.State validateHeader(SignedMessageHeader header, MessageSignature signature, byte[] bodyDigest) {
            return this.validate(signature);
        }

        @Override
        public SignedMessageValidator.State validateMessage(PlayerChatMessage message) {
            return this.validate(message.headerSignature());
        }
    }
}
