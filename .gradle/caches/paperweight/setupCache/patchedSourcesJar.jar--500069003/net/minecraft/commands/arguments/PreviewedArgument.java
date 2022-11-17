package net.minecraft.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public interface PreviewedArgument<T> extends ArgumentType<T> {
    @Nullable
    default CompletableFuture<Component> resolvePreview(CommandSourceStack source, ParsedArgument<CommandSourceStack, ?> parsedValue) throws CommandSyntaxException {
        return this.getValueType().isInstance(parsedValue.getResult()) ? this.resolvePreview(source, this.getValueType().cast(parsedValue.getResult())) : null;
    }

    CompletableFuture<Component> resolvePreview(CommandSourceStack source, T format) throws CommandSyntaxException;

    Class<T> getValueType();
}
