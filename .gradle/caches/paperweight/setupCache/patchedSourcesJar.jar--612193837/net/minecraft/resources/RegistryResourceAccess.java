package net.minecraft.resources;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public interface RegistryResourceAccess {
    <E> Map<ResourceKey<E>, RegistryResourceAccess.EntryThunk<E>> listResources(ResourceKey<? extends Registry<E>> key);

    <E> Optional<RegistryResourceAccess.EntryThunk<E>> getResource(ResourceKey<E> key);

    static RegistryResourceAccess forResourceManager(final ResourceManager resourceManager) {
        return new RegistryResourceAccess() {
            private static final String JSON = ".json";

            @Override
            public <E> Map<ResourceKey<E>, RegistryResourceAccess.EntryThunk<E>> listResources(ResourceKey<? extends Registry<E>> key) {
                String string = registryDirPath(key.location());
                Map<ResourceKey<E>, RegistryResourceAccess.EntryThunk<E>> map = Maps.newHashMap();
                resourceManager.listResources(string, (id) -> {
                    return id.getPath().endsWith(".json");
                }).forEach((id, resourceRef) -> {
                    String string2 = id.getPath();
                    String string3 = string2.substring(string.length() + 1, string2.length() - ".json".length());
                    ResourceKey<E> resourceKey2 = ResourceKey.create(key, new ResourceLocation(id.getNamespace(), string3));
                    map.put(resourceKey2, (jsonOps, decoder) -> {
                        try {
                            Reader reader = resourceRef.openAsReader();

                            DataResult var6;
                            try {
                                var6 = this.decodeElement(jsonOps, decoder, reader);
                            } catch (Throwable var9) {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (Throwable var8) {
                                        var9.addSuppressed(var8);
                                    }
                                }

                                throw var9;
                            }

                            if (reader != null) {
                                reader.close();
                            }

                            return var6;
                        } catch (JsonIOException | JsonSyntaxException | IOException var10) {
                            return DataResult.error("Failed to parse " + id + " file: " + var10.getMessage());
                        }
                    });
                });
                return map;
            }

            @Override
            public <E> Optional<RegistryResourceAccess.EntryThunk<E>> getResource(ResourceKey<E> key) {
                ResourceLocation resourceLocation = elementPath(key);
                return resourceManager.getResource(resourceLocation).map((resource) -> {
                    return (jsonOps, decoder) -> {
                        try {
                            Reader reader = resource.openAsReader();

                            DataResult var6;
                            try {
                                var6 = this.decodeElement(jsonOps, decoder, reader);
                            } catch (Throwable var9) {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (Throwable var8) {
                                        var9.addSuppressed(var8);
                                    }
                                }

                                throw var9;
                            }

                            if (reader != null) {
                                reader.close();
                            }

                            return var6;
                        } catch (JsonIOException | JsonSyntaxException | IOException var10) {
                            return DataResult.error("Failed to parse " + resourceLocation + " file: " + var10.getMessage());
                        }
                    };
                });
            }

            private <E> DataResult<RegistryResourceAccess.ParsedEntry<E>> decodeElement(DynamicOps<JsonElement> jsonOps, Decoder<E> decoder, Reader reader) throws IOException {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                return decoder.parse(jsonOps, jsonElement).map(RegistryResourceAccess.ParsedEntry::createWithoutId);
            }

            private static String registryDirPath(ResourceLocation id) {
                return id.getPath();
            }

            private static <E> ResourceLocation elementPath(ResourceKey<E> rootKey) {
                return new ResourceLocation(rootKey.location().getNamespace(), registryDirPath(rootKey.registry()) + "/" + rootKey.location().getPath() + ".json");
            }

            @Override
            public String toString() {
                return "ResourceAccess[" + resourceManager + "]";
            }
        };
    }

    @FunctionalInterface
    public interface EntryThunk<E> {
        DataResult<RegistryResourceAccess.ParsedEntry<E>> parseElement(DynamicOps<JsonElement> jsonOps, Decoder<E> decoder);
    }

    public static final class InMemoryStorage implements RegistryResourceAccess {
        private static final Logger LOGGER = LogUtils.getLogger();
        private final Map<ResourceKey<?>, RegistryResourceAccess.InMemoryStorage.Entry> entries = Maps.newIdentityHashMap();

        public <E> void add(RegistryAccess registryManager, ResourceKey<E> key, Encoder<E> encoder, int rawId, E entry, Lifecycle lifecycle) {
            DataResult<JsonElement> dataResult = encoder.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registryManager), entry);
            Optional<DataResult.PartialResult<JsonElement>> optional = dataResult.error();
            if (optional.isPresent()) {
                LOGGER.error("Error adding element: {}", (Object)optional.get().message());
            } else {
                this.entries.put(key, new RegistryResourceAccess.InMemoryStorage.Entry(dataResult.result().get(), rawId, lifecycle));
            }

        }

        @Override
        public <E> Map<ResourceKey<E>, RegistryResourceAccess.EntryThunk<E>> listResources(ResourceKey<? extends Registry<E>> key) {
            return this.entries.entrySet().stream().filter((entry) -> {
                return entry.getKey().isFor(key);
            }).collect(Collectors.toMap((entry) -> {
                return entry.getKey();
            }, (entry) -> {
                return entry.getValue()::parse;
            }));
        }

        @Override
        public <E> Optional<RegistryResourceAccess.EntryThunk<E>> getResource(ResourceKey<E> key) {
            RegistryResourceAccess.InMemoryStorage.Entry entry = this.entries.get(key);
            if (entry == null) {
                DataResult<RegistryResourceAccess.ParsedEntry<E>> dataResult = DataResult.error("Unknown element: " + key);
                return Optional.of((jsonOps, decoder) -> {
                    return dataResult;
                });
            } else {
                return Optional.of(entry::parse);
            }
        }

        static record Entry(JsonElement data, int id, Lifecycle lifecycle) {
            public <E> DataResult<RegistryResourceAccess.ParsedEntry<E>> parse(DynamicOps<JsonElement> jsonOps, Decoder<E> decoder) {
                return decoder.parse(jsonOps, this.data).setLifecycle(this.lifecycle).map((value) -> {
                    return RegistryResourceAccess.ParsedEntry.createWithId(value, this.id);
                });
            }
        }
    }

    public static record ParsedEntry<E>(E value, OptionalInt fixedId) {
        public static <E> RegistryResourceAccess.ParsedEntry<E> createWithoutId(E value) {
            return new RegistryResourceAccess.ParsedEntry<>(value, OptionalInt.empty());
        }

        public static <E> RegistryResourceAccess.ParsedEntry<E> createWithId(E value, int id) {
            return new RegistryResourceAccess.ParsedEntry<>(value, OptionalInt.of(id));
        }
    }
}
