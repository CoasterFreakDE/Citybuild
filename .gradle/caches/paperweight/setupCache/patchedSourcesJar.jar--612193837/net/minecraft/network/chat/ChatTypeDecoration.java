package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public record ChatTypeDecoration(String translationKey, List<ChatTypeDecoration.Parameter> parameters, Style style) {
    public static final Codec<ChatTypeDecoration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.STRING.fieldOf("translation_key").forGetter(ChatTypeDecoration::translationKey), ChatTypeDecoration.Parameter.CODEC.listOf().fieldOf("parameters").forGetter(ChatTypeDecoration::parameters), Style.FORMATTING_CODEC.optionalFieldOf("style", Style.EMPTY).forGetter(ChatTypeDecoration::style)).apply(instance, ChatTypeDecoration::new);
    });

    public static ChatTypeDecoration withSender(String translationKey) {
        return new ChatTypeDecoration(translationKey, List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT), Style.EMPTY);
    }

    public static ChatTypeDecoration incomingDirectMessage(String translationKey) {
        Style style = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
        return new ChatTypeDecoration(translationKey, List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT), style);
    }

    public static ChatTypeDecoration outgoingDirectMessage(String translationKey) {
        Style style = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
        return new ChatTypeDecoration(translationKey, List.of(ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.CONTENT), style);
    }

    public static ChatTypeDecoration teamMessage(String translationKey) {
        return new ChatTypeDecoration(translationKey, List.of(ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT), Style.EMPTY);
    }

    public Component decorate(Component content, ChatType.Bound params) {
        Object[] objects = this.resolveParameters(content, params);
        return Component.translatable(this.translationKey, objects).withStyle(this.style);
    }

    private Component[] resolveParameters(Component content, ChatType.Bound params) {
        Component[] components = new Component[this.parameters.size()];

        for(int i = 0; i < components.length; ++i) {
            ChatTypeDecoration.Parameter parameter = this.parameters.get(i);
            components[i] = parameter.select(content, params);
        }

        return components;
    }

    public static enum Parameter implements StringRepresentable {
        SENDER("sender", (content, params) -> {
            return params.name();
        }),
        TARGET("target", (content, params) -> {
            return params.targetName();
        }),
        CONTENT("content", (content, params) -> {
            return content;
        });

        public static final Codec<ChatTypeDecoration.Parameter> CODEC = StringRepresentable.fromEnum(ChatTypeDecoration.Parameter::values);
        private final String name;
        private final ChatTypeDecoration.Parameter.Selector selector;

        private Parameter(String name, ChatTypeDecoration.Parameter.Selector selector) {
            this.name = name;
            this.selector = selector;
        }

        public Component select(Component content, ChatType.Bound params) {
            Component component = this.selector.select(content, params);
            return Objects.requireNonNullElse(component, CommonComponents.EMPTY);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public interface Selector {
            @Nullable
            Component select(Component content, ChatType.Bound params);
        }
    }
}
