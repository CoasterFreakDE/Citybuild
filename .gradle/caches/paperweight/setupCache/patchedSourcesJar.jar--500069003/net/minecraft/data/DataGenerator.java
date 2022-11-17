package net.minecraft.data;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.WorldVersion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.slf4j.Logger;

public class DataGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Collection<Path> inputFolders;
    private final Path outputFolder;
    private final List<DataProvider> allProviders = Lists.newArrayList();
    private final List<DataProvider> providersToRun = Lists.newArrayList();
    private final WorldVersion version;
    private final boolean alwaysGenerate;

    public DataGenerator(Path output, Collection<Path> inputs, WorldVersion gameVersion, boolean ignoreCache) {
        this.outputFolder = output;
        this.inputFolders = inputs;
        this.version = gameVersion;
        this.alwaysGenerate = ignoreCache;
    }

    public Collection<Path> getInputFolders() {
        return this.inputFolders;
    }

    public Path getOutputFolder() {
        return this.outputFolder;
    }

    public Path getOutputFolder(DataGenerator.Target outputType) {
        return this.getOutputFolder().resolve(outputType.directory);
    }

    public void run() throws IOException {
        HashCache hashCache = new HashCache(this.outputFolder, this.allProviders, this.version);
        Stopwatch stopwatch = Stopwatch.createStarted();
        Stopwatch stopwatch2 = Stopwatch.createUnstarted();

        for(DataProvider dataProvider : this.providersToRun) {
            if (!this.alwaysGenerate && !hashCache.shouldRunInThisVersion(dataProvider)) {
                LOGGER.debug("Generator {} already run for version {}", dataProvider.getName(), this.version.getName());
            } else {
                LOGGER.info("Starting provider: {}", (Object)dataProvider.getName());
                stopwatch2.start();
                dataProvider.run(hashCache.getUpdater(dataProvider));
                stopwatch2.stop();
                LOGGER.info("{} finished after {} ms", dataProvider.getName(), stopwatch2.elapsed(TimeUnit.MILLISECONDS));
                stopwatch2.reset();
            }
        }

        LOGGER.info("All providers took: {} ms", (long)stopwatch.elapsed(TimeUnit.MILLISECONDS));
        hashCache.purgeStaleAndWrite();
    }

    public void addProvider(boolean shouldRun, DataProvider provider) {
        if (shouldRun) {
            this.providersToRun.add(provider);
        }

        this.allProviders.add(provider);
    }

    public DataGenerator.PathProvider createPathProvider(DataGenerator.Target outputType, String directoryName) {
        return new DataGenerator.PathProvider(this, outputType, directoryName);
    }

    static {
        Bootstrap.bootStrap();
    }

    public static class PathProvider {
        private final Path root;
        private final String kind;

        PathProvider(DataGenerator dataGenerator, DataGenerator.Target outputType, String directoryName) {
            this.root = dataGenerator.getOutputFolder(outputType);
            this.kind = directoryName;
        }

        public Path file(ResourceLocation id, String fileExtension) {
            return this.root.resolve(id.getNamespace()).resolve(this.kind).resolve(id.getPath() + "." + fileExtension);
        }

        public Path json(ResourceLocation id) {
            return this.root.resolve(id.getNamespace()).resolve(this.kind).resolve(id.getPath() + ".json");
        }
    }

    public static enum Target {
        DATA_PACK("data"),
        RESOURCE_PACK("assets"),
        REPORTS("reports");

        final String directory;

        private Target(String path) {
            this.directory = path;
        }
    }
}
