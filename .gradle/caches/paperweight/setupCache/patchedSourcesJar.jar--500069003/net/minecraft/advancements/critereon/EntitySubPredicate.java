package net.minecraft.advancements.critereon;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.phys.Vec3;

public interface EntitySubPredicate {
    EntitySubPredicate ANY = new EntitySubPredicate() {
        @Override
        public boolean matches(Entity entity, ServerLevel world, @Nullable Vec3 pos) {
            return true;
        }

        @Override
        public JsonObject serializeCustomData() {
            return new JsonObject();
        }

        @Override
        public EntitySubPredicate.Type type() {
            return EntitySubPredicate.Types.ANY;
        }
    };

    static EntitySubPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "type_specific");
            String string = GsonHelper.getAsString(jsonObject, "type", (String)null);
            if (string == null) {
                return ANY;
            } else {
                EntitySubPredicate.Type type = EntitySubPredicate.Types.TYPES.get(string);
                if (type == null) {
                    throw new JsonSyntaxException("Unknown sub-predicate type: " + string);
                } else {
                    return type.deserialize(jsonObject);
                }
            }
        } else {
            return ANY;
        }
    }

    boolean matches(Entity entity, ServerLevel world, @Nullable Vec3 pos);

    JsonObject serializeCustomData();

    default JsonElement serialize() {
        if (this.type() == EntitySubPredicate.Types.ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = this.serializeCustomData();
            String string = EntitySubPredicate.Types.TYPES.inverse().get(this.type());
            jsonObject.addProperty("type", string);
            return jsonObject;
        }
    }

    EntitySubPredicate.Type type();

    static EntitySubPredicate variant(CatVariant variant) {
        return EntitySubPredicate.Types.CAT.createPredicate(variant);
    }

    static EntitySubPredicate variant(FrogVariant variant) {
        return EntitySubPredicate.Types.FROG.createPredicate(variant);
    }

    public interface Type {
        EntitySubPredicate deserialize(JsonObject jsonObject);
    }

    public static final class Types {
        public static final EntitySubPredicate.Type ANY = (json) -> {
            return EntitySubPredicate.ANY;
        };
        public static final EntitySubPredicate.Type LIGHTNING = LighthingBoltPredicate::fromJson;
        public static final EntitySubPredicate.Type FISHING_HOOK = FishingHookPredicate::fromJson;
        public static final EntitySubPredicate.Type PLAYER = PlayerPredicate::fromJson;
        public static final EntitySubPredicate.Type SLIME = SlimePredicate::fromJson;
        public static final EntityVariantPredicate<CatVariant> CAT = EntityVariantPredicate.create(Registry.CAT_VARIANT, (entity) -> {
            Optional var10000;
            if (entity instanceof Cat cat) {
                var10000 = Optional.of(cat.getCatVariant());
            } else {
                var10000 = Optional.empty();
            }

            return var10000;
        });
        public static final EntityVariantPredicate<FrogVariant> FROG = EntityVariantPredicate.create(Registry.FROG_VARIANT, (entity) -> {
            Optional var10000;
            if (entity instanceof Frog frog) {
                var10000 = Optional.of(frog.getVariant());
            } else {
                var10000 = Optional.empty();
            }

            return var10000;
        });
        public static final BiMap<String, EntitySubPredicate.Type> TYPES = ImmutableBiMap.of("any", ANY, "lightning", LIGHTNING, "fishing_hook", FISHING_HOOK, "player", PLAYER, "slime", SLIME, "cat", CAT.type(), "frog", FROG.type());
    }
}
