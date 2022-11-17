package net.minecraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Containers {
    public static void dropContents(Level world, BlockPos pos, Container inventory) {
        dropContents(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), inventory);
    }

    public static void dropContents(Level world, Entity entity, Container inventory) {
        dropContents(world, entity.getX(), entity.getY(), entity.getZ(), inventory);
    }

    private static void dropContents(Level world, double x, double y, double z, Container inventory) {
        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            dropItemStack(world, x, y, z, inventory.getItem(i));
        }

    }

    public static void dropContents(Level world, BlockPos pos, NonNullList<ItemStack> stacks) {
        stacks.forEach((stack) -> {
            dropItemStack(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), stack);
        });
    }

    public static void dropItemStack(Level world, double x, double y, double z, ItemStack stack) {
        double d = (double)EntityType.ITEM.getWidth();
        double e = 1.0D - d;
        double f = d / 2.0D;
        double g = Math.floor(x) + world.random.nextDouble() * e + f;
        double h = Math.floor(y) + world.random.nextDouble() * e;
        double i = Math.floor(z) + world.random.nextDouble() * e + f;

        while(!stack.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(world, g, h, i, stack.split(world.random.nextInt(21) + 10));
            float j = 0.05F;
            itemEntity.setDeltaMovement(world.random.triangle(0.0D, 0.11485000171139836D), world.random.triangle(0.2D, 0.11485000171139836D), world.random.triangle(0.0D, 0.11485000171139836D));
            world.addFreshEntity(itemEntity);
        }

    }
}
