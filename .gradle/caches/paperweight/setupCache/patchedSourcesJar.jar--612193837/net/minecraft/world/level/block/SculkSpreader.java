package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class SculkSpreader {
    public static final int MAX_GROWTH_RATE_RADIUS = 24;
    public static final int MAX_CHARGE = 1000;
    public static final float MAX_DECAY_FACTOR = 0.5F;
    private static final int MAX_CURSORS = 32;
    public static final int SHRIEKER_PLACEMENT_RATE = 11;
    final boolean isWorldGeneration;
    private final TagKey<Block> replaceableBlocks;
    private final int growthSpawnCost;
    private final int noGrowthRadius;
    private final int chargeDecayRate;
    private final int additionalDecayRate;
    private List<SculkSpreader.ChargeCursor> cursors = new ArrayList<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    public SculkSpreader(boolean worldGen, TagKey<Block> replaceableTag, int extraBlockChance, int maxDistance, int spreadChance, int decayChance) {
        this.isWorldGeneration = worldGen;
        this.replaceableBlocks = replaceableTag;
        this.growthSpawnCost = extraBlockChance;
        this.noGrowthRadius = maxDistance;
        this.chargeDecayRate = spreadChance;
        this.additionalDecayRate = decayChance;
    }

    public static SculkSpreader createLevelSpreader() {
        return new SculkSpreader(false, BlockTags.SCULK_REPLACEABLE, 10, 4, 10, 5);
    }

    public static SculkSpreader createWorldGenSpreader() {
        return new SculkSpreader(true, BlockTags.SCULK_REPLACEABLE_WORLD_GEN, 50, 1, 5, 10);
    }

    public TagKey<Block> replaceableBlocks() {
        return this.replaceableBlocks;
    }

    public int growthSpawnCost() {
        return this.growthSpawnCost;
    }

    public int noGrowthRadius() {
        return this.noGrowthRadius;
    }

    public int chargeDecayRate() {
        return this.chargeDecayRate;
    }

    public int additionalDecayRate() {
        return this.additionalDecayRate;
    }

    public boolean isWorldGeneration() {
        return this.isWorldGeneration;
    }

    @VisibleForTesting
    public List<SculkSpreader.ChargeCursor> getCursors() {
        return this.cursors;
    }

    public void clear() {
        this.cursors.clear();
    }

    public void load(CompoundTag nbt) {
        if (nbt.contains("cursors", 9)) {
            this.cursors.clear();
            List<SculkSpreader.ChargeCursor> list = SculkSpreader.ChargeCursor.CODEC.listOf().parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getList("cursors", 10))).resultOrPartial(LOGGER::error).orElseGet(ArrayList::new);
            int i = Math.min(list.size(), 32);

            for(int j = 0; j < i; ++j) {
                this.addCursor(list.get(j));
            }
        }

    }

    public void save(CompoundTag nbt) {
        SculkSpreader.ChargeCursor.CODEC.listOf().encodeStart(NbtOps.INSTANCE, this.cursors).resultOrPartial(LOGGER::error).ifPresent((cursorsNbt) -> {
            nbt.put("cursors", cursorsNbt);
        });
    }

    public void addCursors(BlockPos pos, int charge) {
        while(charge > 0) {
            int i = Math.min(charge, 1000);
            this.addCursor(new SculkSpreader.ChargeCursor(pos, i));
            charge -= i;
        }

    }

    private void addCursor(SculkSpreader.ChargeCursor cursor) {
        if (this.cursors.size() < 32) {
            this.cursors.add(cursor);
        }
    }

    public void updateCursors(LevelAccessor world, BlockPos pos, RandomSource random, boolean shouldConvertToBlock) {
        if (!this.cursors.isEmpty()) {
            List<SculkSpreader.ChargeCursor> list = new ArrayList<>();
            Map<BlockPos, SculkSpreader.ChargeCursor> map = new HashMap<>();
            Object2IntMap<BlockPos> object2IntMap = new Object2IntOpenHashMap<>();

            for(SculkSpreader.ChargeCursor chargeCursor : this.cursors) {
                chargeCursor.update(world, pos, random, this, shouldConvertToBlock);
                if (chargeCursor.charge <= 0) {
                    world.levelEvent(3006, chargeCursor.getPos(), 0);
                } else {
                    BlockPos blockPos = chargeCursor.getPos();
                    object2IntMap.computeInt(blockPos, (posx, charge) -> {
                        return (charge == null ? 0 : charge) + chargeCursor.charge;
                    });
                    SculkSpreader.ChargeCursor chargeCursor2 = map.get(blockPos);
                    if (chargeCursor2 == null) {
                        map.put(blockPos, chargeCursor);
                        list.add(chargeCursor);
                    } else if (!this.isWorldGeneration() && chargeCursor.charge + chargeCursor2.charge <= 1000) {
                        chargeCursor2.mergeWith(chargeCursor);
                    } else {
                        list.add(chargeCursor);
                        if (chargeCursor.charge < chargeCursor2.charge) {
                            map.put(blockPos, chargeCursor);
                        }
                    }
                }
            }

            for(Object2IntMap.Entry<BlockPos> entry : object2IntMap.object2IntEntrySet()) {
                BlockPos blockPos2 = entry.getKey();
                int i = entry.getIntValue();
                SculkSpreader.ChargeCursor chargeCursor3 = map.get(blockPos2);
                Collection<Direction> collection = chargeCursor3 == null ? null : chargeCursor3.getFacingData();
                if (i > 0 && collection != null) {
                    int j = (int)(Math.log1p((double)i) / (double)2.3F) + 1;
                    int k = (j << 6) + MultifaceBlock.pack(collection);
                    world.levelEvent(3006, blockPos2, k);
                }
            }

            this.cursors = list;
        }
    }

    public static class ChargeCursor {
        private static final ObjectArrayList<Vec3i> NON_CORNER_NEIGHBOURS = Util.make(new ObjectArrayList<>(18), (objectArrayList) -> {
            BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1)).filter((pos) -> {
                return (pos.getX() == 0 || pos.getY() == 0 || pos.getZ() == 0) && !pos.equals(BlockPos.ZERO);
            }).map(BlockPos::immutable).forEach(objectArrayList::add);
        });
        public static final int MAX_CURSOR_DECAY_DELAY = 1;
        private BlockPos pos;
        int charge;
        private int updateDelay;
        private int decayDelay;
        @Nullable
        private Set<Direction> facings;
        private static final Codec<Set<Direction>> DIRECTION_SET = Direction.CODEC.listOf().xmap((directions) -> {
            return Sets.newEnumSet(directions, Direction.class);
        }, Lists::newArrayList);
        public static final Codec<SculkSpreader.ChargeCursor> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(BlockPos.CODEC.fieldOf("pos").forGetter(SculkSpreader.ChargeCursor::getPos), Codec.intRange(0, 1000).fieldOf("charge").orElse(0).forGetter(SculkSpreader.ChargeCursor::getCharge), Codec.intRange(0, 1).fieldOf("decay_delay").orElse(1).forGetter(SculkSpreader.ChargeCursor::getDecayDelay), Codec.intRange(0, Integer.MAX_VALUE).fieldOf("update_delay").orElse(0).forGetter((cursor) -> {
                return cursor.updateDelay;
            }), DIRECTION_SET.optionalFieldOf("facings").forGetter((cursor) -> {
                return Optional.ofNullable(cursor.getFacingData());
            })).apply(instance, SculkSpreader.ChargeCursor::new);
        });

        private ChargeCursor(BlockPos pos, int charge, int decay, int update, Optional<Set<Direction>> faces) {
            this.pos = pos;
            this.charge = charge;
            this.decayDelay = decay;
            this.updateDelay = update;
            this.facings = faces.orElse((Set<Direction>)null);
        }

        public ChargeCursor(BlockPos pos, int charge) {
            this(pos, charge, 1, 0, Optional.empty());
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public int getCharge() {
            return this.charge;
        }

        public int getDecayDelay() {
            return this.decayDelay;
        }

        @Nullable
        public Set<Direction> getFacingData() {
            return this.facings;
        }

        private boolean shouldUpdate(LevelAccessor world, BlockPos pos, boolean worldGen) {
            if (this.charge <= 0) {
                return false;
            } else if (worldGen) {
                return true;
            } else if (world instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)world;
                return serverLevel.shouldTickBlocksAt(pos);
            } else {
                return false;
            }
        }

        public void update(LevelAccessor world, BlockPos pos, RandomSource random, SculkSpreader spreadManager, boolean shouldConvertToBlock) {
            if (this.shouldUpdate(world, pos, spreadManager.isWorldGeneration)) {
                if (this.updateDelay > 0) {
                    --this.updateDelay;
                } else {
                    BlockState blockState = world.getBlockState(this.pos);
                    SculkBehaviour sculkBehaviour = getBlockBehaviour(blockState);
                    if (shouldConvertToBlock && sculkBehaviour.attemptSpreadVein(world, this.pos, blockState, this.facings, spreadManager.isWorldGeneration())) {
                        if (sculkBehaviour.canChangeBlockStateOnSpread()) {
                            blockState = world.getBlockState(this.pos);
                            sculkBehaviour = getBlockBehaviour(blockState);
                        }

                        world.playSound((Player)null, this.pos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }

                    this.charge = sculkBehaviour.attemptUseCharge(this, world, pos, random, spreadManager, shouldConvertToBlock);
                    if (this.charge <= 0) {
                        sculkBehaviour.onDischarged(world, blockState, this.pos, random);
                    } else {
                        BlockPos blockPos = getValidMovementPos(world, this.pos, random);
                        if (blockPos != null) {
                            sculkBehaviour.onDischarged(world, blockState, this.pos, random);
                            this.pos = blockPos.immutable();
                            if (spreadManager.isWorldGeneration() && !this.pos.closerThan(new Vec3i(pos.getX(), this.pos.getY(), pos.getZ()), 15.0D)) {
                                this.charge = 0;
                                return;
                            }

                            blockState = world.getBlockState(blockPos);
                        }

                        if (blockState.getBlock() instanceof SculkBehaviour) {
                            this.facings = MultifaceBlock.availableFaces(blockState);
                        }

                        this.decayDelay = sculkBehaviour.updateDecayDelay(this.decayDelay);
                        this.updateDelay = sculkBehaviour.getSculkSpreadDelay();
                    }
                }
            }
        }

        void mergeWith(SculkSpreader.ChargeCursor cursor) {
            this.charge += cursor.charge;
            cursor.charge = 0;
            this.updateDelay = Math.min(this.updateDelay, cursor.updateDelay);
        }

        private static SculkBehaviour getBlockBehaviour(BlockState state) {
            Block var2 = state.getBlock();
            SculkBehaviour var10000;
            if (var2 instanceof SculkBehaviour sculkBehaviour) {
                var10000 = sculkBehaviour;
            } else {
                var10000 = SculkBehaviour.DEFAULT;
            }

            return var10000;
        }

        private static List<Vec3i> getRandomizedNonCornerNeighbourOffsets(RandomSource random) {
            return Util.shuffledCopy(NON_CORNER_NEIGHBOURS, random);
        }

        @Nullable
        private static BlockPos getValidMovementPos(LevelAccessor world, BlockPos pos, RandomSource random) {
            BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
            BlockPos.MutableBlockPos mutableBlockPos2 = pos.mutable();

            for(Vec3i vec3i : getRandomizedNonCornerNeighbourOffsets(random)) {
                mutableBlockPos2.setWithOffset(pos, vec3i);
                BlockState blockState = world.getBlockState(mutableBlockPos2);
                if (blockState.getBlock() instanceof SculkBehaviour && isMovementUnobstructed(world, pos, mutableBlockPos2)) {
                    mutableBlockPos.set(mutableBlockPos2);
                    if (SculkVeinBlock.hasSubstrateAccess(world, blockState, mutableBlockPos2)) {
                        break;
                    }
                }
            }

            return mutableBlockPos.equals(pos) ? null : mutableBlockPos;
        }

        private static boolean isMovementUnobstructed(LevelAccessor world, BlockPos sourcePos, BlockPos targetPos) {
            if (sourcePos.distManhattan(targetPos) == 1) {
                return true;
            } else {
                BlockPos blockPos = targetPos.subtract(sourcePos);
                Direction direction = Direction.fromAxisAndDirection(Direction.Axis.X, blockPos.getX() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
                Direction direction2 = Direction.fromAxisAndDirection(Direction.Axis.Y, blockPos.getY() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
                Direction direction3 = Direction.fromAxisAndDirection(Direction.Axis.Z, blockPos.getZ() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
                if (blockPos.getX() == 0) {
                    return isUnobstructed(world, sourcePos, direction2) || isUnobstructed(world, sourcePos, direction3);
                } else if (blockPos.getY() == 0) {
                    return isUnobstructed(world, sourcePos, direction) || isUnobstructed(world, sourcePos, direction3);
                } else {
                    return isUnobstructed(world, sourcePos, direction) || isUnobstructed(world, sourcePos, direction2);
                }
            }
        }

        private static boolean isUnobstructed(LevelAccessor world, BlockPos pos, Direction direction) {
            BlockPos blockPos = pos.relative(direction);
            return !world.getBlockState(blockPos).isFaceSturdy(world, blockPos, direction.getOpposite());
        }
    }
}
