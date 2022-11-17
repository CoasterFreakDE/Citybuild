package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.SectionPos;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class BlendingDataFix extends DataFix {
    private final String name;
    private static final Set<String> STATUSES_TO_SKIP_BLENDING = Set.of("minecraft:empty", "minecraft:structure_starts", "minecraft:structure_references", "minecraft:biomes");

    public BlendingDataFix(Schema schema) {
        super(schema, false);
        this.name = "Blending Data Fix v" + schema.getVersionKey();
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getOutputSchema().getType(References.CHUNK);
        return this.fixTypeEverywhereTyped(this.name, type, (typed) -> {
            return typed.update(DSL.remainderFinder(), (chunk) -> {
                return updateChunkTag(chunk, chunk.get("__context"));
            });
        });
    }

    private static Dynamic<?> updateChunkTag(Dynamic<?> chunk, OptionalDynamic<?> context) {
        chunk = chunk.remove("blending_data");
        boolean bl = "minecraft:overworld".equals(context.get("dimension").asString().result().orElse(""));
        Optional<? extends Dynamic<?>> optional = chunk.get("Status").result();
        if (bl && optional.isPresent()) {
            String string = NamespacedSchema.ensureNamespaced(optional.get().asString("empty"));
            Optional<? extends Dynamic<?>> optional2 = chunk.get("below_zero_retrogen").result();
            if (!STATUSES_TO_SKIP_BLENDING.contains(string)) {
                chunk = updateBlendingData(chunk, 384, -64);
            } else if (optional2.isPresent()) {
                Dynamic<?> dynamic = optional2.get();
                String string2 = NamespacedSchema.ensureNamespaced(dynamic.get("target_status").asString("empty"));
                if (!STATUSES_TO_SKIP_BLENDING.contains(string2)) {
                    chunk = updateBlendingData(chunk, 256, 0);
                }
            }
        }

        return chunk;
    }

    private static Dynamic<?> updateBlendingData(Dynamic<?> dynamic, int height, int minY) {
        return dynamic.set("blending_data", dynamic.createMap(Map.of(dynamic.createString("min_section"), dynamic.createInt(SectionPos.blockToSectionCoord(minY)), dynamic.createString("max_section"), dynamic.createInt(SectionPos.blockToSectionCoord(minY + height)))));
    }
}
