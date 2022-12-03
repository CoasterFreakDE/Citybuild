package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;

public class SlimePredicate implements EntitySubPredicate {
    private final MinMaxBounds.Ints size;

    private SlimePredicate(MinMaxBounds.Ints size) {
        this.size = size;
    }

    public static SlimePredicate sized(MinMaxBounds.Ints size) {
        return new SlimePredicate(size);
    }

    public static SlimePredicate fromJson(JsonObject json) {
        MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(json.get("size"));
        return new SlimePredicate(ints);
    }

    @Override
    public JsonObject serializeCustomData() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("size", this.size.serializeToJson());
        return jsonObject;
    }

    @Override
    public boolean matches(Entity entity, ServerLevel world, @Nullable Vec3 pos) {
        if (entity instanceof Slime slime) {
            return this.size.matches(slime.getSize());
        } else {
            return false;
        }
    }

    @Override
    public EntitySubPredicate.Type type() {
        return EntitySubPredicate.Types.SLIME;
    }
}
