package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3081 extends NamespacedSchema {
    public V3081(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static void registerMob(Schema schema, Map<String, Supplier<TypeTemplate>> map, String id) {
        schema.register(map, id, () -> {
            return V100.equipment(schema);
        });
        schema.register(map, "minecraft:warden", () -> {
            return DSL.optionalFields("ArmorItems", DSL.list(References.ITEM_STACK.in(schema)), "HandItems", DSL.list(References.ITEM_STACK.in(schema)), "listener", DSL.optionalFields("event", DSL.optionalFields("game_event", References.GAME_EVENT_NAME.in(schema))));
        });
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
        registerMob(schema, map, "minecraft:warden");
        return map;
    }
}
