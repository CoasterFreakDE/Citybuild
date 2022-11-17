package net.minecraft.data;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.WorldVersion;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

public class HashCache {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final String HEADER_MARKER = "// ";
    private final Path rootDir;
    private final Path cacheDir;
    private final String versionId;
    private final Map<DataProvider, HashCache.ProviderCache> existingCaches;
    private final Map<DataProvider, HashCache.CacheUpdater> cachesToWrite = new HashMap<>();
    private final Set<Path> cachePaths = new HashSet<>();
    private final int initialCount;

    private Path getProviderCachePath(DataProvider dataProvider) {
        return this.cacheDir.resolve(Hashing.sha1().hashString(dataProvider.getName(), StandardCharsets.UTF_8).toString());
    }

    public HashCache(Path root, List<DataProvider> dataProviders, WorldVersion gameVersion) throws IOException {
        this.versionId = gameVersion.getName();
        this.rootDir = root;
        this.cacheDir = root.resolve(".cache");
        Files.createDirectories(this.cacheDir);
        Map<DataProvider, HashCache.ProviderCache> map = new HashMap<>();
        int i = 0;

        for(DataProvider dataProvider : dataProviders) {
            Path path = this.getProviderCachePath(dataProvider);
            this.cachePaths.add(path);
            HashCache.ProviderCache providerCache = readCache(root, path);
            map.put(dataProvider, providerCache);
            i += providerCache.count();
        }

        this.existingCaches = map;
        this.initialCount = i;
    }

    private static HashCache.ProviderCache readCache(Path root, Path dataProviderPath) {
        if (Files.isReadable(dataProviderPath)) {
            try {
                return HashCache.ProviderCache.load(root, dataProviderPath);
            } catch (Exception var3) {
                LOGGER.warn("Failed to parse cache {}, discarding", dataProviderPath, var3);
            }
        }

        return new HashCache.ProviderCache("unknown");
    }

    public boolean shouldRunInThisVersion(DataProvider dataProvider) {
        HashCache.ProviderCache providerCache = this.existingCaches.get(dataProvider);
        return providerCache == null || !providerCache.version.equals(this.versionId);
    }

    public CachedOutput getUpdater(DataProvider dataProvider) {
        return this.cachesToWrite.computeIfAbsent(dataProvider, (provider) -> {
            HashCache.ProviderCache providerCache = this.existingCaches.get(provider);
            if (providerCache == null) {
                throw new IllegalStateException("Provider not registered: " + provider.getName());
            } else {
                HashCache.CacheUpdater cacheUpdater = new HashCache.CacheUpdater(this.versionId, providerCache);
                this.existingCaches.put(provider, cacheUpdater.newCache);
                return cacheUpdater;
            }
        });
    }

    public void purgeStaleAndWrite() throws IOException {
        MutableInt mutableInt = new MutableInt();
        this.cachesToWrite.forEach((dataProvider, writer) -> {
            Path path = this.getProviderCachePath(dataProvider);
            writer.newCache.save(this.rootDir, path, DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) + "\t" + dataProvider.getName());
            mutableInt.add(writer.writes);
        });
        Set<Path> set = new HashSet<>();
        this.existingCaches.values().forEach((cachedData) -> {
            set.addAll(cachedData.data().keySet());
        });
        set.add(this.rootDir.resolve("version.json"));
        MutableInt mutableInt2 = new MutableInt();
        MutableInt mutableInt3 = new MutableInt();
        Stream<Path> stream = Files.walk(this.rootDir);

        try {
            stream.forEach((path) -> {
                if (!Files.isDirectory(path)) {
                    if (!this.cachePaths.contains(path)) {
                        mutableInt2.increment();
                        if (!set.contains(path)) {
                            try {
                                Files.delete(path);
                            } catch (IOException var6) {
                                LOGGER.warn("Failed to delete file {}", path, var6);
                            }

                            mutableInt3.increment();
                        }
                    }
                }
            });
        } catch (Throwable var9) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable var8) {
                    var9.addSuppressed(var8);
                }
            }

            throw var9;
        }

        if (stream != null) {
            stream.close();
        }

        LOGGER.info("Caching: total files: {}, old count: {}, new count: {}, removed stale: {}, written: {}", mutableInt2, this.initialCount, set.size(), mutableInt3, mutableInt);
    }

    static class CacheUpdater implements CachedOutput {
        private final HashCache.ProviderCache oldCache;
        final HashCache.ProviderCache newCache;
        int writes;

        CacheUpdater(String versionName, HashCache.ProviderCache cachedData) {
            this.oldCache = cachedData;
            this.newCache = new HashCache.ProviderCache(versionName);
        }

        private boolean shouldWrite(Path path, HashCode hashCode) {
            return !Objects.equals(this.oldCache.get(path), hashCode) || !Files.exists(path);
        }

        @Override
        public void writeIfNeeded(Path path, byte[] data, HashCode hashCode) throws IOException {
            if (this.shouldWrite(path, hashCode)) {
                ++this.writes;
                Files.createDirectories(path.getParent());
                Files.write(path, data);
            }

            this.newCache.put(path, hashCode);
        }
    }

    static record ProviderCache(String version, Map<Path, HashCode> data) {
        ProviderCache(String version) {
            this(version, new HashMap<>());
        }

        @Nullable
        public HashCode get(Path path) {
            return this.data.get(path);
        }

        public void put(Path path, HashCode hashCode) {
            this.data.put(path, hashCode);
        }

        public int count() {
            return this.data.size();
        }

        public static HashCache.ProviderCache load(Path root, Path dataProviderPath) throws IOException {
            BufferedReader bufferedReader = Files.newBufferedReader(dataProviderPath, StandardCharsets.UTF_8);

            HashCache.ProviderCache var7;
            try {
                String string = bufferedReader.readLine();
                if (!string.startsWith("// ")) {
                    throw new IllegalStateException("Missing cache file header");
                }

                String[] strings = string.substring("// ".length()).split("\t", 2);
                String string2 = strings[0];
                Map<Path, HashCode> map = new HashMap<>();
                bufferedReader.lines().forEach((line) -> {
                    int i = line.indexOf(32);
                    map.put(root.resolve(line.substring(i + 1)), HashCode.fromString(line.substring(0, i)));
                });
                var7 = new HashCache.ProviderCache(string2, Map.copyOf(map));
            } catch (Throwable var9) {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                    }
                }

                throw var9;
            }

            if (bufferedReader != null) {
                bufferedReader.close();
            }

            return var7;
        }

        public void save(Path root, Path dataProviderPath, String description) {
            try {
                BufferedWriter bufferedWriter = Files.newBufferedWriter(dataProviderPath, StandardCharsets.UTF_8);

                try {
                    bufferedWriter.write("// ");
                    bufferedWriter.write(this.version);
                    bufferedWriter.write(9);
                    bufferedWriter.write(description);
                    bufferedWriter.newLine();

                    for(Map.Entry<Path, HashCode> entry : this.data.entrySet()) {
                        bufferedWriter.write(entry.getValue().toString());
                        bufferedWriter.write(32);
                        bufferedWriter.write(root.relativize(entry.getKey()).toString());
                        bufferedWriter.newLine();
                    }
                } catch (Throwable var8) {
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (Throwable var7) {
                            var8.addSuppressed(var7);
                        }
                    }

                    throw var8;
                }

                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (IOException var9) {
                HashCache.LOGGER.warn("Unable write cachefile {}: {}", dataProviderPath, var9);
            }

        }
    }
}
