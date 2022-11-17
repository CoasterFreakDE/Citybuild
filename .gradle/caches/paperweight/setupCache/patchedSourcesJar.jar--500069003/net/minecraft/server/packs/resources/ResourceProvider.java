package net.minecraft.server.packs.resources;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
public interface ResourceProvider {
    Optional<Resource> getResource(ResourceLocation id);

    default Resource getResourceOrThrow(ResourceLocation resourceLocation) throws FileNotFoundException {
        return this.getResource(resourceLocation).orElseThrow(() -> {
            return new FileNotFoundException(resourceLocation.toString());
        });
    }

    default InputStream open(ResourceLocation resourceLocation) throws IOException {
        return this.getResourceOrThrow(resourceLocation).open();
    }

    default BufferedReader openAsReader(ResourceLocation resourceLocation) throws IOException {
        return this.getResourceOrThrow(resourceLocation).openAsReader();
    }
}
