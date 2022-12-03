package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class BlockStateMatchTest extends RuleTest {
    public static final Codec<BlockStateMatchTest> CODEC = BlockState.CODEC.fieldOf("block_state").xmap(BlockStateMatchTest::new, (ruleTest) -> {
        return ruleTest.blockState;
    }).codec();
    private final BlockState blockState;

    public BlockStateMatchTest(BlockState blockState) {
        this.blockState = blockState;
    }

    @Override
    public boolean test(BlockState state, RandomSource random) {
        return state == this.blockState;
    }

    @Override
    protected RuleTestType<?> getType() {
        return RuleTestType.BLOCKSTATE_TEST;
    }
}
