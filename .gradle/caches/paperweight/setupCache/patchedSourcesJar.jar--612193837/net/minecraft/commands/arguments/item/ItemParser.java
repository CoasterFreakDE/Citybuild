package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ItemParser {
    private static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType(Component.translatable("argument.item.tag.disallowed"));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("argument.item.id.invalid", id);
    });
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((tag) -> {
        return Component.translatable("arguments.item.tag.unknown", tag);
    });
    private static final char SYNTAX_START_NBT = '{';
    private static final char SYNTAX_TAG = '#';
    private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
    private final HolderLookup<Item> items;
    private final StringReader reader;
    private final boolean allowTags;
    private Either<Holder<Item>, HolderSet<Item>> result;
    @Nullable
    private CompoundTag nbt;
    private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

    private ItemParser(HolderLookup<Item> registryWrapper, StringReader reader, boolean allowTag) {
        this.items = registryWrapper;
        this.reader = reader;
        this.allowTags = allowTag;
    }

    public static ItemParser.ItemResult parseForItem(HolderLookup<Item> registryWrapper, StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();

        try {
            ItemParser itemParser = new ItemParser(registryWrapper, reader, false);
            itemParser.parse();
            Holder<Item> holder = itemParser.result.left().orElseThrow(() -> {
                return new IllegalStateException("Parser returned unexpected tag name");
            });
            return new ItemParser.ItemResult(holder, itemParser.nbt);
        } catch (CommandSyntaxException var5) {
            reader.setCursor(i);
            throw var5;
        }
    }

    public static Either<ItemParser.ItemResult, ItemParser.TagResult> parseForTesting(HolderLookup<Item> registryWrapper, StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();

        try {
            ItemParser itemParser = new ItemParser(registryWrapper, reader, true);
            itemParser.parse();
            return itemParser.result.mapBoth((item) -> {
                return new ItemParser.ItemResult(item, itemParser.nbt);
            }, (tag) -> {
                return new ItemParser.TagResult(tag, itemParser.nbt);
            });
        } catch (CommandSyntaxException var4) {
            reader.setCursor(i);
            throw var4;
        }
    }

    public static CompletableFuture<Suggestions> fillSuggestions(HolderLookup<Item> registryWrapper, SuggestionsBuilder builder, boolean allowTag) {
        StringReader stringReader = new StringReader(builder.getInput());
        stringReader.setCursor(builder.getStart());
        ItemParser itemParser = new ItemParser(registryWrapper, stringReader, allowTag);

        try {
            itemParser.parse();
        } catch (CommandSyntaxException var6) {
        }

        return itemParser.suggestions.apply(builder.createOffset(stringReader.getCursor()));
    }

    private void readItem() throws CommandSyntaxException {
        int i = this.reader.getCursor();
        ResourceLocation resourceLocation = ResourceLocation.read(this.reader);
        Optional<Holder<Item>> optional = this.items.get(ResourceKey.create(Registry.ITEM_REGISTRY, resourceLocation));
        this.result = Either.left(optional.orElseThrow(() -> {
            this.reader.setCursor(i);
            return ERROR_UNKNOWN_ITEM.createWithContext(this.reader, resourceLocation);
        }));
    }

    private void readTag() throws CommandSyntaxException {
        if (!this.allowTags) {
            throw ERROR_NO_TAGS_ALLOWED.createWithContext(this.reader);
        } else {
            int i = this.reader.getCursor();
            this.reader.expect('#');
            this.suggestions = this::suggestTag;
            ResourceLocation resourceLocation = ResourceLocation.read(this.reader);
            Optional<? extends HolderSet<Item>> optional = this.items.get(TagKey.create(Registry.ITEM_REGISTRY, resourceLocation));
            this.result = Either.right(optional.orElseThrow(() -> {
                this.reader.setCursor(i);
                return ERROR_UNKNOWN_TAG.createWithContext(this.reader, resourceLocation);
            }));
        }
    }

    private void readNbt() throws CommandSyntaxException {
        this.nbt = (new TagParser(this.reader)).readStruct();
    }

    private void parse() throws CommandSyntaxException {
        if (this.allowTags) {
            this.suggestions = this::suggestItemIdOrTag;
        } else {
            this.suggestions = this::suggestItem;
        }

        if (this.reader.canRead() && this.reader.peek() == '#') {
            this.readTag();
        } else {
            this.readItem();
        }

        this.suggestions = this::suggestOpenNbt;
        if (this.reader.canRead() && this.reader.peek() == '{') {
            this.suggestions = SUGGEST_NOTHING;
            this.readNbt();
        }

    }

    private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf('{'));
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(this.items.listTags().map(TagKey::location), builder, String.valueOf('#'));
    }

    private CompletableFuture<Suggestions> suggestItem(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(this.items.listElements().map(ResourceKey::location), builder);
    }

    private CompletableFuture<Suggestions> suggestItemIdOrTag(SuggestionsBuilder builder) {
        this.suggestTag(builder);
        return this.suggestItem(builder);
    }

    public static record ItemResult(Holder<Item> item, @Nullable CompoundTag nbt) {
    }

    public static record TagResult(HolderSet<Item> tag, @Nullable CompoundTag nbt) {
    }
}
