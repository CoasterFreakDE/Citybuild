package net.minecraft.world.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

public class AdventureModeCheck {
    private final String tagName;
    @Nullable
    private BlockInWorld lastCheckedBlock;
    private boolean lastResult;
    private boolean checksBlockEntity;

    public AdventureModeCheck(String key) {
        this.tagName = key;
    }

    private static boolean areSameBlocks(BlockInWorld pos, @Nullable BlockInWorld cachedPos, boolean nbtAware) {
        if (cachedPos != null && pos.getState() == cachedPos.getState()) {
            if (!nbtAware) {
                return true;
            } else if (pos.getEntity() == null && cachedPos.getEntity() == null) {
                return true;
            } else {
                return pos.getEntity() != null && cachedPos.getEntity() != null ? Objects.equals(pos.getEntity().saveWithId(), cachedPos.getEntity().saveWithId()) : false;
            }
        } else {
            return false;
        }
    }

    public boolean test(ItemStack stack, Registry<Block> blockRegistry, BlockInWorld pos) {
        if (areSameBlocks(pos, this.lastCheckedBlock, this.checksBlockEntity)) {
            return this.lastResult;
        } else {
            this.lastCheckedBlock = pos;
            this.checksBlockEntity = false;
            CompoundTag compoundTag = stack.getTag();
            if (compoundTag != null && compoundTag.contains(this.tagName, 9)) {
                ListTag listTag = compoundTag.getList(this.tagName, 8);

                for(int i = 0; i < listTag.size(); ++i) {
                    String string = listTag.getString(i);

                    try {
                        BlockPredicateArgument.Result result = BlockPredicateArgument.parse(HolderLookup.forRegistry(blockRegistry), new StringReader(string));
                        this.checksBlockEntity |= result.requiresNbt();
                        if (result.test(pos)) {
                            this.lastResult = true;
                            return true;
                        }
                    } catch (CommandSyntaxException var9) {
                    }
                }
            }

            this.lastResult = false;
            return false;
        }
    }
}
