package net.minecraft.network.chat.contents;

import java.util.Optional;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public record LiteralContents(String text) implements ComponentContents {
    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
        return visitor.accept(this.text);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
        return visitor.accept(style, this.text);
    }

    @Override
    public String toString() {
        return "literal{" + this.text + "}";
    }
}
