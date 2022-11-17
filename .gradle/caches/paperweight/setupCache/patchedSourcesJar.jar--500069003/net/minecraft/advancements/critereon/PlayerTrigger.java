package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class PlayerTrigger extends SimpleCriterionTrigger<PlayerTrigger.TriggerInstance> {
    final ResourceLocation id;

    public PlayerTrigger(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public PlayerTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        return new PlayerTrigger.TriggerInstance(this.id, composite);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, (conditions) -> {
            return true;
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        public TriggerInstance(ResourceLocation id, EntityPredicate.Composite entity) {
            super(id, entity);
        }

        public static PlayerTrigger.TriggerInstance located(LocationPredicate location) {
            return new PlayerTrigger.TriggerInstance(CriteriaTriggers.LOCATION.id, EntityPredicate.Composite.wrap(EntityPredicate.Builder.entity().located(location).build()));
        }

        public static PlayerTrigger.TriggerInstance located(EntityPredicate entity) {
            return new PlayerTrigger.TriggerInstance(CriteriaTriggers.LOCATION.id, EntityPredicate.Composite.wrap(entity));
        }

        public static PlayerTrigger.TriggerInstance sleptInBed() {
            return new PlayerTrigger.TriggerInstance(CriteriaTriggers.SLEPT_IN_BED.id, EntityPredicate.Composite.ANY);
        }

        public static PlayerTrigger.TriggerInstance raidWon() {
            return new PlayerTrigger.TriggerInstance(CriteriaTriggers.RAID_WIN.id, EntityPredicate.Composite.ANY);
        }

        public static PlayerTrigger.TriggerInstance avoidVibration() {
            return new PlayerTrigger.TriggerInstance(CriteriaTriggers.AVOID_VIBRATION.id, EntityPredicate.Composite.ANY);
        }

        public static PlayerTrigger.TriggerInstance walkOnBlockWithEquipment(Block block, Item item) {
            return located(EntityPredicate.Builder.entity().equipment(EntityEquipmentPredicate.Builder.equipment().feet(ItemPredicate.Builder.item().of(item).build()).build()).steppingOn(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(block).build()).build()).build());
        }
    }
}
