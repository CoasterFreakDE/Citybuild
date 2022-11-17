package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PreviewableCommand;

public record ArgumentSignatures(List<ArgumentSignatures.Entry> entries) {
    public static final ArgumentSignatures EMPTY = new ArgumentSignatures(List.of());
    private static final int MAX_ARGUMENT_COUNT = 8;
    private static final int MAX_ARGUMENT_NAME_LENGTH = 16;

    public ArgumentSignatures(FriendlyByteBuf buf) {
        this(buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 8), ArgumentSignatures.Entry::new));
    }

    public MessageSignature get(String argumentName) {
        for(ArgumentSignatures.Entry entry : this.entries) {
            if (entry.name.equals(argumentName)) {
                return entry.signature;
            }
        }

        return MessageSignature.EMPTY;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(this.entries, (buf2, entry) -> {
            entry.write(buf2);
        });
    }

    public static boolean hasSignableArguments(PreviewableCommand<?> arguments) {
        return arguments.arguments().stream().anyMatch((argument) -> {
            return argument.previewType() instanceof SignedArgument;
        });
    }

    public static ArgumentSignatures signCommand(PreviewableCommand<?> arguments, ArgumentSignatures.Signer signer) {
        List<ArgumentSignatures.Entry> list = collectPlainSignableArguments(arguments).stream().map((entry) -> {
            MessageSignature messageSignature = signer.sign(entry.getFirst(), entry.getSecond());
            return new ArgumentSignatures.Entry(entry.getFirst(), messageSignature);
        }).toList();
        return new ArgumentSignatures(list);
    }

    public static List<Pair<String, String>> collectPlainSignableArguments(PreviewableCommand<?> arguments) {
        List<Pair<String, String>> list = new ArrayList<>();

        for(PreviewableCommand.Argument<?> argument : arguments.arguments()) {
            PreviewedArgument string = argument.previewType();
            if (string instanceof SignedArgument<?> signedArgument) {
                String string = getSignableText(signedArgument, argument.parsedValue());
                list.add(Pair.of(argument.name(), string));
            }
        }

        return list;
    }

    private static <T> String getSignableText(SignedArgument<T> type, ParsedArgument<?, ?> argument) {
        return type.getSignableText((T)argument.getResult());
    }

    public static record Entry(String name, MessageSignature signature) {
        public Entry(FriendlyByteBuf buf) {
            this(buf.readUtf(16), new MessageSignature(buf));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.name, 16);
            this.signature.write(buf);
        }
    }

    @FunctionalInterface
    public interface Signer {
        MessageSignature sign(String argumentName, String value);
    }
}
