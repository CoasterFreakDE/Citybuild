package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetInstrumentFunction extends LootItemConditionalFunction {
    final TagKey<Instrument> options;

    SetInstrumentFunction(LootItemCondition[] conditions, TagKey<Instrument> options) {
        super(conditions);
        this.options = options;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_INSTRUMENT;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        InstrumentItem.setRandom(stack, this.options, context.getRandom());
        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> setInstrumentOptions(TagKey<Instrument> options) {
        return simpleBuilder((conditions) -> {
            return new SetInstrumentFunction(conditions, options);
        });
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetInstrumentFunction> {
        @Override
        public void serialize(JsonObject json, SetInstrumentFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("options", "#" + object.options.location());
        }

        @Override
        public SetInstrumentFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            String string = GsonHelper.getAsString(jsonObject, "options");
            if (!string.startsWith("#")) {
                throw new JsonSyntaxException("Inline tag value not supported: " + string);
            } else {
                return new SetInstrumentFunction(lootItemConditions, TagKey.create(Registry.INSTRUMENT_REGISTRY, new ResourceLocation(string.substring(1))));
            }
        }
    }
}
