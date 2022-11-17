package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public class FishingHookPredicate implements EntitySubPredicate {
    public static final FishingHookPredicate ANY = new FishingHookPredicate(false);
    private static final String IN_OPEN_WATER_KEY = "in_open_water";
    private final boolean inOpenWater;

    private FishingHookPredicate(boolean inOpenWater) {
        this.inOpenWater = inOpenWater;
    }

    public static FishingHookPredicate inOpenWater(boolean inOpenWater) {
        return new FishingHookPredicate(inOpenWater);
    }

    public static FishingHookPredicate fromJson(JsonObject json) {
        JsonElement jsonElement = json.get("in_open_water");
        return jsonElement != null ? new FishingHookPredicate(GsonHelper.convertToBoolean(jsonElement, "in_open_water")) : ANY;
    }

    @Override
    public JsonObject serializeCustomData() {
        if (this == ANY) {
            return new JsonObject();
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("in_open_water", new JsonPrimitive(this.inOpenWater));
            return jsonObject;
        }
    }

    @Override
    public EntitySubPredicate.Type type() {
        return EntitySubPredicate.Types.FISHING_HOOK;
    }

    @Override
    public boolean matches(Entity entity, ServerLevel world, @Nullable Vec3 pos) {
        if (this == ANY) {
            return true;
        } else if (!(entity instanceof FishingHook)) {
            return false;
        } else {
            FishingHook fishingHook = (FishingHook)entity;
            return this.inOpenWater == fishingHook.isOpenWaterFishing();
        }
    }
}
