package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class InstrumentItem extends Item {
    private static final String TAG_INSTRUMENT = "instrument";
    private TagKey<Instrument> instruments;

    public InstrumentItem(Item.Properties settings, TagKey<Instrument> instrumentTag) {
        super(settings);
        this.instruments = instrumentTag;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        Optional<ResourceKey<Instrument>> optional = this.getInstrument(stack).flatMap(Holder::unwrapKey);
        if (optional.isPresent()) {
            MutableComponent mutableComponent = Component.translatable(Util.makeDescriptionId("instrument", optional.get().location()));
            tooltip.add(mutableComponent.withStyle(ChatFormatting.GRAY));
        }

    }

    public static ItemStack create(Item item, Holder<Instrument> instrument) {
        ItemStack itemStack = new ItemStack(item);
        setSoundVariantId(itemStack, instrument);
        return itemStack;
    }

    public static void setRandom(ItemStack stack, TagKey<Instrument> instrumentTag, RandomSource random) {
        Optional<Holder<Instrument>> optional = Registry.INSTRUMENT.getTag(instrumentTag).flatMap((entryList) -> {
            return entryList.getRandomElement(random);
        });
        if (optional.isPresent()) {
            setSoundVariantId(stack, optional.get());
        }

    }

    private static void setSoundVariantId(ItemStack stack, Holder<Instrument> instrument) {
        CompoundTag compoundTag = stack.getOrCreateTag();
        compoundTag.putString("instrument", instrument.unwrapKey().orElseThrow(() -> {
            return new IllegalStateException("Invalid instrument");
        }).location().toString());
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (this.allowedIn(group)) {
            for(Holder<Instrument> holder : Registry.INSTRUMENT.getTagOrEmpty(this.instruments)) {
                stacks.add(create(Items.GOAT_HORN, holder));
            }
        }

    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        Optional<Holder<Instrument>> optional = this.getInstrument(itemStack);
        if (optional.isPresent()) {
            Instrument instrument = optional.get().value();
            user.startUsingItem(hand);
            play(world, user, instrument);
            user.getCooldowns().addCooldown(this, instrument.useDuration());
            return InteractionResultHolder.consume(itemStack);
        } else {
            return InteractionResultHolder.fail(itemStack);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        Optional<Holder<Instrument>> optional = this.getInstrument(stack);
        return optional.isPresent() ? optional.get().value().useDuration() : 0;
    }

    private Optional<Holder<Instrument>> getInstrument(ItemStack stack) {
        CompoundTag compoundTag = stack.getTag();
        if (compoundTag != null) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(compoundTag.getString("instrument"));
            if (resourceLocation != null) {
                return Registry.INSTRUMENT.getHolder(ResourceKey.create(Registry.INSTRUMENT_REGISTRY, resourceLocation));
            }
        }

        Iterator<Holder<Instrument>> iterator = Registry.INSTRUMENT.getTagOrEmpty(this.instruments).iterator();
        return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    private static void play(Level world, Player player, Instrument instrument) {
        SoundEvent soundEvent = instrument.soundEvent();
        float f = instrument.range() / 16.0F;
        world.playSound(player, player, soundEvent, SoundSource.RECORDS, f, 1.0F);
        world.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
    }
}
