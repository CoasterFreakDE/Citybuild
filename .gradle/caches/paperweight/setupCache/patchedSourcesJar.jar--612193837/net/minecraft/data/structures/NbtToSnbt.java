package net.minecraft.data.structures;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.slf4j.Logger;

public class NbtToSnbt implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DataGenerator generator;

    public NbtToSnbt(DataGenerator root) {
        this.generator = root;
    }

    @Override
    public void run(CachedOutput writer) throws IOException {
        Path path = this.generator.getOutputFolder();

        for(Path path2 : this.generator.getInputFolders()) {
            Files.walk(path2).filter((pathx) -> {
                return pathx.toString().endsWith(".nbt");
            }).forEach((pathx) -> {
                convertStructure(writer, pathx, this.getName(path2, pathx), path);
            });
        }

    }

    @Override
    public String getName() {
        return "NBT to SNBT";
    }

    private String getName(Path targetPath, Path rootPath) {
        String string = targetPath.relativize(rootPath).toString().replaceAll("\\\\", "/");
        return string.substring(0, string.length() - ".nbt".length());
    }

    @Nullable
    public static Path convertStructure(CachedOutput writer, Path inputPath, String filename, Path outputPath) {
        try {
            InputStream inputStream = Files.newInputStream(inputPath);

            Path var6;
            try {
                Path path = outputPath.resolve(filename + ".snbt");
                writeSnbt(writer, path, NbtUtils.structureToSnbt(NbtIo.readCompressed(inputStream)));
                LOGGER.info("Converted {} from NBT to SNBT", (Object)filename);
                var6 = path;
            } catch (Throwable var8) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                    }
                }

                throw var8;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return var6;
        } catch (IOException var9) {
            LOGGER.error("Couldn't convert {} from NBT to SNBT at {}", filename, inputPath, var9);
            return null;
        }
    }

    public static void writeSnbt(CachedOutput writer, Path path, String content) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), byteArrayOutputStream);
        hashingOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        hashingOutputStream.write(10);
        writer.writeIfNeeded(path, byteArrayOutputStream.toByteArray(), hashingOutputStream.hash());
    }
}
