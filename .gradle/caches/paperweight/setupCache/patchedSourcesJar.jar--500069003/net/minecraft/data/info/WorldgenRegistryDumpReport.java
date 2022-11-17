package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public class WorldgenRegistryDumpReport implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DataGenerator generator;

    public WorldgenRegistryDumpReport(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(CachedOutput writer) {
        RegistryAccess registryAccess = RegistryAccess.BUILTIN.get();
        DynamicOps<JsonElement> dynamicOps = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        RegistryAccess.knownRegistries().forEach((info) -> {
            this.dumpRegistryCap(writer, registryAccess, dynamicOps, info);
        });
    }

    private <T> void dumpRegistryCap(CachedOutput cachedOutput, RegistryAccess registryAccess, DynamicOps<JsonElement> dynamicOps, RegistryAccess.RegistryData<T> registryData) {
        ResourceKey<? extends Registry<T>> resourceKey = registryData.key();
        Registry<T> registry = registryAccess.ownedRegistryOrThrow(resourceKey);
        DataGenerator.PathProvider pathProvider = this.generator.createPathProvider(DataGenerator.Target.REPORTS, resourceKey.location().getPath());

        for(Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
            dumpValue(pathProvider.json(entry.getKey().location()), cachedOutput, dynamicOps, registryData.codec(), entry.getValue());
        }

    }

    private static <E> void dumpValue(Path path, CachedOutput cache, DynamicOps<JsonElement> json, Encoder<E> encoder, E value) {
        try {
            Optional<JsonElement> optional = encoder.encodeStart(json, value).resultOrPartial((string) -> {
                LOGGER.error("Couldn't serialize element {}: {}", path, string);
            });
            if (optional.isPresent()) {
                DataProvider.saveStable(cache, optional.get(), path);
            }
        } catch (IOException var6) {
            LOGGER.error("Couldn't save element {}", path, var6);
        }

    }

    @Override
    public String getName() {
        return "Worldgen";
    }
}
