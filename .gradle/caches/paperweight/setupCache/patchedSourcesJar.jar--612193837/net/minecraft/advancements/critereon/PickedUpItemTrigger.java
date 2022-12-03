package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class PickedUpItemTrigger extends SimpleCriterionTrigger<PickedUpItemTrigger.TriggerInstance> {
    private final ResourceLocation id;

    public PickedUpItemTrigger(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    protected PickedUpItemTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "entity", deserializationContext);
        return new PickedUpItemTrigger.TriggerInstance(this.id, composite, itemPredicate, composite2);
    }

    public void trigger(ServerPlayer player, ItemStack stack, @Nullable Entity entity) {
        LootContext lootContext = EntityPredicate.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, stack, lootContext);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final ItemPredicate item;
        private final EntityPredicate.Composite entity;

        public TriggerInstance(ResourceLocation id, EntityPredicate.Composite player, ItemPredicate item, EntityPredicate.Composite entity) {
            super(id, player);
            this.item = item;
            this.entity = entity;
        }

        public static PickedUpItemTrigger.TriggerInstance thrownItemPickedUpByEntity(EntityPredicate.Composite player, ItemPredicate item, EntityPredicate.Composite entity) {
            return new PickedUpItemTrigger.TriggerInstance(CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.getId(), player, item, entity);
        }

        public static PickedUpItemTrigger.TriggerInstance thrownItemPickedUpByPlayer(EntityPredicate.Composite player, ItemPredicate item, EntityPredicate.Composite entity) {
            return new PickedUpItemTrigger.TriggerInstance(CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER.getId(), player, item, entity);
        }

        public boolean matches(ServerPlayer player, ItemStack stack, LootContext entityContext) {
            if (!this.item.matches(stack)) {
                return false;
            } else {
                return this.entity.matches(entityContext);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
