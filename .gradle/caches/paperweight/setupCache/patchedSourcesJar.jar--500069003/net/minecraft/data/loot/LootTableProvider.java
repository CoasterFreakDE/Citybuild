package net.minecraft.data.loot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTableProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DataGenerator.PathProvider pathProvider;
    private final List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> subProviders = ImmutableList.of(Pair.of(FishingLoot::new, LootContextParamSets.FISHING), Pair.of(ChestLoot::new, LootContextParamSets.CHEST), Pair.of(EntityLoot::new, LootContextParamSets.ENTITY), Pair.of(BlockLoot::new, LootContextParamSets.BLOCK), Pair.of(PiglinBarterLoot::new, LootContextParamSets.PIGLIN_BARTER), Pair.of(GiftLoot::new, LootContextParamSets.GIFT));

    public LootTableProvider(DataGenerator root) {
        this.pathProvider = root.createPathProvider(DataGenerator.Target.DATA_PACK, "loot_tables");
    }

    @Override
    public void run(CachedOutput writer) {
        Map<ResourceLocation, LootTable> map = Maps.newHashMap();
        this.subProviders.forEach((generator) -> {
            generator.getFirst().get().accept((id, builder) -> {
                if (map.put(id, builder.setParamSet(generator.getSecond()).build()) != null) {
                    throw new IllegalStateException("Duplicate loot table " + id);
                }
            });
        });
        ValidationContext validationContext = new ValidationContext(LootContextParamSets.ALL_PARAMS, (id) -> {
            return null;
        }, map::get);

        for(ResourceLocation resourceLocation : Sets.difference(BuiltInLootTables.all(), map.keySet())) {
            validationContext.reportProblem("Missing built-in table: " + resourceLocation);
        }

        map.forEach((id, table) -> {
            LootTables.validate(validationContext, id, table);
        });
        Multimap<String, String> multimap = validationContext.getProblems();
        if (!multimap.isEmpty()) {
            multimap.forEach((name, message) -> {
                LOGGER.warn("Found validation problem in {}: {}", name, message);
            });
            throw new IllegalStateException("Failed to validate loot tables, see logs");
        } else {
            map.forEach((id, table) -> {
                Path path = this.pathProvider.json(id);

                try {
                    DataProvider.saveStable(writer, LootTables.serialize(table), path);
                } catch (IOException var6) {
                    LOGGER.error("Couldn't save loot table {}", path, var6);
                }

            });
        }
    }

    @Override
    public String getName() {
        return "LootTables";
    }
}
