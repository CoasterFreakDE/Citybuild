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

    default Resource getResourceOrThrow(ResourceLocation id) throws FileNotFoundException {
        return this.getResource(id).orElseThrow(() -> {
            return new FileNotFoundException(id.toString());
        });
    }

    default InputStream open(ResourceLocation id) throws IOException {
        return this.getResourceOrThrow(id).open();
    }

    default BufferedReader openAsReader(ResourceLocation id) throws IOException {
        return this.getResourceOrThrow(id).openAsReader();
    }
}
