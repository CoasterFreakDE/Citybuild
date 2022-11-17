package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class ChunkStorage implements AutoCloseable {
    public static final int LAST_MONOLYTH_STRUCTURE_DATA_VERSION = 1493;
    private final IOWorker worker;
    protected final DataFixer fixerUpper;
    @Nullable
    private volatile LegacyStructureDataHandler legacyStructureHandler;

    public ChunkStorage(Path directory, DataFixer dataFixer, boolean dsync) {
        this.fixerUpper = dataFixer;
        this.worker = new IOWorker(directory, dsync, "chunk");
    }

    public boolean isOldChunkAround(ChunkPos chunkPos, int checkRadius) {
        return this.worker.isOldChunkAround(chunkPos, checkRadius);
    }

    public CompoundTag upgradeChunkTag(ResourceKey<Level> worldKey, Supplier<DimensionDataStorage> persistentStateManagerFactory, CompoundTag nbt, Optional<ResourceKey<Codec<? extends ChunkGenerator>>> generatorCodecKey) {
        int i = getVersion(nbt);
        if (i < 1493) {
            nbt = NbtUtils.update(this.fixerUpper, DataFixTypes.CHUNK, nbt, i, 1493);
            if (nbt.getCompound("Level").getBoolean("hasLegacyStructureData")) {
                LegacyStructureDataHandler legacyStructureDataHandler = this.getLegacyStructureHandler(worldKey, persistentStateManagerFactory);
                nbt = legacyStructureDataHandler.updateFromLegacy(nbt);
            }
        }

        injectDatafixingContext(nbt, worldKey, generatorCodecKey);
        nbt = NbtUtils.update(this.fixerUpper, DataFixTypes.CHUNK, nbt, Math.max(1493, i));
        if (i < SharedConstants.getCurrentVersion().getWorldVersion()) {
            nbt.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        }

        nbt.remove("__context");
        return nbt;
    }

    private LegacyStructureDataHandler getLegacyStructureHandler(ResourceKey<Level> resourceKey, Supplier<DimensionDataStorage> supplier) {
        LegacyStructureDataHandler legacyStructureDataHandler = this.legacyStructureHandler;
        if (legacyStructureDataHandler == null) {
            synchronized(this) {
                legacyStructureDataHandler = this.legacyStructureHandler;
                if (legacyStructureDataHandler == null) {
                    this.legacyStructureHandler = legacyStructureDataHandler = LegacyStructureDataHandler.getLegacyStructureHandler(resourceKey, supplier.get());
                }
            }
        }

        return legacyStructureDataHandler;
    }

    public static void injectDatafixingContext(CompoundTag nbt, ResourceKey<Level> worldKey, Optional<ResourceKey<Codec<? extends ChunkGenerator>>> generatorCodecKey) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("dimension", worldKey.location().toString());
        generatorCodecKey.ifPresent((key) -> {
            compoundTag.putString("generator", key.location().toString());
        });
        nbt.put("__context", compoundTag);
    }

    public static int getVersion(CompoundTag nbt) {
        return nbt.contains("DataVersion", 99) ? nbt.getInt("DataVersion") : -1;
    }

    public CompletableFuture<Optional<CompoundTag>> read(ChunkPos chunkPos) {
        return this.worker.loadAsync(chunkPos);
    }

    public void write(ChunkPos chunkPos, CompoundTag nbt) {
        this.worker.store(chunkPos, nbt);
        if (this.legacyStructureHandler != null) {
            this.legacyStructureHandler.removeIndex(chunkPos.toLong());
        }

    }

    public void flushWorker() {
        this.worker.synchronize(true).join();
    }

    @Override
    public void close() throws IOException {
        this.worker.close();
    }

    public ChunkScanAccess chunkScanner() {
        return this.worker;
    }
}
