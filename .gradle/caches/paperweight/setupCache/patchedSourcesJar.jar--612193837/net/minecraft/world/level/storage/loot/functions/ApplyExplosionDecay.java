package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyExplosionDecay extends LootItemConditionalFunction {
    ApplyExplosionDecay(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.EXPLOSION_DECAY;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        Float float_ = context.getParamOrNull(LootContextParams.EXPLOSION_RADIUS);
        if (float_ != null) {
            RandomSource randomSource = context.getRandom();
            float f = 1.0F / float_;
            int i = stack.getCount();
            int j = 0;

            for(int k = 0; k < i; ++k) {
                if (randomSource.nextFloat() <= f) {
                    ++j;
                }
            }

            stack.setCount(j);
        }

        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> explosionDecay() {
        return simpleBuilder(ApplyExplosionDecay::new);
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<ApplyExplosionDecay> {
        @Override
        public ApplyExplosionDecay deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            return new ApplyExplosionDecay(lootItemConditions);
        }
    }
}
