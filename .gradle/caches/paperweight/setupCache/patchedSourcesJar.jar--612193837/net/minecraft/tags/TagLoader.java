package net.minecraft.tags;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public class TagLoader<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PATH_SUFFIX = ".json";
    private static final int PATH_SUFFIX_LENGTH = ".json".length();
    final Function<ResourceLocation, Optional<T>> idToValue;
    private final String directory;

    public TagLoader(Function<ResourceLocation, Optional<T>> registryGetter, String dataType) {
        this.idToValue = registryGetter;
        this.directory = dataType;
    }

    public Map<ResourceLocation, List<TagLoader.EntryWithSource>> load(ResourceManager manager) {
        Map<ResourceLocation, List<TagLoader.EntryWithSource>> map = Maps.newHashMap();

        for(Map.Entry<ResourceLocation, List<Resource>> entry : manager.listResourceStacks(this.directory, (id) -> {
            return id.getPath().endsWith(".json");
        }).entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            String string = resourceLocation.getPath();
            ResourceLocation resourceLocation2 = new ResourceLocation(resourceLocation.getNamespace(), string.substring(this.directory.length() + 1, string.length() - PATH_SUFFIX_LENGTH));

            for(Resource resource : entry.getValue()) {
                try {
                    Reader reader = resource.openAsReader();

                    try {
                        JsonElement jsonElement = JsonParser.parseReader(reader);
                        List<TagLoader.EntryWithSource> list = map.computeIfAbsent(resourceLocation2, (id) -> {
                            return new ArrayList();
                        });
                        TagFile tagFile = TagFile.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, jsonElement)).getOrThrow(false, LOGGER::error);
                        if (tagFile.replace()) {
                            list.clear();
                        }

                        String string2 = resource.sourcePackId();
                        tagFile.entries().forEach((entryx) -> {
                            list.add(new TagLoader.EntryWithSource(entryx, string2));
                        });
                    } catch (Throwable var16) {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Throwable var15) {
                                var16.addSuppressed(var15);
                            }
                        }

                        throw var16;
                    }

                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception var17) {
                    LOGGER.error("Couldn't read tag list {} from {} in data pack {}", resourceLocation2, resourceLocation, resource.sourcePackId(), var17);
                }
            }
        }

        return map;
    }

    private static void visitDependenciesAndElement(Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags, Multimap<ResourceLocation, ResourceLocation> referencedTagIdsByTagId, Set<ResourceLocation> alreadyResolved, ResourceLocation tagId, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>> resolver) {
        if (alreadyResolved.add(tagId)) {
            referencedTagIdsByTagId.get(tagId).forEach((resolvedTagId) -> {
                visitDependenciesAndElement(tags, referencedTagIdsByTagId, alreadyResolved, resolvedTagId, resolver);
            });
            List<TagLoader.EntryWithSource> list = tags.get(tagId);
            if (list != null) {
                resolver.accept(tagId, list);
            }

        }
    }

    private static boolean isCyclic(Multimap<ResourceLocation, ResourceLocation> referencedTagIdsByTagId, ResourceLocation tagId, ResourceLocation referencedTagId) {
        Collection<ResourceLocation> collection = referencedTagIdsByTagId.get(referencedTagId);
        return collection.contains(tagId) ? true : collection.stream().anyMatch((id) -> {
            return isCyclic(referencedTagIdsByTagId, tagId, id);
        });
    }

    private static void addDependencyIfNotCyclic(Multimap<ResourceLocation, ResourceLocation> referencedTagIdsByTagId, ResourceLocation tagId, ResourceLocation referencedTagId) {
        if (!isCyclic(referencedTagIdsByTagId, tagId, referencedTagId)) {
            referencedTagIdsByTagId.put(tagId, referencedTagId);
        }

    }

    private Either<Collection<TagLoader.EntryWithSource>, Collection<T>> build(TagEntry.Lookup<T> valueGetter, List<TagLoader.EntryWithSource> entries) {
        ImmutableSet.Builder<T> builder = ImmutableSet.builder();
        List<TagLoader.EntryWithSource> list = new ArrayList<>();

        for(TagLoader.EntryWithSource entryWithSource : entries) {
            if (!entryWithSource.entry().build(valueGetter, builder::add)) {
                list.add(entryWithSource);
            }
        }

        return list.isEmpty() ? Either.right(builder.build()) : Either.left(list);
    }

    public Map<ResourceLocation, Collection<T>> build(Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags) {
        final Map<ResourceLocation, Collection<T>> map = Maps.newHashMap();
        TagEntry.Lookup<T> lookup = new TagEntry.Lookup<T>() {
            @Nullable
            @Override
            public T element(ResourceLocation id) {
                return TagLoader.this.idToValue.apply(id).orElse((T)null);
            }

            @Nullable
            @Override
            public Collection<T> tag(ResourceLocation id) {
                return map.get(id);
            }
        };
        Multimap<ResourceLocation, ResourceLocation> multimap = HashMultimap.create();
        tags.forEach((tagId, entries) -> {
            entries.forEach((entry) -> {
                entry.entry.visitRequiredDependencies((referencedTagId) -> {
                    addDependencyIfNotCyclic(multimap, tagId, referencedTagId);
                });
            });
        });
        tags.forEach((tagId, entries) -> {
            entries.forEach((entry) -> {
                entry.entry.visitOptionalDependencies((referencedTagId) -> {
                    addDependencyIfNotCyclic(multimap, tagId, referencedTagId);
                });
            });
        });
        Set<ResourceLocation> set = Sets.newHashSet();
        tags.keySet().forEach((tagId) -> {
            visitDependenciesAndElement(tags, multimap, set, tagId, (tagId2, entries) -> {
                this.build(lookup, entries).ifLeft((missingReferences) -> {
                    LOGGER.error("Couldn't load tag {} as it is missing following references: {}", tagId2, missingReferences.stream().map(Objects::toString).collect(Collectors.joining(", ")));
                }).ifRight((resolvedEntries) -> {
                    map.put(tagId2, resolvedEntries);
                });
            });
        });
        return map;
    }

    public Map<ResourceLocation, Collection<T>> loadAndBuild(ResourceManager manager) {
        return this.build(this.load(manager));
    }

    public static record EntryWithSource(TagEntry entry, String source) {
        @Override
        public String toString() {
            return this.entry + " (from " + this.source + ")";
        }
    }
}
