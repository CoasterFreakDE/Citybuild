package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public class LighthingBoltPredicate implements EntitySubPredicate {
    private static final String BLOCKS_SET_ON_FIRE_KEY = "blocks_set_on_fire";
    private static final String ENTITY_STRUCK_KEY = "entity_struck";
    private final MinMaxBounds.Ints blocksSetOnFire;
    private final EntityPredicate entityStruck;

    private LighthingBoltPredicate(MinMaxBounds.Ints blocksSetOnFire, EntityPredicate entityStruck) {
        this.blocksSetOnFire = blocksSetOnFire;
        this.entityStruck = entityStruck;
    }

    public static LighthingBoltPredicate blockSetOnFire(MinMaxBounds.Ints blocksSetOnFire) {
        return new LighthingBoltPredicate(blocksSetOnFire, EntityPredicate.ANY);
    }

    public static LighthingBoltPredicate fromJson(JsonObject json) {
        return new LighthingBoltPredicate(MinMaxBounds.Ints.fromJson(json.get("blocks_set_on_fire")), EntityPredicate.fromJson(json.get("entity_struck")));
    }

    @Override
    public JsonObject serializeCustomData() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("blocks_set_on_fire", this.blocksSetOnFire.serializeToJson());
        jsonObject.add("entity_struck", this.entityStruck.serializeToJson());
        return jsonObject;
    }

    @Override
    public EntitySubPredicate.Type type() {
        return EntitySubPredicate.Types.LIGHTNING;
    }

    @Override
    public boolean matches(Entity entity, ServerLevel world, @Nullable Vec3 pos) {
        if (!(entity instanceof LightningBolt lightningBolt)) {
            return false;
        } else {
            return this.blocksSetOnFire.matches(lightningBolt.getBlocksSetOnFire()) && (this.entityStruck == EntityPredicate.ANY || lightningBolt.getHitEntities().anyMatch((struckEntity) -> {
                return this.entityStruck.matches(world, pos, struckEntity);
            }));
        }
    }
}
