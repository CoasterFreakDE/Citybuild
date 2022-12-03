package net.minecraft.core;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface HolderLookup<T> {
    Optional<Holder<T>> get(ResourceKey<T> key);

    Stream<ResourceKey<T>> listElements();

    Optional<? extends HolderSet<T>> get(TagKey<T> tag);

    Stream<TagKey<T>> listTags();

    static <T> HolderLookup<T> forRegistry(Registry<T> registry) {
        return new HolderLookup.RegistryLookup<>(registry);
    }

    public static class RegistryLookup<T> implements HolderLookup<T> {
        protected final Registry<T> registry;

        public RegistryLookup(Registry<T> registry) {
            this.registry = registry;
        }

        @Override
        public Optional<Holder<T>> get(ResourceKey<T> key) {
            return this.registry.getHolder(key);
        }

        @Override
        public Stream<ResourceKey<T>> listElements() {
            return this.registry.entrySet().stream().map(Map.Entry::getKey);
        }

        @Override
        public Optional<? extends HolderSet<T>> get(TagKey<T> tag) {
            return this.registry.getTag(tag);
        }

        @Override
        public Stream<TagKey<T>> listTags() {
            return this.registry.getTagNames();
        }
    }
}
