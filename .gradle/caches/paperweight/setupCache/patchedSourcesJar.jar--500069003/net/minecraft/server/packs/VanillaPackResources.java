package net.minecraft.server.packs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.slf4j.Logger;

public class VanillaPackResources implements PackResources {
    @Nullable
    public static Path generatedDir;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Class<?> clientObject;
    private static final Map<PackType, Path> ROOT_DIR_BY_TYPE = Util.make(() -> {
        synchronized(VanillaPackResources.class) {
            ImmutableMap.Builder<PackType, Path> builder = ImmutableMap.builder();

            for(PackType packType : PackType.values()) {
                String string = "/" + packType.getDirectory() + "/.mcassetsroot";
                URL uRL = VanillaPackResources.class.getResource(string);
                if (uRL == null) {
                    LOGGER.error("File {} does not exist in classpath", (Object)string);
                } else {
                    try {
                        URI uRI = uRL.toURI();
                        String string2 = uRI.getScheme();
                        if (!"jar".equals(string2) && !"file".equals(string2)) {
                            LOGGER.warn("Assets URL '{}' uses unexpected schema", (Object)uRI);
                        }

                        Path path = safeGetPath(uRI);
                        builder.put(packType, path.getParent());
                    } catch (Exception var12) {
                        LOGGER.error("Couldn't resolve path to vanilla assets", (Throwable)var12);
                    }
                }
            }

            return builder.build();
        }
    });
    public final PackMetadataSection packMetadata;
    public final Set<String> namespaces;

    private static Path safeGetPath(URI uri) throws IOException {
        try {
            return Paths.get(uri);
        } catch (FileSystemNotFoundException var3) {
        } catch (Throwable var4) {
            LOGGER.warn("Unable to get path for: {}", uri, var4);
        }

        try {
            FileSystems.newFileSystem(uri, Collections.emptyMap());
        } catch (FileSystemAlreadyExistsException var2) {
        }

        return Paths.get(uri);
    }

    public VanillaPackResources(PackMetadataSection metadata, String... namespaces) {
        this.packMetadata = metadata;
        this.namespaces = ImmutableSet.copyOf(namespaces);
    }

    @Override
    public InputStream getRootResource(String fileName) throws IOException {
        if (!fileName.contains("/") && !fileName.contains("\\")) {
            if (generatedDir != null) {
                Path path = generatedDir.resolve(fileName);
                if (Files.exists(path)) {
                    return Files.newInputStream(path);
                }
            }

            return this.getResourceAsStream(fileName);
        } else {
            throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
        }
    }

    @Override
    public InputStream getResource(PackType type, ResourceLocation id) throws IOException {
        InputStream inputStream = this.getResourceAsStream(type, id);
        if (inputStream != null) {
            return inputStream;
        } else {
            throw new FileNotFoundException(id.getPath());
        }
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType type, String namespace, String prefix, Predicate<ResourceLocation> allowedPathPredicate) {
        Set<ResourceLocation> set = Sets.newHashSet();
        if (generatedDir != null) {
            try {
                getResources(set, namespace, generatedDir.resolve(type.getDirectory()), prefix, allowedPathPredicate);
            } catch (IOException var12) {
            }

            if (type == PackType.CLIENT_RESOURCES) {
                Enumeration<URL> enumeration = null;

                try {
                    enumeration = clientObject.getClassLoader().getResources(type.getDirectory() + "/");
                } catch (IOException var11) {
                }

                while(enumeration != null && enumeration.hasMoreElements()) {
                    try {
                        URI uRI = enumeration.nextElement().toURI();
                        if ("file".equals(uRI.getScheme())) {
                            getResources(set, namespace, Paths.get(uRI), prefix, allowedPathPredicate);
                        }
                    } catch (IOException | URISyntaxException var10) {
                    }
                }
            }
        }

        try {
            Path path = ROOT_DIR_BY_TYPE.get(type);
            if (path != null) {
                getResources(set, namespace, path, prefix, allowedPathPredicate);
            } else {
                LOGGER.error("Can't access assets root for type: {}", (Object)type);
            }
        } catch (NoSuchFileException | FileNotFoundException var8) {
        } catch (IOException var9) {
            LOGGER.error("Couldn't get a list of all vanilla resources", (Throwable)var9);
        }

        return set;
    }

    private static void getResources(Collection<ResourceLocation> results, String namespace, Path root, String prefix, Predicate<ResourceLocation> allowedPathPredicate) throws IOException {
        Path path = root.resolve(namespace);
        Stream<Path> stream = Files.walk(path.resolve(prefix));

        try {
            stream.filter((pathx) -> {
                return !pathx.endsWith(".mcmeta") && Files.isRegularFile(pathx);
            }).mapMulti((pathx, consumer) -> {
                String string2 = path.relativize(pathx).toString().replaceAll("\\\\", "/");
                ResourceLocation resourceLocation = ResourceLocation.tryBuild(namespace, string2);
                if (resourceLocation == null) {
                    Util.logAndPauseIfInIde(String.format(Locale.ROOT, "Invalid path in datapack: %s:%s, ignoring", namespace, string2));
                } else {
                    consumer.accept(resourceLocation);
                }

            }).filter(allowedPathPredicate).forEach(results::add);
        } catch (Throwable var10) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable var9) {
                    var10.addSuppressed(var9);
                }
            }

            throw var10;
        }

        if (stream != null) {
            stream.close();
        }

    }

    @Nullable
    protected InputStream getResourceAsStream(PackType type, ResourceLocation id) {
        String string = createPath(type, id);
        if (generatedDir != null) {
            Path path = generatedDir.resolve(type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath());
            if (Files.exists(path)) {
                try {
                    return Files.newInputStream(path);
                } catch (IOException var7) {
                }
            }
        }

        try {
            URL uRL = VanillaPackResources.class.getResource(string);
            return isResourceUrlValid(string, uRL) ? uRL.openStream() : null;
        } catch (IOException var6) {
            return VanillaPackResources.class.getResourceAsStream(string);
        }
    }

    private static String createPath(PackType type, ResourceLocation id) {
        return "/" + type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath();
    }

    private static boolean isResourceUrlValid(String fileName, @Nullable URL url) throws IOException {
        return url != null && (url.getProtocol().equals("jar") || FolderPackResources.validatePath(new File(url.getFile()), fileName));
    }

    @Nullable
    protected InputStream getResourceAsStream(String path) {
        return VanillaPackResources.class.getResourceAsStream("/" + path);
    }

    @Override
    public boolean hasResource(PackType type, ResourceLocation id) {
        String string = createPath(type, id);
        if (generatedDir != null) {
            Path path = generatedDir.resolve(type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath());
            if (Files.exists(path)) {
                return true;
            }
        }

        try {
            URL uRL = VanillaPackResources.class.getResource(string);
            return isResourceUrlValid(string, uRL);
        } catch (IOException var5) {
            return false;
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return this.namespaces;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> metaReader) throws IOException {
        try {
            InputStream inputStream = this.getRootResource("pack.mcmeta");

            Object var4;
            label59: {
                try {
                    if (inputStream != null) {
                        T object = AbstractPackResources.getMetadataFromStream(metaReader, inputStream);
                        if (object != null) {
                            var4 = object;
                            break label59;
                        }
                    }
                } catch (Throwable var6) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable var5) {
                            var6.addSuppressed(var5);
                        }
                    }

                    throw var6;
                }

                if (inputStream != null) {
                    inputStream.close();
                }

                return (T)(metaReader == PackMetadataSection.SERIALIZER ? this.packMetadata : null);
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return (T)var4;
        } catch (FileNotFoundException | RuntimeException var7) {
            return (T)(metaReader == PackMetadataSection.SERIALIZER ? this.packMetadata : null);
        }
    }

    @Override
    public String getName() {
        return "Default";
    }

    @Override
    public void close() {
    }

    public ResourceProvider asProvider() {
        return (id) -> {
            return Optional.of(new Resource(this.getName(), () -> {
                return this.getResource(PackType.CLIENT_RESOURCES, id);
            }));
        };
    }
}
