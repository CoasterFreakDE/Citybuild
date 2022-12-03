package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

public class LocateCommand {
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_NOT_FOUND = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("commands.locate.structure.not_found", id);
    });
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("commands.locate.structure.invalid", id);
    });
    private static final DynamicCommandExceptionType ERROR_BIOME_NOT_FOUND = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("commands.locate.biome.not_found", id);
    });
    private static final DynamicCommandExceptionType ERROR_BIOME_INVALID = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("commands.locate.biome.invalid", id);
    });
    private static final DynamicCommandExceptionType ERROR_POI_NOT_FOUND = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("commands.locate.poi.not_found", id);
    });
    private static final DynamicCommandExceptionType ERROR_POI_INVALID = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("commands.locate.poi.invalid", id);
    });
    private static final int MAX_STRUCTURE_SEARCH_RADIUS = 100;
    private static final int MAX_BIOME_SEARCH_RADIUS = 6400;
    private static final int BIOME_SAMPLE_RESOLUTION_HORIZONTAL = 32;
    private static final int BIOME_SAMPLE_RESOLUTION_VERTICAL = 64;
    private static final int POI_SEARCH_RADIUS = 256;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("locate").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("structure").then(Commands.argument("structure", ResourceOrTagLocationArgument.resourceOrTag(Registry.STRUCTURE_REGISTRY)).executes((commandContext) -> {
            return locateStructure(commandContext.getSource(), ResourceOrTagLocationArgument.getRegistryType(commandContext, "structure", Registry.STRUCTURE_REGISTRY, ERROR_STRUCTURE_INVALID));
        }))).then(Commands.literal("biome").then(Commands.argument("biome", ResourceOrTagLocationArgument.resourceOrTag(Registry.BIOME_REGISTRY)).executes((commandContext) -> {
            return locateBiome(commandContext.getSource(), ResourceOrTagLocationArgument.getRegistryType(commandContext, "biome", Registry.BIOME_REGISTRY, ERROR_BIOME_INVALID));
        }))).then(Commands.literal("poi").then(Commands.argument("poi", ResourceOrTagLocationArgument.resourceOrTag(Registry.POINT_OF_INTEREST_TYPE_REGISTRY)).executes((commandContext) -> {
            return locatePoi(commandContext.getSource(), ResourceOrTagLocationArgument.getRegistryType(commandContext, "poi", Registry.POINT_OF_INTEREST_TYPE_REGISTRY, ERROR_POI_INVALID));
        }))));
    }

    private static Optional<? extends HolderSet.ListBacked<Structure>> getHolders(ResourceOrTagLocationArgument.Result<Structure> predicate, Registry<Structure> structureRegistry) {
        return predicate.unwrap().map((key) -> {
            return structureRegistry.getHolder(key).map((entry) -> {
                return HolderSet.direct(entry);
            });
        }, structureRegistry::getTag);
    }

    private static int locateStructure(CommandSourceStack source, ResourceOrTagLocationArgument.Result<Structure> predicate) throws CommandSyntaxException {
        Registry<Structure> registry = source.getLevel().registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
        HolderSet<Structure> holderSet = getHolders(predicate, registry).orElseThrow(() -> {
            return ERROR_STRUCTURE_INVALID.create(predicate.asPrintable());
        });
        BlockPos blockPos = new BlockPos(source.getPosition());
        ServerLevel serverLevel = source.getLevel();
        Pair<BlockPos, Holder<Structure>> pair = serverLevel.getChunkSource().getGenerator().findNearestMapStructure(serverLevel, holderSet, blockPos, 100, false);
        if (pair == null) {
            throw ERROR_STRUCTURE_NOT_FOUND.create(predicate.asPrintable());
        } else {
            return showLocateResult(source, predicate, blockPos, pair, "commands.locate.structure.success", false);
        }
    }

    private static int locateBiome(CommandSourceStack source, ResourceOrTagLocationArgument.Result<Biome> predicate) throws CommandSyntaxException {
        BlockPos blockPos = new BlockPos(source.getPosition());
        Pair<BlockPos, Holder<Biome>> pair = source.getLevel().findClosestBiome3d(predicate, blockPos, 6400, 32, 64);
        if (pair == null) {
            throw ERROR_BIOME_NOT_FOUND.create(predicate.asPrintable());
        } else {
            return showLocateResult(source, predicate, blockPos, pair, "commands.locate.biome.success", true);
        }
    }

    private static int locatePoi(CommandSourceStack source, ResourceOrTagLocationArgument.Result<PoiType> predicate) throws CommandSyntaxException {
        BlockPos blockPos = new BlockPos(source.getPosition());
        ServerLevel serverLevel = source.getLevel();
        Optional<Pair<Holder<PoiType>, BlockPos>> optional = serverLevel.getPoiManager().findClosestWithType(predicate, blockPos, 256, PoiManager.Occupancy.ANY);
        if (optional.isEmpty()) {
            throw ERROR_POI_NOT_FOUND.create(predicate.asPrintable());
        } else {
            return showLocateResult(source, predicate, blockPos, optional.get().swap(), "commands.locate.poi.success", false);
        }
    }

    public static int showLocateResult(CommandSourceStack source, ResourceOrTagLocationArgument.Result<?> structure, BlockPos currentPos, Pair<BlockPos, ? extends Holder<?>> structurePosAndEntry, String successMessage, boolean bl) {
        BlockPos blockPos = structurePosAndEntry.getFirst();
        String string = structure.unwrap().map((key) -> {
            return key.location().toString();
        }, (key) -> {
            return "#" + key.location() + " (" + (String)structurePosAndEntry.getSecond().unwrapKey().map((keyx) -> {
                return keyx.location().toString();
            }).orElse("[unregistered]") + ")";
        });
        int i = bl ? Mth.floor(Mth.sqrt((float)currentPos.distSqr(blockPos))) : Mth.floor(dist(currentPos.getX(), currentPos.getZ(), blockPos.getX(), blockPos.getZ()));
        String string2 = bl ? String.valueOf(blockPos.getY()) : "~";
        Component component = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", blockPos.getX(), string2, blockPos.getZ())).withStyle((style) -> {
            return style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockPos.getX() + " " + string2 + " " + blockPos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")));
        });
        source.sendSuccess(Component.translatable(successMessage, string, component, i), false);
        return i;
    }

    private static float dist(int x1, int y1, int x2, int y2) {
        int i = x2 - x1;
        int j = y2 - y1;
        return Mth.sqrt((float)(i * i + j * j));
    }
}
