package net.minecraft.server.packs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;

public class ResourcePackFileNotFoundException extends FileNotFoundException {
    public ResourcePackFileNotFoundException(File packSource, String resource) {
        super(String.format(Locale.ROOT, "'%s' in ResourcePack '%s'", resource, packSource));
    }
}
