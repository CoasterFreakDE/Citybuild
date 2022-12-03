package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;

public class BlendingDataRemoveFromNetherEndFix extends DataFix {
    public BlendingDataRemoveFromNetherEndFix(Schema schema) {
        super(schema, false);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getOutputSchema().getType(References.CHUNK);
        return this.fixTypeEverywhereTyped("BlendingDataRemoveFromNetherEndFix", type, (typed) -> {
            return typed.update(DSL.remainderFinder(), (chunk) -> {
                return updateChunkTag(chunk, chunk.get("__context"));
            });
        });
    }

    private static Dynamic<?> updateChunkTag(Dynamic<?> chunk, OptionalDynamic<?> context) {
        boolean bl = "minecraft:overworld".equals(context.get("dimension").asString().result().orElse(""));
        return bl ? chunk : chunk.remove("blending_data");
    }
}
