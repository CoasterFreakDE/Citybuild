package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface StringRepresentable {
    int PRE_BUILT_MAP_THRESHOLD = 16;

    String getSerializedName();

    static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(Supplier<E[]> enumValues) {
        E[] enums = (Enum[])enumValues.get();
        if (enums.length > 16) {
            Map<String, E> map = Arrays.stream(enums).collect(Collectors.toMap((identifiable) -> {
                return identifiable.getSerializedName();
            }, (enum_) -> {
                return enum_;
            }));
            return new StringRepresentable.EnumCodec<>(enums, (id) -> {
                return (E)(id == null ? null : map.get(id));
            });
        } else {
            return new StringRepresentable.EnumCodec<>(enums, (id) -> {
                for(E enum_ : enums) {
                    if (enum_.getSerializedName().equals(id)) {
                        return enum_;
                    }
                }

                return (E)null;
            });
        }
    }

    static Keyable keys(final StringRepresentable[] values) {
        return new Keyable() {
            public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
                return Arrays.stream(values).map(StringRepresentable::getSerializedName).map(dynamicOps::createString);
            }
        };
    }

    /** @deprecated */
    @Deprecated
    public static class EnumCodec<E extends Enum<E> & StringRepresentable> implements Codec<E> {
        private Codec<E> codec;
        private Function<String, E> resolver;

        public EnumCodec(E[] values, Function<String, E> idToIdentifiable) {
            this.codec = ExtraCodecs.orCompressed(ExtraCodecs.stringResolverCodec((identifiable) -> {
                return identifiable.getSerializedName();
            }, idToIdentifiable), ExtraCodecs.idResolverCodec((enum_) -> {
                return enum_.ordinal();
            }, (ordinal) -> {
                return (E)(ordinal >= 0 && ordinal < values.length ? values[ordinal] : null);
            }, -1));
            this.resolver = idToIdentifiable;
        }

        public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> dynamicOps, T object) {
            return this.codec.decode(dynamicOps, object);
        }

        public <T> DataResult<T> encode(E enum_, DynamicOps<T> dynamicOps, T object) {
            return this.codec.encode(enum_, dynamicOps, object);
        }

        @Nullable
        public E byName(@Nullable String id) {
            return this.resolver.apply(id);
        }
    }
}
