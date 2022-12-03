package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemInput implements Predicate<ItemStack> {
    private static final Dynamic2CommandExceptionType ERROR_STACK_TOO_BIG = new Dynamic2CommandExceptionType((item, maxCount) -> {
        return Component.translatable("arguments.item.overstacked", item, maxCount);
    });
    private final Holder<Item> item;
    @Nullable
    private final CompoundTag tag;

    public ItemInput(Holder<Item> item, @Nullable CompoundTag nbt) {
        this.item = item;
        this.tag = nbt;
    }

    public Item getItem() {
        return this.item.value();
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return itemStack.is(this.item) && NbtUtils.compareNbt(this.tag, itemStack.getTag(), true);
    }

    public ItemStack createItemStack(int amount, boolean checkOverstack) throws CommandSyntaxException {
        ItemStack itemStack = new ItemStack(this.item, amount);
        if (this.tag != null) {
            itemStack.setTag(this.tag);
        }

        if (checkOverstack && amount > itemStack.getMaxStackSize()) {
            throw ERROR_STACK_TOO_BIG.create(this.getItemName(), itemStack.getMaxStackSize());
        } else {
            return itemStack;
        }
    }

    public String serialize() {
        StringBuilder stringBuilder = new StringBuilder(this.getItemName());
        if (this.tag != null) {
            stringBuilder.append((Object)this.tag);
        }

        return stringBuilder.toString();
    }

    private String getItemName() {
        return this.item.unwrapKey().map(ResourceKey::location).orElseGet(() -> {
            return "unknown[" + this.item + "]";
        }).toString();
    }
}
