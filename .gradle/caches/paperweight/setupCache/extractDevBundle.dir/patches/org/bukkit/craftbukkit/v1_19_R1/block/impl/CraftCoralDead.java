/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_19_R1.block.impl;

public final class CraftCoralDead extends org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData implements org.bukkit.block.data.Waterlogged {

    public CraftCoralDead() {
        super();
    }

    public CraftCoralDead(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.v1_19_R1.block.data.CraftWaterlogged

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED = getBoolean(net.minecraft.world.level.block.BaseCoralPlantBlock.class, "waterlogged");

    @Override
    public boolean isWaterlogged() {
        return get(CraftCoralDead.WATERLOGGED);
    }

    @Override
    public void setWaterlogged(boolean waterlogged) {
        set(CraftCoralDead.WATERLOGGED, waterlogged);
    }
}
