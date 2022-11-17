package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class BannerPattern {
    final String hashname;

    public BannerPattern(String id) {
        this.hashname = id;
    }

    public static ResourceLocation location(ResourceKey<BannerPattern> pattern, boolean banner) {
        String string = banner ? "banner" : "shield";
        ResourceLocation resourceLocation = pattern.location();
        return new ResourceLocation(resourceLocation.getNamespace(), "entity/" + string + "/" + resourceLocation.getPath());
    }

    public String getHashname() {
        return this.hashname;
    }

    @Nullable
    public static Holder<BannerPattern> byHash(String id) {
        return Registry.BANNER_PATTERN.holders().filter((pattern) -> {
            return (pattern.value()).hashname.equals(id);
        }).findAny().orElse((Holder.Reference<BannerPattern>)null);
    }

    public static class Builder {
        private final List<Pair<Holder<BannerPattern>, DyeColor>> patterns = Lists.newArrayList();

        public BannerPattern.Builder addPattern(ResourceKey<BannerPattern> pattern, DyeColor color) {
            return this.addPattern(Registry.BANNER_PATTERN.getHolderOrThrow(pattern), color);
        }

        public BannerPattern.Builder addPattern(Holder<BannerPattern> pattern, DyeColor color) {
            return this.addPattern(Pair.of(pattern, color));
        }

        public BannerPattern.Builder addPattern(Pair<Holder<BannerPattern>, DyeColor> pattern) {
            this.patterns.add(pattern);
            return this;
        }

        public ListTag toListTag() {
            ListTag listTag = new ListTag();

            for(Pair<Holder<BannerPattern>, DyeColor> pair : this.patterns) {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putString("Pattern", (pair.getFirst().value()).hashname);
                compoundTag.putInt("Color", pair.getSecond().getId());
                listTag.add(compoundTag);
            }

            return listTag;
        }
    }
}
