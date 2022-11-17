package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Optional;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class PlaceCommand {
    private static final SimpleCommandExceptionType ERROR_FEATURE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.feature.failed"));
    private static final SimpleCommandExceptionType ERROR_JIGSAW_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.jigsaw.failed"));
    private static final SimpleCommandExceptionType ERROR_STRUCTURE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.structure.failed"));
    private static final DynamicCommandExceptionType ERROR_TEMPLATE_INVALID = new DynamicCommandExceptionType((id) -> {
        return Component.translatable("commands.place.template.invalid", id);
    });
    private static final SimpleCommandExceptionType ERROR_TEMPLATE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.template.failed"));
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATES = (context, builder) -> {
        StructureTemplateManager structureTemplateManager = context.getSource().getLevel().getStructureManager();
        return SharedSuggestionProvider.suggestResource(structureTemplateManager.listTemplates(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("place").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("feature").then(Commands.argument("feature", ResourceKeyArgument.key(Registry.CONFIGURED_FEATURE_REGISTRY)).executes((context) -> {
            return placeFeature(context.getSource(), ResourceKeyArgument.getConfiguredFeature(context, "feature"), new BlockPos(context.getSource().getPosition()));
        }).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((context) -> {
            return placeFeature(context.getSource(), ResourceKeyArgument.getConfiguredFeature(context, "feature"), BlockPosArgument.getLoadedBlockPos(context, "pos"));
        })))).then(Commands.literal("jigsaw").then(Commands.argument("pool", ResourceKeyArgument.key(Registry.TEMPLATE_POOL_REGISTRY)).then(Commands.argument("target", ResourceLocationArgument.id()).then(Commands.argument("max_depth", IntegerArgumentType.integer(1, 7)).executes((context) -> {
            return placeJigsaw(context.getSource(), ResourceKeyArgument.getStructureTemplatePool(context, "pool"), ResourceLocationArgument.getId(context, "target"), IntegerArgumentType.getInteger(context, "max_depth"), new BlockPos(context.getSource().getPosition()));
        }).then(Commands.argument("position", BlockPosArgument.blockPos()).executes((context) -> {
            return placeJigsaw(context.getSource(), ResourceKeyArgument.getStructureTemplatePool(context, "pool"), ResourceLocationArgument.getId(context, "target"), IntegerArgumentType.getInteger(context, "max_depth"), BlockPosArgument.getLoadedBlockPos(context, "position"));
        })))))).then(Commands.literal("structure").then(Commands.argument("structure", ResourceKeyArgument.key(Registry.STRUCTURE_REGISTRY)).executes((context) -> {
            return placeStructure(context.getSource(), ResourceKeyArgument.getStructure(context, "structure"), new BlockPos(context.getSource().getPosition()));
        }).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((context) -> {
            return placeStructure(context.getSource(), ResourceKeyArgument.getStructure(context, "structure"), BlockPosArgument.getLoadedBlockPos(context, "pos"));
        })))).then(Commands.literal("template").then(Commands.argument("template", ResourceLocationArgument.id()).suggests(SUGGEST_TEMPLATES).executes((context) -> {
            return placeTemplate(context.getSource(), ResourceLocationArgument.getId(context, "template"), new BlockPos(context.getSource().getPosition()), Rotation.NONE, Mirror.NONE, 1.0F, 0);
        }).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((context) -> {
            return placeTemplate(context.getSource(), ResourceLocationArgument.getId(context, "template"), BlockPosArgument.getLoadedBlockPos(context, "pos"), Rotation.NONE, Mirror.NONE, 1.0F, 0);
        }).then(Commands.argument("rotation", TemplateRotationArgument.templateRotation()).executes((context) -> {
            return placeTemplate(context.getSource(), ResourceLocationArgument.getId(context, "template"), BlockPosArgument.getLoadedBlockPos(context, "pos"), TemplateRotationArgument.getRotation(context, "rotation"), Mirror.NONE, 1.0F, 0);
        }).then(Commands.argument("mirror", TemplateMirrorArgument.templateMirror()).executes((context) -> {
            return placeTemplate(context.getSource(), ResourceLocationArgument.getId(context, "template"), BlockPosArgument.getLoadedBlockPos(context, "pos"), TemplateRotationArgument.getRotation(context, "rotation"), TemplateMirrorArgument.getMirror(context, "mirror"), 1.0F, 0);
        }).then(Commands.argument("integrity", FloatArgumentType.floatArg(0.0F, 1.0F)).executes((context) -> {
            return placeTemplate(context.getSource(), ResourceLocationArgument.getId(context, "template"), BlockPosArgument.getLoadedBlockPos(context, "pos"), TemplateRotationArgument.getRotation(context, "rotation"), TemplateMirrorArgument.getMirror(context, "mirror"), FloatArgumentType.getFloat(context, "integrity"), 0);
        }).then(Commands.argument("seed", IntegerArgumentType.integer()).executes((context) -> {
            return placeTemplate(context.getSource(), ResourceLocationArgument.getId(context, "template"), BlockPosArgument.getLoadedBlockPos(context, "pos"), TemplateRotationArgument.getRotation(context, "rotation"), TemplateMirrorArgument.getMirror(context, "mirror"), FloatArgumentType.getFloat(context, "integrity"), IntegerArgumentType.getInteger(context, "seed"));
        })))))))));
    }

    public static int placeFeature(CommandSourceStack source, Holder<ConfiguredFeature<?, ?>> feature, BlockPos pos) throws CommandSyntaxException {
        ServerLevel serverLevel = source.getLevel();
        ConfiguredFeature<?, ?> configuredFeature = feature.value();
        ChunkPos chunkPos = new ChunkPos(pos);
        checkLoaded(serverLevel, new ChunkPos(chunkPos.x - 1, chunkPos.z - 1), new ChunkPos(chunkPos.x + 1, chunkPos.z + 1));
        if (!configuredFeature.place(serverLevel, serverLevel.getChunkSource().getGenerator(), serverLevel.getRandom(), pos)) {
            throw ERROR_FEATURE_FAILED.create();
        } else {
            String string = feature.unwrapKey().map((key) -> {
                return key.location().toString();
            }).orElse("[unregistered]");
            source.sendSuccess(Component.translatable("commands.place.feature.success", string, pos.getX(), pos.getY(), pos.getZ()), true);
            return 1;
        }
    }

    public static int placeJigsaw(CommandSourceStack source, Holder<StructureTemplatePool> structurePool, ResourceLocation id, int maxDepth, BlockPos pos) throws CommandSyntaxException {
        ServerLevel serverLevel = source.getLevel();
        if (!JigsawPlacement.generateJigsaw(serverLevel, structurePool, id, maxDepth, pos, false)) {
            throw ERROR_JIGSAW_FAILED.create();
        } else {
            source.sendSuccess(Component.translatable("commands.place.jigsaw.success", pos.getX(), pos.getY(), pos.getZ()), true);
            return 1;
        }
    }

    public static int placeStructure(CommandSourceStack source, Holder<Structure> structure, BlockPos pos) throws CommandSyntaxException {
        ServerLevel serverLevel = source.getLevel();
        Structure structure2 = structure.value();
        ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
        StructureStart structureStart = structure2.generate(source.registryAccess(), chunkGenerator, chunkGenerator.getBiomeSource(), serverLevel.getChunkSource().randomState(), serverLevel.getStructureManager(), serverLevel.getSeed(), new ChunkPos(pos), 0, serverLevel, (holder) -> {
            return true;
        });
        if (!structureStart.isValid()) {
            throw ERROR_STRUCTURE_FAILED.create();
        } else {
            BoundingBox boundingBox = structureStart.getBoundingBox();
            ChunkPos chunkPos = new ChunkPos(SectionPos.blockToSectionCoord(boundingBox.minX()), SectionPos.blockToSectionCoord(boundingBox.minZ()));
            ChunkPos chunkPos2 = new ChunkPos(SectionPos.blockToSectionCoord(boundingBox.maxX()), SectionPos.blockToSectionCoord(boundingBox.maxZ()));
            checkLoaded(serverLevel, chunkPos, chunkPos2);
            ChunkPos.rangeClosed(chunkPos, chunkPos2).forEach((chunkPosx) -> {
                structureStart.placeInChunk(serverLevel, serverLevel.structureManager(), chunkGenerator, serverLevel.getRandom(), new BoundingBox(chunkPosx.getMinBlockX(), serverLevel.getMinBuildHeight(), chunkPosx.getMinBlockZ(), chunkPosx.getMaxBlockX(), serverLevel.getMaxBuildHeight(), chunkPosx.getMaxBlockZ()), chunkPosx);
            });
            String string = structure.unwrapKey().map((key) -> {
                return key.location().toString();
            }).orElse("[unregistered]");
            source.sendSuccess(Component.translatable("commands.place.structure.success", string, pos.getX(), pos.getY(), pos.getZ()), true);
            return 1;
        }
    }

    public static int placeTemplate(CommandSourceStack source, ResourceLocation id, BlockPos pos, Rotation rotation, Mirror mirror, float integrity, int seed) throws CommandSyntaxException {
        ServerLevel serverLevel = source.getLevel();
        StructureTemplateManager structureTemplateManager = serverLevel.getStructureManager();

        Optional<StructureTemplate> optional;
        try {
            optional = structureTemplateManager.get(id);
        } catch (ResourceLocationException var13) {
            throw ERROR_TEMPLATE_INVALID.create(id);
        }

        if (optional.isEmpty()) {
            throw ERROR_TEMPLATE_INVALID.create(id);
        } else {
            StructureTemplate structureTemplate = optional.get();
            checkLoaded(serverLevel, new ChunkPos(pos), new ChunkPos(pos.offset(structureTemplate.getSize())));
            StructurePlaceSettings structurePlaceSettings = (new StructurePlaceSettings()).setMirror(mirror).setRotation(rotation);
            if (integrity < 1.0F) {
                structurePlaceSettings.clearProcessors().addProcessor(new BlockRotProcessor(integrity)).setRandom(StructureBlockEntity.createRandom((long)seed));
            }

            boolean bl = structureTemplate.placeInWorld(serverLevel, pos, pos, structurePlaceSettings, StructureBlockEntity.createRandom((long)seed), 2);
            if (!bl) {
                throw ERROR_TEMPLATE_FAILED.create();
            } else {
                source.sendSuccess(Component.translatable("commands.place.template.success", id, pos.getX(), pos.getY(), pos.getZ()), true);
                return 1;
            }
        }
    }

    private static void checkLoaded(ServerLevel world, ChunkPos pos1, ChunkPos pos2) throws CommandSyntaxException {
        if (ChunkPos.rangeClosed(pos1, pos2).filter((pos) -> {
            return !world.isLoaded(pos.getWorldPosition());
        }).findAny().isPresent()) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
        }
    }
}
