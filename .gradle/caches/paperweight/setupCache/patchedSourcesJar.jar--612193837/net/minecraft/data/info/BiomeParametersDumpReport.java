package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.slf4j.Logger;

public class BiomeParametersDumpReport implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path topPath;

    public BiomeParametersDumpReport(DataGenerator dataGenerator) {
        this.topPath = dataGenerator.getOutputFolder(DataGenerator.Target.REPORTS).resolve("biome_parameters");
    }

    @Override
    public void run(CachedOutput writer) {
        RegistryAccess.Frozen frozen = RegistryAccess.BUILTIN.get();
        DynamicOps<JsonElement> dynamicOps = RegistryOps.create(JsonOps.INSTANCE, frozen);
        Registry<Biome> registry = frozen.registryOrThrow(Registry.BIOME_REGISTRY);
        MultiNoiseBiomeSource.Preset.getPresets().forEach((preset) -> {
            MultiNoiseBiomeSource multiNoiseBiomeSource = preset.getSecond().biomeSource(registry, false);
            dumpValue(this.createPath(preset.getFirst()), writer, dynamicOps, MultiNoiseBiomeSource.CODEC, multiNoiseBiomeSource);
        });
    }

    private static <E> void dumpValue(Path path, CachedOutput writer, DynamicOps<JsonElement> ops, Encoder<E> codec, E biomeSource) {
        try {
            Optional<JsonElement> optional = codec.encodeStart(ops, biomeSource).resultOrPartial((error) -> {
                LOGGER.error("Couldn't serialize element {}: {}", path, error);
            });
            if (optional.isPresent()) {
                DataProvider.saveStable(writer, optional.get(), path);
            }
        } catch (IOException var6) {
            LOGGER.error("Couldn't save element {}", path, var6);
        }

    }

    private Path createPath(ResourceLocation id) {
        return this.topPath.resolve(id.getNamespace()).resolve(id.getPath() + ".json");
    }

    @Override
    public String getName() {
        return "Biome Parameters";
    }
}
