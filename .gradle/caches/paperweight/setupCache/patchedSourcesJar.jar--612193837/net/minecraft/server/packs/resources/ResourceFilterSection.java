package net.minecraft.server.packs.resources;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.ExtraCodecs;
import org.slf4j.Logger;

public class ResourceFilterSection {
    static final Logger LOGGER = LogUtils.getLogger();
    static final Codec<ResourceFilterSection> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.list(ResourceFilterSection.ResourceLocationPattern.CODEC).fieldOf("block").forGetter((filter) -> {
            return filter.blockList;
        })).apply(instance, ResourceFilterSection::new);
    });
    public static final MetadataSectionSerializer<ResourceFilterSection> SERIALIZER = new MetadataSectionSerializer<ResourceFilterSection>() {
        @Override
        public String getMetadataSectionName() {
            return "filter";
        }

        @Override
        public ResourceFilterSection fromJson(JsonObject jsonObject) {
            return ResourceFilterSection.CODEC.parse(JsonOps.INSTANCE, jsonObject).getOrThrow(false, ResourceFilterSection.LOGGER::error);
        }
    };
    private final List<ResourceFilterSection.ResourceLocationPattern> blockList;

    public ResourceFilterSection(List<ResourceFilterSection.ResourceLocationPattern> blocks) {
        this.blockList = List.copyOf(blocks);
    }

    public boolean isNamespaceFiltered(String namespace) {
        return this.blockList.stream().anyMatch((block) -> {
            return block.namespacePredicate.test(namespace);
        });
    }

    public boolean isPathFiltered(String namespace) {
        return this.blockList.stream().anyMatch((block) -> {
            return block.pathPredicate.test(namespace);
        });
    }

    static class ResourceLocationPattern implements Predicate<ResourceLocation> {
        static final Codec<ResourceFilterSection.ResourceLocationPattern> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(ExtraCodecs.PATTERN.optionalFieldOf("namespace").forGetter((entry) -> {
                return entry.namespacePattern;
            }), ExtraCodecs.PATTERN.optionalFieldOf("path").forGetter((entry) -> {
                return entry.pathPattern;
            })).apply(instance, ResourceFilterSection.ResourceLocationPattern::new);
        });
        private final Optional<Pattern> namespacePattern;
        final Predicate<String> namespacePredicate;
        private final Optional<Pattern> pathPattern;
        final Predicate<String> pathPredicate;

        private ResourceLocationPattern(Optional<Pattern> namespace, Optional<Pattern> path) {
            this.namespacePattern = namespace;
            this.namespacePredicate = namespace.map(Pattern::asPredicate).orElse((namespace_) -> {
                return true;
            });
            this.pathPattern = path;
            this.pathPredicate = path.map(Pattern::asPredicate).orElse((path_) -> {
                return true;
            });
        }

        @Override
        public boolean test(ResourceLocation resourceLocation) {
            return this.namespacePredicate.test(resourceLocation.getNamespace()) && this.pathPredicate.test(resourceLocation.getPath());
        }
    }
}
