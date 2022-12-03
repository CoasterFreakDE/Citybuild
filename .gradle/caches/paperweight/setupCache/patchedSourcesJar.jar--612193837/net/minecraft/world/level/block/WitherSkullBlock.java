package net.minecraft.world.level.block;

import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockMaterialPredicate;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.material.Material;

// CraftBukkit start
import org.bukkit.craftbukkit.v1_19_R1.util.BlockStateListPopulator;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
// CraftBukkit end

public class WitherSkullBlock extends SkullBlock {

    @Nullable
    private static BlockPattern witherPatternFull;
    @Nullable
    private static BlockPattern witherPatternBase;

    protected WitherSkullBlock(BlockBehaviour.Properties settings) {
        super(SkullBlock.Types.WITHER_SKELETON, settings);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        BlockEntity tileentity = world.getBlockEntity(pos);

        if (tileentity instanceof SkullBlockEntity) {
            WitherSkullBlock.checkSpawn(world, pos, (SkullBlockEntity) tileentity);
        }

    }

    public static void checkSpawn(Level world, BlockPos pos, SkullBlockEntity blockEntity) {
        if (world.captureBlockStates) return; // CraftBukkit
        if (!world.isClientSide) {
            BlockState iblockdata = blockEntity.getBlockState();
            boolean flag = iblockdata.is(Blocks.WITHER_SKELETON_SKULL) || iblockdata.is(Blocks.WITHER_SKELETON_WALL_SKULL);

            if (flag && pos.getY() >= world.getMinBuildHeight() && world.getDifficulty() != Difficulty.PEACEFUL) {
                BlockPattern shapedetector = WitherSkullBlock.getOrCreateWitherFull();
                BlockPattern.BlockPatternMatch shapedetector_shapedetectorcollection = shapedetector.find(world, pos);

                if (shapedetector_shapedetectorcollection != null) {
                    // CraftBukkit start - Use BlockStateListPopulator
                    BlockStateListPopulator blockList = new BlockStateListPopulator(world);
                    for (int i = 0; i < shapedetector.getWidth(); ++i) {
                        for (int j = 0; j < shapedetector.getHeight(); ++j) {
                            BlockInWorld shapedetectorblock = shapedetector_shapedetectorcollection.getBlock(i, j, 0);

                            blockList.setBlock(shapedetectorblock.getPos(), Blocks.AIR.defaultBlockState(), 2); // CraftBukkit
                            // world.levelEvent(2001, shapedetectorblock.getPos(), Block.getId(shapedetectorblock.getState())); // CraftBukkit
                        }
                    }

                    WitherBoss entitywither = (WitherBoss) EntityType.WITHER.create(world);
                    BlockPos blockposition1 = shapedetector_shapedetectorcollection.getBlock(1, 2, 0).getPos();

                    entitywither.moveTo((double) blockposition1.getX() + 0.5D, (double) blockposition1.getY() + 0.55D, (double) blockposition1.getZ() + 0.5D, shapedetector_shapedetectorcollection.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F, 0.0F);
                    entitywither.yBodyRot = shapedetector_shapedetectorcollection.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F;
                    entitywither.makeInvulnerable();
                    // CraftBukkit start
                    if (!world.addFreshEntity(entitywither, SpawnReason.BUILD_WITHER)) {
                        return;
                    }
                    for (BlockPos pos1 : blockList.getBlocks()) {
                        world.levelEvent(2001, pos1, Block.getId(world.getBlockState(pos1)));
                    }
                    blockList.updateList();
                    // CraftBukkit end
                    Iterator iterator = world.getEntitiesOfClass(ServerPlayer.class, entitywither.getBoundingBox().inflate(50.0D)).iterator();

                    while (iterator.hasNext()) {
                        ServerPlayer entityplayer = (ServerPlayer) iterator.next();

                        CriteriaTriggers.SUMMONED_ENTITY.trigger(entityplayer, (Entity) entitywither);
                    }

                    // world.addFreshEntity(entitywither); // CraftBukkit - moved up

                    for (int k = 0; k < shapedetector.getWidth(); ++k) {
                        for (int l = 0; l < shapedetector.getHeight(); ++l) {
                            world.blockUpdated(shapedetector_shapedetectorcollection.getBlock(k, l, 0).getPos(), Blocks.AIR);
                        }
                    }

                }
            }
        }
    }

    public static boolean canSpawnMob(Level world, BlockPos pos, ItemStack stack) {
        return stack.is(Items.WITHER_SKELETON_SKULL) && pos.getY() >= world.getMinBuildHeight() + 2 && world.getDifficulty() != Difficulty.PEACEFUL && !world.isClientSide ? WitherSkullBlock.getOrCreateWitherBase().find(world, pos) != null : false;
    }

    private static BlockPattern getOrCreateWitherFull() {
        if (WitherSkullBlock.witherPatternFull == null) {
            WitherSkullBlock.witherPatternFull = BlockPatternBuilder.start().aisle("^^^", "###", "~#~").where('#', (shapedetectorblock) -> {
                return shapedetectorblock.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS);
            }).where('^', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_SKULL).or(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_WALL_SKULL)))).where('~', BlockInWorld.hasState(BlockMaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return WitherSkullBlock.witherPatternFull;
    }

    private static BlockPattern getOrCreateWitherBase() {
        if (WitherSkullBlock.witherPatternBase == null) {
            WitherSkullBlock.witherPatternBase = BlockPatternBuilder.start().aisle("   ", "###", "~#~").where('#', (shapedetectorblock) -> {
                return shapedetectorblock.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS);
            }).where('~', BlockInWorld.hasState(BlockMaterialPredicate.forMaterial(Material.AIR))).build();
        }

        return WitherSkullBlock.witherPatternBase;
    }
}
