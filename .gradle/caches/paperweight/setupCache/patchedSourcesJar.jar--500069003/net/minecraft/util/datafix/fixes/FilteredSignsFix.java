package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class FilteredSignsFix extends NamedEntityFix {
    public FilteredSignsFix(Schema schema) {
        super(schema, false, "Remove filtered text from signs", References.BLOCK_ENTITY, "minecraft:sign");
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), (blockEntity) -> {
            return blockEntity.remove("FilteredText1").remove("FilteredText2").remove("FilteredText3").remove("FilteredText4");
        });
    }
}
