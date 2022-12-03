package net.minecraft.world.item;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData;
import org.bukkit.event.block.BlockCanBuildEvent;
// CraftBukkit end

public class StandingAndWallBlockItem extends BlockItem {

    public final Block wallBlock;

    public StandingAndWallBlockItem(Block standingBlock, Block wallBlock, Item.Properties settings) {
        super(standingBlock, settings);
        this.wallBlock = wallBlock;
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState iblockdata = this.wallBlock.getStateForPlacement(context);
        BlockState iblockdata1 = null;
        Level world = context.getLevel();
        BlockPos blockposition = context.getClickedPos();
        Direction[] aenumdirection = context.getNearestLookingDirections();
        int i = aenumdirection.length;

        for (int j = 0; j < i; ++j) {
            Direction enumdirection = aenumdirection[j];

            if (enumdirection != Direction.UP) {
                BlockState iblockdata2 = enumdirection == Direction.DOWN ? this.getBlock().getStateForPlacement(context) : iblockdata;

                if (iblockdata2 != null && iblockdata2.canSurvive(world, blockposition)) {
                    iblockdata1 = iblockdata2;
                    break;
                }
            }
        }

        // CraftBukkit start
        if (iblockdata1 != null) {
            boolean defaultReturn = world.isUnobstructed(iblockdata1, blockposition, CollisionContext.empty());
            org.bukkit.entity.Player player = (context.getPlayer() instanceof ServerPlayer) ? (org.bukkit.entity.Player) context.getPlayer().getBukkitEntity() : null;

            BlockCanBuildEvent event = new BlockCanBuildEvent(CraftBlock.at(world, blockposition), player, CraftBlockData.fromData(iblockdata1), defaultReturn);
            context.getLevel().getCraftServer().getPluginManager().callEvent(event);

            return (event.isBuildable()) ? iblockdata1 : null;
        } else {
            return null;
        }
        // CraftBukkit end
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        super.registerBlocks(map, item);
        map.put(this.wallBlock, item);
    }
}
