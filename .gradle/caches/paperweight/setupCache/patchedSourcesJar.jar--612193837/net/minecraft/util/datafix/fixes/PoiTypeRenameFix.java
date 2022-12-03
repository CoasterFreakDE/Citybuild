package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.Function;
import java.util.stream.Stream;

public class PoiTypeRenameFix extends AbstractPoiSectionFix {
    private final Function<String, String> renamer;

    public PoiTypeRenameFix(Schema schema, String name, Function<String, String> renamer) {
        super(schema, name);
        this.renamer = renamer;
    }

    @Override
    protected <T> Stream<Dynamic<T>> processRecords(Stream<Dynamic<T>> dynamics) {
        return dynamics.map((dynamic) -> {
            return dynamic.update("type", (dynamicx) -> {
                return DataFixUtils.orElse(dynamicx.asString().map(this.renamer).map(dynamicx::createString).result(), dynamicx);
            });
        });
    }
}
