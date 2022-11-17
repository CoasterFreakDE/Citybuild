package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class EntityPaintingFieldsRenameFix extends NamedEntityFix {
    public EntityPaintingFieldsRenameFix(Schema schema) {
        super(schema, false, "EntityPaintingFieldsRenameFix", References.ENTITY, "minecraft:painting");
    }

    public Dynamic<?> fixTag(Dynamic<?> dynamic) {
        return this.renameField(this.renameField(dynamic, "Motive", "variant"), "Facing", "facing");
    }

    private Dynamic<?> renameField(Dynamic<?> dynamic, String oldKey, String newKey) {
        Optional<? extends Dynamic<?>> optional = dynamic.get(oldKey).result();
        Optional<? extends Dynamic<?>> optional2 = optional.map((value) -> {
            return dynamic.remove(oldKey).set(newKey, value);
        });
        return DataFixUtils.orElse(optional2, dynamic);
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), this::fixTag);
    }
}
