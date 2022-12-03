package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import org.slf4j.Logger;

public abstract class TagsProvider<T> implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final DataGenerator.PathProvider pathProvider;
    protected final Registry<T> registry;
    private final Map<ResourceLocation, TagBuilder> builders = Maps.newLinkedHashMap();

    protected TagsProvider(DataGenerator root, Registry<T> registry) {
        this.pathProvider = root.createPathProvider(DataGenerator.Target.DATA_PACK, TagManager.getTagDir(registry.key()));
        this.registry = registry;
    }

    @Override
    public final String getName() {
        return "Tags for " + this.registry.key().location();
    }

    protected abstract void addTags();

    @Override
    public void run(CachedOutput writer) {
        this.builders.clear();
        this.addTags();
        this.builders.forEach((id, builder) -> {
            List<TagEntry> list = builder.build();
            List<TagEntry> list2 = list.stream().filter((tag) -> {
                return !tag.verifyIfPresent(this.registry::containsKey, this.builders::containsKey);
            }).toList();
            if (!list2.isEmpty()) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Couldn't define tag %s as it is missing following references: %s", id, list2.stream().map(Objects::toString).collect(Collectors.joining(","))));
            } else {
                JsonElement jsonElement = TagFile.CODEC.encodeStart(JsonOps.INSTANCE, new TagFile(list, false)).getOrThrow(false, LOGGER::error);
                Path path = this.pathProvider.json(id);

                try {
                    DataProvider.saveStable(writer, jsonElement, path);
                } catch (IOException var9) {
                    LOGGER.error("Couldn't save tags to {}", path, var9);
                }

            }
        });
    }

    protected TagsProvider.TagAppender<T> tag(TagKey<T> tag) {
        TagBuilder tagBuilder = this.getOrCreateRawBuilder(tag);
        return new TagsProvider.TagAppender<>(tagBuilder, this.registry);
    }

    protected TagBuilder getOrCreateRawBuilder(TagKey<T> tag) {
        return this.builders.computeIfAbsent(tag.location(), (id) -> {
            return TagBuilder.create();
        });
    }

    protected static class TagAppender<T> {
        private final TagBuilder builder;
        private final Registry<T> registry;

        TagAppender(TagBuilder builder, Registry<T> registry) {
            this.builder = builder;
            this.registry = registry;
        }

        public TagsProvider.TagAppender<T> add(T element) {
            this.builder.addElement(this.registry.getKey(element));
            return this;
        }

        @SafeVarargs
        public final TagsProvider.TagAppender<T> add(ResourceKey<T>... keys) {
            for(ResourceKey<T> resourceKey : keys) {
                this.builder.addElement(resourceKey.location());
            }

            return this;
        }

        public TagsProvider.TagAppender<T> addOptional(ResourceLocation id) {
            this.builder.addOptionalElement(id);
            return this;
        }

        public TagsProvider.TagAppender<T> addTag(TagKey<T> identifiedTag) {
            this.builder.addTag(identifiedTag.location());
            return this;
        }

        public TagsProvider.TagAppender<T> addOptionalTag(ResourceLocation id) {
            this.builder.addOptionalTag(id);
            return this;
        }

        @SafeVarargs
        public final TagsProvider.TagAppender<T> add(T... elements) {
            Stream.<T>of(elements).map(this.registry::getKey).forEach((id) -> {
                this.builder.addElement(id);
            });
            return this;
        }
    }
}
