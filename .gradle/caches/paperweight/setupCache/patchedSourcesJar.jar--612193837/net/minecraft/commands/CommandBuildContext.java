package net.minecraft.commands;

import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public final class CommandBuildContext {
    private final RegistryAccess registryAccess;
    CommandBuildContext.MissingTagAccessPolicy missingTagAccessPolicy = CommandBuildContext.MissingTagAccessPolicy.FAIL;

    public CommandBuildContext(RegistryAccess dynamicRegistryManager) {
        this.registryAccess = dynamicRegistryManager;
    }

    public void missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy entryListCreationPolicy) {
        this.missingTagAccessPolicy = entryListCreationPolicy;
    }

    public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> registryRef) {
        return new HolderLookup.RegistryLookup<T>(this.registryAccess.registryOrThrow(registryRef)) {
            @Override
            public Optional<? extends HolderSet<T>> get(TagKey<T> tag) {
                Optional var10000;
                switch (CommandBuildContext.this.missingTagAccessPolicy) {
                    case FAIL:
                        var10000 = this.registry.getTag(tag);
                        break;
                    case CREATE_NEW:
                        var10000 = Optional.of(this.registry.getOrCreateTag(tag));
                        break;
                    case RETURN_EMPTY:
                        Optional<? extends HolderSet<T>> optional = this.registry.getTag(tag);
                        var10000 = Optional.of(optional.isPresent() ? optional.get() : HolderSet.direct());
                        break;
                    default:
                        throw new IncompatibleClassChangeError();
                }

                return var10000;
            }
        };
    }

    public static enum MissingTagAccessPolicy {
        CREATE_NEW,
        RETURN_EMPTY,
        FAIL;
    }
}
