package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.pathfinder.Path;

public class SetClosestHomeAsWalkTarget extends Behavior<LivingEntity> {
    private static final int CACHE_TIMEOUT = 40;
    private static final int BATCH_SIZE = 5;
    private static final int RATE = 20;
    private static final int OK_DISTANCE_SQR = 4;
    private final float speedModifier;
    private final Long2LongMap batchCache = new Long2LongOpenHashMap();
    private int triedCount;
    private long lastUpdate;

    public SetClosestHomeAsWalkTarget(float speed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        if (world.getGameTime() - this.lastUpdate < 20L) {
            return false;
        } else {
            PathfinderMob pathfinderMob = (PathfinderMob)entity;
            PoiManager poiManager = world.getPoiManager();
            Optional<BlockPos> optional = poiManager.findClosest((poiType) -> {
                return poiType.is(PoiTypes.HOME);
            }, entity.blockPosition(), 48, PoiManager.Occupancy.ANY);
            return optional.isPresent() && !(optional.get().distSqr(pathfinderMob.blockPosition()) <= 4.0D);
        }
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        this.triedCount = 0;
        this.lastUpdate = world.getGameTime() + (long)world.getRandom().nextInt(20);
        PathfinderMob pathfinderMob = (PathfinderMob)entity;
        PoiManager poiManager = world.getPoiManager();
        Predicate<BlockPos> predicate = (pos) -> {
            long l = pos.asLong();
            if (this.batchCache.containsKey(l)) {
                return false;
            } else if (++this.triedCount >= 5) {
                return false;
            } else {
                this.batchCache.put(l, this.lastUpdate + 40L);
                return true;
            }
        };
        Set<Pair<Holder<PoiType>, BlockPos>> set = poiManager.findAllWithType((poiType) -> {
            return poiType.is(PoiTypes.HOME);
        }, predicate, entity.blockPosition(), 48, PoiManager.Occupancy.ANY).collect(Collectors.toSet());
        Path path = AcquirePoi.findPathToPois(pathfinderMob, set);
        if (path != null && path.canReach()) {
            BlockPos blockPos = path.getTarget();
            Optional<Holder<PoiType>> optional = poiManager.getType(blockPos);
            if (optional.isPresent()) {
                entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(blockPos, this.speedModifier, 1));
                DebugPackets.sendPoiTicketCountPacket(world, blockPos);
            }
        } else if (this.triedCount < 5) {
            this.batchCache.long2LongEntrySet().removeIf((entry) -> {
                return entry.getLongValue() < this.lastUpdate;
            });
        }

    }
}
