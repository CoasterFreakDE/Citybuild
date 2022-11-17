package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public record EntityDataSource(String selectorPattern, @Nullable EntitySelector compiledSelector) implements DataSource {
    public EntityDataSource(String rawPath) {
        this(rawPath, compileSelector(rawPath));
    }

    @Nullable
    private static EntitySelector compileSelector(String rawSelector) {
        try {
            EntitySelectorParser entitySelectorParser = new EntitySelectorParser(new StringReader(rawSelector));
            return entitySelectorParser.parse();
        } catch (CommandSyntaxException var2) {
            return null;
        }
    }

    @Override
    public Stream<CompoundTag> getData(CommandSourceStack source) throws CommandSyntaxException {
        if (this.compiledSelector != null) {
            List<? extends Entity> list = this.compiledSelector.findEntities(source);
            return list.stream().map(NbtPredicate::getEntityTagToCompare);
        } else {
            return Stream.empty();
        }
    }

    @Override
    public String toString() {
        return "entity=" + this.selectorPattern;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            if (object instanceof EntityDataSource) {
                EntityDataSource entityDataSource = (EntityDataSource)object;
                if (this.selectorPattern.equals(entityDataSource.selectorPattern)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.selectorPattern.hashCode();
    }
}
