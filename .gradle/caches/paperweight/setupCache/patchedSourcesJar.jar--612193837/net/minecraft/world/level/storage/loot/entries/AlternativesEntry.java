package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.apache.commons.lang3.ArrayUtils;

public class AlternativesEntry extends CompositeEntryBase {
    AlternativesEntry(LootPoolEntryContainer[] children, LootItemCondition[] conditions) {
        super(children, conditions);
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.ALTERNATIVES;
    }

    @Override
    protected ComposableEntryContainer compose(ComposableEntryContainer[] children) {
        switch (children.length) {
            case 0:
                return ALWAYS_FALSE;
            case 1:
                return children[0];
            case 2:
                return children[0].or(children[1]);
            default:
                return (context, lootChoiceExpander) -> {
                    for(ComposableEntryContainer composableEntryContainer : children) {
                        if (composableEntryContainer.expand(context, lootChoiceExpander)) {
                            return true;
                        }
                    }

                    return false;
                };
        }
    }

    @Override
    public void validate(ValidationContext reporter) {
        super.validate(reporter);

        for(int i = 0; i < this.children.length - 1; ++i) {
            if (ArrayUtils.isEmpty((Object[])this.children[i].conditions)) {
                reporter.reportProblem("Unreachable entry!");
            }
        }

    }

    public static AlternativesEntry.Builder alternatives(LootPoolEntryContainer.Builder<?>... children) {
        return new AlternativesEntry.Builder(children);
    }

    public static <E> AlternativesEntry.Builder alternatives(Collection<E> children, Function<E, LootPoolEntryContainer.Builder<?>> toBuilderFunction) {
        return new AlternativesEntry.Builder(children.stream().map(toBuilderFunction::apply).toArray((i) -> {
            return new LootPoolEntryContainer.Builder[i];
        }));
    }

    public static class Builder extends LootPoolEntryContainer.Builder<AlternativesEntry.Builder> {
        private final List<LootPoolEntryContainer> entries = Lists.newArrayList();

        public Builder(LootPoolEntryContainer.Builder<?>... children) {
            for(LootPoolEntryContainer.Builder<?> builder : children) {
                this.entries.add(builder.build());
            }

        }

        @Override
        protected AlternativesEntry.Builder getThis() {
            return this;
        }

        @Override
        public AlternativesEntry.Builder otherwise(LootPoolEntryContainer.Builder<?> builder) {
            this.entries.add(builder.build());
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new AlternativesEntry(this.entries.toArray(new LootPoolEntryContainer[0]), this.getConditions());
        }
    }
}
