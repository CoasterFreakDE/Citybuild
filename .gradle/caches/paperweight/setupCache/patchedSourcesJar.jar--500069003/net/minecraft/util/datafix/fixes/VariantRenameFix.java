package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;

public class VariantRenameFix extends NamedEntityFix {
    private final Map<String, String> renames;

    public VariantRenameFix(Schema schema, String name, DSL.TypeReference type, String choiceName, Map<String, String> oldToNewNames) {
        super(schema, false, name, type, choiceName);
        this.renames = oldToNewNames;
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), (dynamic) -> {
            return dynamic.update("variant", (variant) -> {
                return DataFixUtils.orElse(variant.asString().map((variantName) -> {
                    return variant.createString(this.renames.getOrDefault(variantName, variantName));
                }).result(), variant);
            });
        });
    }
}
