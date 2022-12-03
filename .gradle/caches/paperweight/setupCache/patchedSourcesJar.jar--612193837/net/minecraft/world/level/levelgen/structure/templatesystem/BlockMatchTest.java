package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMatchTest extends RuleTest {
    public static final Codec<BlockMatchTest> CODEC = Registry.BLOCK.byNameCodec().fieldOf("block").xmap(BlockMatchTest::new, (ruleTest) -> {
        return ruleTest.block;
    }).codec();
    private final Block block;

    public BlockMatchTest(Block block) {
        this.block = block;
    }

    @Override
    public boolean test(BlockState state, RandomSource random) {
        return state.is(this.block);
    }

    @Override
    protected RuleTestType<?> getType() {
        return RuleTestType.BLOCK_TEST;
    }
}
