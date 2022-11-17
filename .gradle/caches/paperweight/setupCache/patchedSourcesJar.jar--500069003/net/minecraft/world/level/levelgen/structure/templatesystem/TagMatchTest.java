package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TagMatchTest extends RuleTest {
    public static final Codec<TagMatchTest> CODEC = TagKey.codec(Registry.BLOCK_REGISTRY).fieldOf("tag").xmap(TagMatchTest::new, (ruleTest) -> {
        return ruleTest.tag;
    }).codec();
    private final TagKey<Block> tag;

    public TagMatchTest(TagKey<Block> tag) {
        this.tag = tag;
    }

    @Override
    public boolean test(BlockState state, RandomSource random) {
        return state.is(this.tag);
    }

    @Override
    protected RuleTestType<?> getType() {
        return RuleTestType.TAG_TEST;
    }
}
