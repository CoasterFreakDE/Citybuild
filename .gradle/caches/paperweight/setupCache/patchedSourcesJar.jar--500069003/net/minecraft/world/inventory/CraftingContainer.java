package net.minecraft.world.inventory;

import java.util.Iterator;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
// CraftBukkit start
import java.util.List;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
// CraftBukkit end

public class CraftingContainer implements Container, StackedContentsCompatible {

    private final NonNullList<ItemStack> items;
    private final int width;
    private final int height;
    public final AbstractContainerMenu menu;

    // CraftBukkit start - add fields
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    private Recipe currentRecipe;
    public Container resultInventory;
    private Player owner;
    private int maxStack = MAX_STACK;

    public List<ItemStack> getContents() {
        return this.items;
    }

    public void onOpen(CraftHumanEntity who) {
        this.transaction.add(who);
    }

    public InventoryType getInvType() {
        return this.items.size() == 4 ? InventoryType.CRAFTING : InventoryType.WORKBENCH;
    }

    public void onClose(CraftHumanEntity who) {
        this.transaction.remove(who);
    }

    public List<HumanEntity> getViewers() {
        return this.transaction;
    }

    public org.bukkit.inventory.InventoryHolder getOwner() {
        return (this.owner == null) ? null : this.owner.getBukkitEntity();
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStack;
    }

    public void setMaxStackSize(int size) {
        this.maxStack = size;
        this.resultInventory.setMaxStackSize(size);
    }

    @Override
    public Location getLocation() {
        return this.menu instanceof CraftingMenu ? ((CraftingMenu) this.menu).access.getLocation() : this.owner.getBukkitEntity().getLocation();
    }

    @Override
    public Recipe getCurrentRecipe() {
        return this.currentRecipe;
    }

    @Override
    public void setCurrentRecipe(Recipe currentRecipe) {
        this.currentRecipe = currentRecipe;
    }

    public CraftingContainer(AbstractContainerMenu container, int i, int j, Player player) {
        this(container, i, j);
        this.owner = player;
    }
    // CraftBukkit end

    public CraftingContainer(AbstractContainerMenu handler, int width, int height) {
        this.items = NonNullList.withSize(width * height, ItemStack.EMPTY);
        this.menu = handler;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        Iterator iterator = this.items.iterator();

        ItemStack itemstack;

        do {
            if (!iterator.hasNext()) {
                return true;
            }

            itemstack = (ItemStack) iterator.next();
        } while (itemstack.isEmpty());

        return false;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= this.getContainerSize() ? ItemStack.EMPTY : (ItemStack) this.items.get(slot);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack itemstack = ContainerHelper.removeItem(this.items, slot, amount);

        if (!itemstack.isEmpty()) {
            this.menu.slotsChanged(this);
        }

        return itemstack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        this.menu.slotsChanged(this);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    @Override
    public void fillStackedContents(StackedContents finder) {
        Iterator iterator = this.items.iterator();

        while (iterator.hasNext()) {
            ItemStack itemstack = (ItemStack) iterator.next();

            finder.accountSimpleStack(itemstack);
        }

    }
}
