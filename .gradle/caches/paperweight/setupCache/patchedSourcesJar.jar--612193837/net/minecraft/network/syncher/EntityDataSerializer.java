package net.minecraft.network.syncher;

import java.util.Optional;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;

public interface EntityDataSerializer<T> {
    void write(FriendlyByteBuf buf, T value);

    T read(FriendlyByteBuf buf);

    default EntityDataAccessor<T> createAccessor(int id) {
        return new EntityDataAccessor<>(id, this);
    }

    T copy(T value);

    static <T> EntityDataSerializer<T> simple(final FriendlyByteBuf.Writer<T> writer, final FriendlyByteBuf.Reader<T> reader) {
        return new EntityDataSerializer.ForValueType<T>() {
            @Override
            public void write(FriendlyByteBuf buf, T value) {
                writer.accept((T)buf, value);
            }

            @Override
            public T read(FriendlyByteBuf buf) {
                return reader.apply((T)buf);
            }
        };
    }

    static <T> EntityDataSerializer<Optional<T>> optional(FriendlyByteBuf.Writer<T> writer, FriendlyByteBuf.Reader<T> reader) {
        return simple(writer.asOptional(), reader.asOptional());
    }

    static <T extends Enum<T>> EntityDataSerializer<T> simpleEnum(Class<T> enum_) {
        return simple(FriendlyByteBuf::writeEnum, (buf) -> {
            return buf.readEnum(enum_);
        });
    }

    static <T> EntityDataSerializer<T> simpleId(IdMap<T> registry) {
        return simple((buf, value) -> {
            buf.writeId(registry, (T)value);
        }, (buf) -> {
            return buf.<T>readById(registry);
        });
    }

    public interface ForValueType<T> extends EntityDataSerializer<T> {
        @Override
        default T copy(T value) {
            return value;
        }
    }
}
