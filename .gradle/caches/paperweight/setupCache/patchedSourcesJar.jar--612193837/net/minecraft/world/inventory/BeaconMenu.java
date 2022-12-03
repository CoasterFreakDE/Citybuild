package net.minecraft.world.inventory;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftInventoryView;
// CraftBukkit end

public class BeaconMenu extends AbstractContainerMenu {

    private static final int PAYMENT_SLOT = 0;
    private static final int SLOT_COUNT = 1;
    private static final int DATA_COUNT = 3;
    private static final int INV_SLOT_START = 1;
    private static final int INV_SLOT_END = 28;
    private static final int USE_ROW_SLOT_START = 28;
    private static final int USE_ROW_SLOT_END = 37;
    private final Container beacon;
    private final BeaconMenu.PaymentSlot paymentSlot;
    private final ContainerLevelAccess access;
    private final ContainerData beaconData;
    // CraftBukkit start
    private CraftInventoryView bukkitEntity = null;
    private Inventory player;
    // CraftBukkit end

    public BeaconMenu(int syncId, Container inventory) {
        this(syncId, inventory, new SimpleContainerData(3), ContainerLevelAccess.NULL);
    }

    public BeaconMenu(int syncId, Container inventory, ContainerData propertyDelegate, ContainerLevelAccess context) {
        super(MenuType.BEACON, syncId);
        this.player = (Inventory) inventory; // CraftBukkit - TODO: check this
        this.beacon = new SimpleContainer(1) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return stack.is(ItemTags.BEACON_PAYMENT_ITEMS);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
        checkContainerDataCount(propertyDelegate, 3);
        this.beaconData = propertyDelegate;
        this.access = context;
        this.paymentSlot = new BeaconMenu.PaymentSlot(this.beacon, 0, 136, 110);
        this.addSlot(this.paymentSlot);
        this.addDataSlots(propertyDelegate);
        boolean flag = true;
        boolean flag1 = true;

        int j;

        for (j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(inventory, k + j * 9 + 9, 36 + k * 18, 137 + j * 18));
            }
        }

        for (j = 0; j < 9; ++j) {
            this.addSlot(new Slot(inventory, j, 36 + j * 18, 195));
        }

    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level.isClientSide) {
            ItemStack itemstack = this.paymentSlot.remove(this.paymentSlot.getMaxStackSize());

            if (!itemstack.isEmpty()) {
                player.drop(itemstack, false);
            }

        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (!this.checkReachable) return true; // CraftBukkit
        return stillValid(this.access, player, Blocks.BEACON);
    }

    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        this.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot) this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();

            itemstack = itemstack1.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.paymentSlot.hasItem() && this.paymentSlot.mayPlace(itemstack1) && itemstack1.getCount() == 1) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < 28) {
                if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 28 && index < 37) {
                if (!this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 1, 37, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    public int getLevels() {
        return this.beaconData.get(0);
    }

    @Nullable
    public MobEffect getPrimaryEffect() {
        return MobEffect.byId(this.beaconData.get(1));
    }

    @Nullable
    public MobEffect getSecondaryEffect() {
        return MobEffect.byId(this.beaconData.get(2));
    }
    // Paper start
    private static @Nullable org.bukkit.potion.PotionEffectType convert(Optional<MobEffect> effect) {
        return effect.flatMap(net.minecraft.core.Registry.MOB_EFFECT::getResourceKey).map(key -> {
            return org.bukkit.potion.PotionEffectType.getByKey(org.bukkit.craftbukkit.v1_19_R1.util.CraftNamespacedKey.fromMinecraft(key.location()));
        }).orElse(null);
    }
    // Paper end

    public void updateEffects(Optional<MobEffect> primary, Optional<MobEffect> secondary) {
        if (this.paymentSlot.hasItem()) {
            // Paper start
            io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent event = new io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent((org.bukkit.entity.Player) this.player.player.getBukkitEntity(), convert(primary), convert(secondary), this.access.getLocation().getBlock());
            if (event.callEvent()) {
            this.beaconData.set(1, event.getPrimary() == null ? -1 : event.getPrimary().getId());
            this.beaconData.set(2, event.getSecondary() == null ? -1 : event.getSecondary().getId());
            if (event.willConsumeItem()) {
            // Paper end
            this.paymentSlot.remove(1);
            }
            this.access.execute(Level::blockEntityChanged);
            } // Paper end
        }

    }

    public boolean hasPayment() {
        return !this.beacon.getItem(0).isEmpty();
    }

    private class PaymentSlot extends Slot {

        public PaymentSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ItemTags.BEACON_PAYMENT_ITEMS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // CraftBukkit start
    @Override
    public CraftInventoryView getBukkitView() {
        if (this.bukkitEntity != null) {
            return this.bukkitEntity;
        }

        org.bukkit.craftbukkit.v1_19_R1.inventory.CraftInventory inventory = new org.bukkit.craftbukkit.v1_19_R1.inventory.CraftInventoryBeacon(this.beacon);
        this.bukkitEntity = new CraftInventoryView(this.player.player.getBukkitEntity(), inventory, this);
        return this.bukkitEntity;
    }
    // CraftBukkit end
}
