package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record MangroveRootPlacement(HolderSet<Block> canGrowThrough, HolderSet<Block> muddyRootsIn, BlockStateProvider muddyRootsProvider, int maxRootWidth, int maxRootLength, float randomSkewChance) {
    public static final Codec<MangroveRootPlacement> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(RegistryCodecs.homogeneousList(Registry.BLOCK_REGISTRY).fieldOf("can_grow_through").forGetter((rootPlacement) -> {
            return rootPlacement.canGrowThrough;
        }), RegistryCodecs.homogeneousList(Registry.BLOCK_REGISTRY).fieldOf("muddy_roots_in").forGetter((rootPlacement) -> {
            return rootPlacement.muddyRootsIn;
        }), BlockStateProvider.CODEC.fieldOf("muddy_roots_provider").forGetter((rootPlacement) -> {
            return rootPlacement.muddyRootsProvider;
        }), Codec.intRange(1, 12).fieldOf("max_root_width").forGetter((rootPlacement) -> {
            return rootPlacement.maxRootWidth;
        }), Codec.intRange(1, 64).fieldOf("max_root_length").forGetter((rootPlacement) -> {
            return rootPlacement.maxRootLength;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("random_skew_chance").forGetter((rootPlacement) -> {
            return rootPlacement.randomSkewChance;
        })).apply(instance, MangroveRootPlacement::new);
    });
}
