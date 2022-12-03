package net.minecraft.data;

import com.google.common.hash.HashCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface CachedOutput {
    CachedOutput NO_CACHE = (path, data, hashCode) -> {
        Files.createDirectories(path.getParent());
        Files.write(path, data);
    };

    void writeIfNeeded(Path path, byte[] data, HashCode hashCode) throws IOException;
}
