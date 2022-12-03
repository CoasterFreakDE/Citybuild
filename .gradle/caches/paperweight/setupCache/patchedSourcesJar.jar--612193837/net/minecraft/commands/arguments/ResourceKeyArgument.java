package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class ResourceKeyArgument<T> implements ArgumentType<ResourceKey<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ATTRIBUTE = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("attribute.unknown", id);
    });
    private static final DynamicCommandExceptionType ERROR_INVALID_FEATURE = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("commands.place.feature.invalid", id);
    });
    private static final DynamicCommandExceptionType ERROR_INVALID_STRUCTURE = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("commands.place.structure.invalid", id);
    });
    private static final DynamicCommandExceptionType ERROR_INVALID_TEMPLATE_POOL = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("commands.place.jigsaw.invalid", id);
    });
    final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceKeyArgument(ResourceKey<? extends Registry<T>> registryRef) {
        this.registryKey = registryRef;
    }

    public static <T> ResourceKeyArgument<T> key(ResourceKey<? extends Registry<T>> registryRef) {
        return new ResourceKeyArgument<>(registryRef);
    }

    private static <T> ResourceKey<T> getRegistryType(CommandContext<CommandSourceStack> context, String name, ResourceKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        ResourceKey<?> resourceKey = context.getArgument(name, ResourceKey.class);
        Optional<ResourceKey<T>> optional = resourceKey.cast(registryRef);
        return optional.orElseThrow(() -> {
            return invalidException.create(resourceKey);
        });
    }

    private static <T> Registry<T> getRegistry(CommandContext<CommandSourceStack> context, ResourceKey<? extends Registry<T>> registryRef) {
        return context.getSource().getServer().registryAccess().registryOrThrow(registryRef);
    }

    private static <T> Holder<T> getRegistryKeyType(CommandContext<CommandSourceStack> context, String name, ResourceKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        ResourceKey<T> resourceKey = getRegistryType(context, name, registryRef, invalidException);
        return getRegistry(context, registryRef).getHolder(resourceKey).orElseThrow(() -> {
            return invalidException.create(resourceKey.location());
        });
    }

    public static Attribute getAttribute(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ResourceKey<Attribute> resourceKey = getRegistryType(context, name, Registry.ATTRIBUTE_REGISTRY, ERROR_UNKNOWN_ATTRIBUTE);
        return getRegistry(context, Registry.ATTRIBUTE_REGISTRY).getOptional(resourceKey).orElseThrow(() -> {
            return ERROR_UNKNOWN_ATTRIBUTE.create(resourceKey.location());
        });
    }

    public static Holder<ConfiguredFeature<?, ?>> getConfiguredFeature(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return getRegistryKeyType(context, name, Registry.CONFIGURED_FEATURE_REGISTRY, ERROR_INVALID_FEATURE);
    }

    public static Holder<Structure> getStructure(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return getRegistryKeyType(context, name, Registry.STRUCTURE_REGISTRY, ERROR_INVALID_STRUCTURE);
    }

    public static Holder<StructureTemplatePool> getStructureTemplatePool(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return getRegistryKeyType(context, name, Registry.TEMPLATE_POOL_REGISTRY, ERROR_INVALID_TEMPLATE_POOL);
    }

    public ResourceKey<T> parse(StringReader stringReader) throws CommandSyntaxException {
        ResourceLocation resourceLocation = ResourceLocation.read(stringReader);
        return ResourceKey.create(this.registryKey, resourceLocation);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        Object var4 = commandContext.getSource();
        if (var4 instanceof SharedSuggestionProvider sharedSuggestionProvider) {
            return sharedSuggestionProvider.suggestRegistryElements(this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS, suggestionsBuilder, commandContext);
        } else {
            return suggestionsBuilder.buildFuture();
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T> implements ArgumentTypeInfo<ResourceKeyArgument<T>, ResourceKeyArgument.Info<T>.Template> {
        @Override
        public void serializeToNetwork(ResourceKeyArgument.Info.Template properties, FriendlyByteBuf buf) {
            buf.writeResourceLocation(properties.registryKey.location());
        }

        @Override
        public ResourceKeyArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
            ResourceLocation resourceLocation = friendlyByteBuf.readResourceLocation();
            return new ResourceKeyArgument.Info.Template(ResourceKey.createRegistryKey(resourceLocation));
        }

        @Override
        public void serializeToJson(ResourceKeyArgument.Info.Template properties, JsonObject json) {
            json.addProperty("registry", properties.registryKey.location().toString());
        }

        @Override
        public ResourceKeyArgument.Info<T>.Template unpack(ResourceKeyArgument<T> argumentType) {
            return new ResourceKeyArgument.Info.Template(argumentType.registryKey);
        }

        public final class Template implements ArgumentTypeInfo.Template<ResourceKeyArgument<T>> {
            final ResourceKey<? extends Registry<T>> registryKey;

            Template(ResourceKey<? extends Registry<T>> registryRef) {
                this.registryKey = registryRef;
            }

            @Override
            public ResourceKeyArgument<T> instantiate(CommandBuildContext commandBuildContext) {
                return new ResourceKeyArgument<>(this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceKeyArgument<T>, ?> type() {
                return Info.this;
            }
        }
    }
}
