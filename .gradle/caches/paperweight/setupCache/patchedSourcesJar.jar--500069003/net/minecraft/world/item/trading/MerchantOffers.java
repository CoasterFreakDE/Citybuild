package net.minecraft.world.item.trading;

import java.util.ArrayList;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class MerchantOffers extends ArrayList<MerchantOffer> {
    public MerchantOffers() {
    }

    private MerchantOffers(int size) {
        super(size);
    }

    public MerchantOffers(CompoundTag nbt) {
        ListTag listTag = nbt.getList("Recipes", 10);

        for(int i = 0; i < listTag.size(); ++i) {
            this.add(new MerchantOffer(listTag.getCompound(i)));
        }

    }

    @Nullable
    public MerchantOffer getRecipeFor(ItemStack firstBuyItem, ItemStack secondBuyItem, int index) {
        if (index > 0 && index < this.size()) {
            MerchantOffer merchantOffer = this.get(index);
            return merchantOffer.satisfiedBy(firstBuyItem, secondBuyItem) ? merchantOffer : null;
        } else {
            for(int i = 0; i < this.size(); ++i) {
                MerchantOffer merchantOffer2 = this.get(i);
                if (merchantOffer2.satisfiedBy(firstBuyItem, secondBuyItem)) {
                    return merchantOffer2;
                }
            }

            return null;
        }
    }

    public void writeToStream(FriendlyByteBuf buf) {
        buf.writeCollection(this, (buf2, offer) -> {
            buf2.writeItem(offer.getBaseCostA());
            buf2.writeItem(offer.getResult());
            buf2.writeItem(offer.getCostB());
            buf2.writeBoolean(offer.isOutOfStock());
            buf2.writeInt(offer.getUses());
            buf2.writeInt(offer.getMaxUses());
            buf2.writeInt(offer.getXp());
            buf2.writeInt(offer.getSpecialPriceDiff());
            buf2.writeFloat(offer.getPriceMultiplier());
            buf2.writeInt(offer.getDemand());
        });
    }

    public static MerchantOffers createFromStream(FriendlyByteBuf buf) {
        return buf.readCollection(MerchantOffers::new, (buf2) -> {
            ItemStack itemStack = buf2.readItem();
            ItemStack itemStack2 = buf2.readItem();
            ItemStack itemStack3 = buf2.readItem();
            boolean bl = buf2.readBoolean();
            int i = buf2.readInt();
            int j = buf2.readInt();
            int k = buf2.readInt();
            int l = buf2.readInt();
            float f = buf2.readFloat();
            int m = buf2.readInt();
            MerchantOffer merchantOffer = new MerchantOffer(itemStack, itemStack3, itemStack2, i, j, k, f, m);
            if (bl) {
                merchantOffer.setToOutOfStock();
            }

            merchantOffer.setSpecialPriceDiff(l);
            return merchantOffer;
        });
    }

    public CompoundTag createTag() {
        CompoundTag compoundTag = new CompoundTag();
        ListTag listTag = new ListTag();

        for(int i = 0; i < this.size(); ++i) {
            MerchantOffer merchantOffer = this.get(i);
            listTag.add(merchantOffer.createTag());
        }

        compoundTag.put("Recipes", listTag);
        return compoundTag;
    }
}
