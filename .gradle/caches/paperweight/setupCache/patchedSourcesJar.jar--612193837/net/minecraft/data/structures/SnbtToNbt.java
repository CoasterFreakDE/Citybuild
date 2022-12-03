package net.minecraft.data.structures;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtToNbt implements DataProvider {
    @Nullable
    private static final Path DUMP_SNBT_TO = null;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DataGenerator generator;
    private final List<SnbtToNbt.Filter> filters = Lists.newArrayList();

    public SnbtToNbt(DataGenerator generator) {
        this.generator = generator;
    }

    public SnbtToNbt addFilter(SnbtToNbt.Filter tweaker) {
        this.filters.add(tweaker);
        return this;
    }

    private CompoundTag applyFilters(String key, CompoundTag compound) {
        CompoundTag compoundTag = compound;

        for(SnbtToNbt.Filter filter : this.filters) {
            compoundTag = filter.apply(key, compoundTag);
        }

        return compoundTag;
    }

    @Override
    public void run(CachedOutput writer) throws IOException {
        Path path = this.generator.getOutputFolder();
        List<CompletableFuture<SnbtToNbt.TaskResult>> list = Lists.newArrayList();

        for(Path path2 : this.generator.getInputFolders()) {
            Files.walk(path2).filter((pathx) -> {
                return pathx.toString().endsWith(".snbt");
            }).forEach((pathx) -> {
                list.add(CompletableFuture.supplyAsync(() -> {
                    return this.readStructure(pathx, this.getName(path2, pathx));
                }, Util.backgroundExecutor()));
            });
        }

        boolean bl = false;

        for(CompletableFuture<SnbtToNbt.TaskResult> completableFuture : list) {
            try {
                this.storeStructureIfChanged(writer, completableFuture.get(), path);
            } catch (Exception var8) {
                LOGGER.error("Failed to process structure", (Throwable)var8);
                bl = true;
            }
        }

        if (bl) {
            throw new IllegalStateException("Failed to convert all structures, aborting");
        }
    }

    @Override
    public String getName() {
        return "SNBT -> NBT";
    }

    private String getName(Path root, Path file) {
        String string = root.relativize(file).toString().replaceAll("\\\\", "/");
        return string.substring(0, string.length() - ".snbt".length());
    }

    private SnbtToNbt.TaskResult readStructure(Path path, String name) {
        try {
            BufferedReader bufferedReader = Files.newBufferedReader(path);

            SnbtToNbt.TaskResult var11;
            try {
                String string = IOUtils.toString((Reader)bufferedReader);
                CompoundTag compoundTag = this.applyFilters(name, NbtUtils.snbtToStructure(string));
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), byteArrayOutputStream);
                NbtIo.writeCompressed(compoundTag, hashingOutputStream);
                byte[] bs = byteArrayOutputStream.toByteArray();
                HashCode hashCode = hashingOutputStream.hash();
                String string2;
                if (DUMP_SNBT_TO != null) {
                    string2 = NbtUtils.structureToSnbt(compoundTag);
                } else {
                    string2 = null;
                }

                var11 = new SnbtToNbt.TaskResult(name, bs, string2, hashCode);
            } catch (Throwable var13) {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Throwable var12) {
                        var13.addSuppressed(var12);
                    }
                }

                throw var13;
            }

            if (bufferedReader != null) {
                bufferedReader.close();
            }

            return var11;
        } catch (Throwable var14) {
            throw new SnbtToNbt.StructureConversionException(path, var14);
        }
    }

    private void storeStructureIfChanged(CachedOutput cache, SnbtToNbt.TaskResult data, Path root) {
        if (data.snbtPayload != null) {
            Path path = DUMP_SNBT_TO.resolve(data.name + ".snbt");

            try {
                NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, path, data.snbtPayload);
            } catch (IOException var7) {
                LOGGER.error("Couldn't write structure SNBT {} at {}", data.name, path, var7);
            }
        }

        Path path2 = root.resolve(data.name + ".nbt");

        try {
            cache.writeIfNeeded(path2, data.payload, data.hash);
        } catch (IOException var6) {
            LOGGER.error("Couldn't write structure {} at {}", data.name, path2, var6);
        }

    }

    @FunctionalInterface
    public interface Filter {
        CompoundTag apply(String name, CompoundTag nbt);
    }

    static class StructureConversionException extends RuntimeException {
        public StructureConversionException(Path path, Throwable cause) {
            super(path.toAbsolutePath().toString(), cause);
        }
    }

    static record TaskResult(String name, byte[] payload, @Nullable String snbtPayload, HashCode hash) {
    }
}
