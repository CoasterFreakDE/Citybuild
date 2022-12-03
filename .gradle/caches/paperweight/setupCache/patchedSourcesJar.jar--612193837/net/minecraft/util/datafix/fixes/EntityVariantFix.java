package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;
import java.util.function.IntFunction;

public class EntityVariantFix extends NamedEntityFix {
    private final String fieldName;
    private final IntFunction<String> idConversions;

    public EntityVariantFix(Schema schema, String name, DSL.TypeReference typeReference, String entityId, String variantKey, IntFunction<String> variantIntToId) {
        super(schema, false, name, typeReference, entityId);
        this.fieldName = variantKey;
        this.idConversions = variantIntToId;
    }

    private static <T> Dynamic<T> updateAndRename(Dynamic<T> dynamic, String string, String string2, Function<Dynamic<T>, Dynamic<T>> function) {
        return dynamic.map((object) -> {
            DynamicOps<T> dynamicOps = dynamic.getOps();
            Function<T, T> function2 = (objectx) -> {
                return function.apply(new Dynamic<>(dynamicOps, objectx)).getValue();
            };
            return dynamicOps.get(object, string).map((object2) -> {
                return dynamicOps.set((T)object, string2, function2.apply(object2));
            }).result().orElse(object);
        });
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), (dynamic) -> {
            return updateAndRename(dynamic, this.fieldName, "variant", (dynamicx) -> {
                return DataFixUtils.orElse(dynamicx.asNumber().map((number) -> {
                    return dynamicx.createString(this.idConversions.apply(number.intValue()));
                }).result(), dynamicx);
            });
        });
    }
}
