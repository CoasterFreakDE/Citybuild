package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;

public class FallbackResourceManager implements ResourceManager {
    static final Logger LOGGER = LogUtils.getLogger();
    protected final List<FallbackResourceManager.PackEntry> fallbacks = Lists.newArrayList();
    final PackType type;
    private final String namespace;

    public FallbackResourceManager(PackType type, String namespace) {
        this.type = type;
        this.namespace = namespace;
    }

    public void push(PackResources pack) {
        this.pushInternal(pack.getName(), pack, (Predicate<ResourceLocation>)null);
    }

    public void push(PackResources pack, Predicate<ResourceLocation> filter) {
        this.pushInternal(pack.getName(), pack, filter);
    }

    public void pushFilterOnly(String name, Predicate<ResourceLocation> filter) {
        this.pushInternal(name, (PackResources)null, filter);
    }

    private void pushInternal(String name, @Nullable PackResources underlyingPack, @Nullable Predicate<ResourceLocation> filter) {
        this.fallbacks.add(new FallbackResourceManager.PackEntry(name, underlyingPack, filter));
    }

    @Override
    public Set<String> getNamespaces() {
        return ImmutableSet.of(this.namespace);
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation id) {
        if (!this.isValidLocation(id)) {
            return Optional.empty();
        } else {
            for(int i = this.fallbacks.size() - 1; i >= 0; --i) {
                FallbackResourceManager.PackEntry packEntry = this.fallbacks.get(i);
                PackResources packResources = packEntry.resources;
                if (packResources != null && packResources.hasResource(this.type, id)) {
                    return Optional.of(new Resource(packResources.getName(), this.createResourceGetter(id, packResources), this.createStackMetadataFinder(id, i)));
                }

                if (packEntry.isFiltered(id)) {
                    LOGGER.warn("Resource {} not found, but was filtered by pack {}", id, packEntry.name);
                    return Optional.empty();
                }
            }

            return Optional.empty();
        }
    }

    Resource.IoSupplier<InputStream> createResourceGetter(ResourceLocation id, PackResources pack) {
        return LOGGER.isDebugEnabled() ? () -> {
            InputStream inputStream = pack.getResource(this.type, id);
            return new FallbackResourceManager.LeakedResourceWarningInputStream(inputStream, id, pack.getName());
        } : () -> {
            return pack.getResource(this.type, id);
        };
    }

    private boolean isValidLocation(ResourceLocation id) {
        return !id.getPath().contains("..");
    }

    @Override
    public List<Resource> getResourceStack(ResourceLocation id) {
        if (!this.isValidLocation(id)) {
            return List.of();
        } else {
            List<FallbackResourceManager.SinglePackResourceThunkSupplier> list = Lists.newArrayList();
            ResourceLocation resourceLocation = getMetadataLocation(id);
            String string = null;

            for(FallbackResourceManager.PackEntry packEntry : this.fallbacks) {
                if (packEntry.isFiltered(id)) {
                    if (!list.isEmpty()) {
                        string = packEntry.name;
                    }

                    list.clear();
                } else if (packEntry.isFiltered(resourceLocation)) {
                    list.forEach(FallbackResourceManager.SinglePackResourceThunkSupplier::ignoreMeta);
                }

                PackResources packResources = packEntry.resources;
                if (packResources != null && packResources.hasResource(this.type, id)) {
                    list.add(new FallbackResourceManager.SinglePackResourceThunkSupplier(id, resourceLocation, packResources));
                }
            }

            if (list.isEmpty() && string != null) {
                LOGGER.info("Resource {} was filtered by pack {}", id, string);
            }

            return list.stream().map(FallbackResourceManager.SinglePackResourceThunkSupplier::create).toList();
        }
    }

    @Override
    public Map<ResourceLocation, Resource> listResources(String startingPath, Predicate<ResourceLocation> allowedPathPredicate) {
        Object2IntMap<ResourceLocation> object2IntMap = new Object2IntOpenHashMap<>();
        int i = this.fallbacks.size();

        for(int j = 0; j < i; ++j) {
            FallbackResourceManager.PackEntry packEntry = this.fallbacks.get(j);
            packEntry.filterAll(object2IntMap.keySet());
            if (packEntry.resources != null) {
                for(ResourceLocation resourceLocation : packEntry.resources.getResources(this.type, this.namespace, startingPath, allowedPathPredicate)) {
                    object2IntMap.put(resourceLocation, j);
                }
            }
        }

        Map<ResourceLocation, Resource> map = Maps.newTreeMap();

        for(Object2IntMap.Entry<ResourceLocation> entry : Object2IntMaps.fastIterable(object2IntMap)) {
            int k = entry.getIntValue();
            ResourceLocation resourceLocation2 = entry.getKey();
            PackResources packResources = (this.fallbacks.get(k)).resources;
            map.put(resourceLocation2, new Resource(packResources.getName(), this.createResourceGetter(resourceLocation2, packResources), this.createStackMetadataFinder(resourceLocation2, k)));
        }

        return map;
    }

    private Resource.IoSupplier<ResourceMetadata> createStackMetadataFinder(ResourceLocation id, int index) {
        return () -> {
            ResourceLocation resourceLocation2 = getMetadataLocation(id);

            for(int j = this.fallbacks.size() - 1; j >= index; --j) {
                FallbackResourceManager.PackEntry packEntry = this.fallbacks.get(j);
                PackResources packResources = packEntry.resources;
                if (packResources != null && packResources.hasResource(this.type, resourceLocation2)) {
                    InputStream inputStream = packResources.getResource(this.type, resourceLocation2);

                    ResourceMetadata var8;
                    try {
                        var8 = ResourceMetadata.fromJsonStream(inputStream);
                    } catch (Throwable var11) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Throwable var10) {
                                var11.addSuppressed(var10);
                            }
                        }

                        throw var11;
                    }

                    if (inputStream != null) {
                        inputStream.close();
                    }

                    return var8;
                }

                if (packEntry.isFiltered(resourceLocation2)) {
                    break;
                }
            }

            return ResourceMetadata.EMPTY;
        };
    }

    private static void applyPackFiltersToExistingResources(FallbackResourceManager.PackEntry pack, Map<ResourceLocation, FallbackResourceManager.EntryStack> idToEntryList) {
        Iterator<Map.Entry<ResourceLocation, FallbackResourceManager.EntryStack>> iterator = idToEntryList.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<ResourceLocation, FallbackResourceManager.EntryStack> entry = iterator.next();
            ResourceLocation resourceLocation = entry.getKey();
            FallbackResourceManager.EntryStack entryStack = entry.getValue();
            if (pack.isFiltered(resourceLocation)) {
                iterator.remove();
            } else if (pack.isFiltered(entryStack.metadataLocation())) {
                entryStack.entries.forEach(FallbackResourceManager.SinglePackResourceThunkSupplier::ignoreMeta);
            }
        }

    }

    private void listPackResources(FallbackResourceManager.PackEntry pack, String startingPath, Predicate<ResourceLocation> allowedPathPredicate, Map<ResourceLocation, FallbackResourceManager.EntryStack> idToEntryList) {
        PackResources packResources = pack.resources;
        if (packResources != null) {
            for(ResourceLocation resourceLocation : packResources.getResources(this.type, this.namespace, startingPath, allowedPathPredicate)) {
                ResourceLocation resourceLocation2 = getMetadataLocation(resourceLocation);
                idToEntryList.computeIfAbsent(resourceLocation, (id) -> {
                    return new FallbackResourceManager.EntryStack(resourceLocation2, Lists.newArrayList());
                }).entries().add(new FallbackResourceManager.SinglePackResourceThunkSupplier(resourceLocation, resourceLocation2, packResources));
            }

        }
    }

    @Override
    public Map<ResourceLocation, List<Resource>> listResourceStacks(String startingPath, Predicate<ResourceLocation> allowedPathPredicate) {
        Map<ResourceLocation, FallbackResourceManager.EntryStack> map = Maps.newHashMap();

        for(FallbackResourceManager.PackEntry packEntry : this.fallbacks) {
            applyPackFiltersToExistingResources(packEntry, map);
            this.listPackResources(packEntry, startingPath, allowedPathPredicate, map);
        }

        TreeMap<ResourceLocation, List<Resource>> treeMap = Maps.newTreeMap();
        map.forEach((id, entryList) -> {
            treeMap.put(id, entryList.createThunks());
        });
        return treeMap;
    }

    @Override
    public Stream<PackResources> listPacks() {
        return this.fallbacks.stream().map((pack) -> {
            return pack.resources;
        }).filter(Objects::nonNull);
    }

    static ResourceLocation getMetadataLocation(ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), id.getPath() + ".mcmeta");
    }

    static record EntryStack(ResourceLocation metadataLocation, List<FallbackResourceManager.SinglePackResourceThunkSupplier> entries) {
        List<Resource> createThunks() {
            return this.entries().stream().map(FallbackResourceManager.SinglePackResourceThunkSupplier::create).toList();
        }
    }

    static class LeakedResourceWarningInputStream extends FilterInputStream {
        private final String message;
        private boolean closed;

        public LeakedResourceWarningInputStream(InputStream parent, ResourceLocation id, String packName) {
            super(parent);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            (new Exception()).printStackTrace(new PrintStream(byteArrayOutputStream));
            this.message = "Leaked resource: '" + id + "' loaded from pack: '" + packName + "'\n" + byteArrayOutputStream;
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.closed = true;
        }

        @Override
        protected void finalize() throws Throwable {
            if (!this.closed) {
                FallbackResourceManager.LOGGER.warn(this.message);
            }

            super.finalize();
        }
    }

    static record PackEntry(String name, @Nullable PackResources resources, @Nullable Predicate<ResourceLocation> filter) {
        public void filterAll(Collection<ResourceLocation> ids) {
            if (this.filter != null) {
                ids.removeIf(this.filter);
            }

        }

        public boolean isFiltered(ResourceLocation id) {
            return this.filter != null && this.filter.test(id);
        }
    }

    class SinglePackResourceThunkSupplier {
        private final ResourceLocation location;
        private final ResourceLocation metadataLocation;
        private final PackResources source;
        private boolean shouldGetMeta = true;

        SinglePackResourceThunkSupplier(ResourceLocation id, ResourceLocation metadataId, PackResources pack) {
            this.source = pack;
            this.location = id;
            this.metadataLocation = metadataId;
        }

        public void ignoreMeta() {
            this.shouldGetMeta = false;
        }

        public Resource create() {
            String string = this.source.getName();
            return this.shouldGetMeta ? new Resource(string, FallbackResourceManager.this.createResourceGetter(this.location, this.source), () -> {
                if (this.source.hasResource(FallbackResourceManager.this.type, this.metadataLocation)) {
                    InputStream inputStream = this.source.getResource(FallbackResourceManager.this.type, this.metadataLocation);

                    ResourceMetadata var2;
                    try {
                        var2 = ResourceMetadata.fromJsonStream(inputStream);
                    } catch (Throwable var5) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Throwable var4) {
                                var5.addSuppressed(var4);
                            }
                        }

                        throw var5;
                    }

                    if (inputStream != null) {
                        inputStream.close();
                    }

                    return var2;
                } else {
                    return ResourceMetadata.EMPTY;
                }
            }) : new Resource(string, FallbackResourceManager.this.createResourceGetter(this.location, this.source));
        }
    }
}
