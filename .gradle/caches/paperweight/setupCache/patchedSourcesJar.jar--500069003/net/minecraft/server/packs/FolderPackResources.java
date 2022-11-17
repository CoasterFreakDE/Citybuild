package net.minecraft.server.packs;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ResourceLocationException;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.slf4j.Logger;

public class FolderPackResources extends AbstractPackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ON_WINDOWS = Util.getPlatform() == Util.OS.WINDOWS;
    private static final CharMatcher BACKSLASH_MATCHER = CharMatcher.is('\\');

    public FolderPackResources(File base) {
        super(base);
    }

    public static boolean validatePath(File file, String filename) throws IOException {
        String string = file.getCanonicalPath();
        if (ON_WINDOWS) {
            string = BACKSLASH_MATCHER.replaceFrom(string, '/');
        }

        return string.endsWith(filename);
    }

    @Override
    protected InputStream getResource(String name) throws IOException {
        File file = this.getFile(name);
        if (file == null) {
            throw new ResourcePackFileNotFoundException(this.file, name);
        } else {
            return new FileInputStream(file);
        }
    }

    @Override
    protected boolean hasResource(String name) {
        return this.getFile(name) != null;
    }

    @Nullable
    private File getFile(String name) {
        try {
            File file = new File(this.file, name);
            if (file.isFile() && validatePath(file, name)) {
                return file;
            }
        } catch (IOException var3) {
        }

        return null;
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> set = Sets.newHashSet();
        File file = new File(this.file, type.getDirectory());
        File[] files = file.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
        if (files != null) {
            for(File file2 : files) {
                String string = getRelativePath(file, file2);
                if (string.equals(string.toLowerCase(Locale.ROOT))) {
                    set.add(string.substring(0, string.length() - 1));
                } else {
                    this.logWarning(string);
                }
            }
        }

        return set;
    }

    @Override
    public void close() {
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType type, String namespace, String prefix, Predicate<ResourceLocation> allowedPathPredicate) {
        File file = new File(this.file, type.getDirectory());
        List<ResourceLocation> list = Lists.newArrayList();
        this.listResources(new File(new File(file, namespace), prefix), namespace, list, prefix + "/", allowedPathPredicate);
        return list;
    }

    private void listResources(File file, String namespace, List<ResourceLocation> foundIds, String rootDirectory, Predicate<ResourceLocation> allowedPathPredicate) {
        File[] files = file.listFiles();
        if (files != null) {
            for(File file2 : files) {
                if (file2.isDirectory()) {
                    this.listResources(file2, namespace, foundIds, rootDirectory + file2.getName() + "/", allowedPathPredicate);
                } else if (!file2.getName().endsWith(".mcmeta")) {
                    try {
                        String string = rootDirectory + file2.getName();
                        ResourceLocation resourceLocation = ResourceLocation.tryBuild(namespace, string);
                        if (resourceLocation == null) {
                            LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", namespace, string);
                        } else if (allowedPathPredicate.test(resourceLocation)) {
                            foundIds.add(resourceLocation);
                        }
                    } catch (ResourceLocationException var13) {
                        LOGGER.error(var13.getMessage());
                    }
                }
            }
        }

    }
}
