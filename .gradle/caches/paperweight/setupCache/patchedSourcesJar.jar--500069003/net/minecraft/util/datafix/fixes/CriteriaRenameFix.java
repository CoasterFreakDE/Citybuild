package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;

public class CriteriaRenameFix extends DataFix {
    private final String name;
    private final String advancementId;
    private final UnaryOperator<String> conversions;

    public CriteriaRenameFix(Schema schema, String description, String advancementId, UnaryOperator<String> renamer) {
        super(schema, false);
        this.name = description;
        this.advancementId = advancementId;
        this.conversions = renamer;
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(this.name, this.getInputSchema().getType(References.ADVANCEMENTS), (typed) -> {
            return typed.update(DSL.remainderFinder(), this::fixAdvancements);
        });
    }

    private Dynamic<?> fixAdvancements(Dynamic<?> advancements) {
        return advancements.update(this.advancementId, (advancement) -> {
            return advancement.update("criteria", (criteria) -> {
                return criteria.updateMapValues((pair) -> {
                    return pair.mapFirst((key) -> {
                        return DataFixUtils.orElse(key.asString().map((keyString) -> {
                            return key.createString(this.conversions.apply(keyString));
                        }).result(), key);
                    });
                });
            });
        });
    }
}
