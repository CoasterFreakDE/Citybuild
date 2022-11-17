package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LongJumpToRandomPos<E extends Mob> extends Behavior<E> {
    protected static final int FIND_JUMP_TRIES = 20;
    private static final int PREPARE_JUMP_DURATION = 40;
    protected static final int MIN_PATHFIND_DISTANCE_TO_VALID_JUMP = 8;
    private static final int TIME_OUT_DURATION = 200;
    private static final List<Integer> ALLOWED_ANGLES = Lists.newArrayList(65, 70, 75, 80);
    private final UniformInt timeBetweenLongJumps;
    protected final int maxLongJumpHeight;
    protected final int maxLongJumpWidth;
    protected final float maxJumpVelocity;
    protected List<LongJumpToRandomPos.PossibleJump> jumpCandidates = Lists.newArrayList();
    protected Optional<Vec3> initialPosition = Optional.empty();
    @Nullable
    protected Vec3 chosenJump;
    protected int findJumpTries;
    protected long prepareJumpStart;
    private Function<E, SoundEvent> getJumpSound;
    private final Predicate<BlockState> acceptableLandingSpot;

    public LongJumpToRandomPos(UniformInt cooldownRange, int verticalRange, int horizontalRange, float maxRange, Function<E, SoundEvent> entityToSound) {
        this(cooldownRange, verticalRange, horizontalRange, maxRange, entityToSound, (state) -> {
            return false;
        });
    }

    public LongJumpToRandomPos(UniformInt cooldownRange, int verticalRange, int horizontalRange, float maxRange, Function<E, SoundEvent> entityToSound, Predicate<BlockState> jumpToPredicate) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_ABSENT), 200);
        this.timeBetweenLongJumps = cooldownRange;
        this.maxLongJumpHeight = verticalRange;
        this.maxLongJumpWidth = horizontalRange;
        this.maxJumpVelocity = maxRange;
        this.getJumpSound = entityToSound;
        this.acceptableLandingSpot = jumpToPredicate;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Mob entity) {
        boolean bl = entity.isOnGround() && !entity.isInWater() && !entity.isInLava() && !world.getBlockState(entity.blockPosition()).is(Blocks.HONEY_BLOCK);
        if (!bl) {
            entity.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(world.random) / 2);
        }

        return bl;
    }

    @Override
    protected boolean canStillUse(ServerLevel world, Mob entity, long time) {
        boolean bl = this.initialPosition.isPresent() && this.initialPosition.get().equals(entity.position()) && this.findJumpTries > 0 && !entity.isInWaterOrBubble() && (this.chosenJump != null || !this.jumpCandidates.isEmpty());
        if (!bl && entity.getBrain().getMemory(MemoryModuleType.LONG_JUMP_MID_JUMP).isEmpty()) {
            entity.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(world.random) / 2);
            entity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        }

        return bl;
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        this.chosenJump = null;
        this.findJumpTries = 20;
        this.initialPosition = Optional.of(entity.position());
        BlockPos blockPos = entity.blockPosition();
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        this.jumpCandidates = BlockPos.betweenClosedStream(i - this.maxLongJumpWidth, j - this.maxLongJumpHeight, k - this.maxLongJumpWidth, i + this.maxLongJumpWidth, j + this.maxLongJumpHeight, k + this.maxLongJumpWidth).filter((blockPos2) -> {
            return !blockPos2.equals(blockPos);
        }).map((blockPos2) -> {
            return new LongJumpToRandomPos.PossibleJump(blockPos2.immutable(), Mth.ceil(blockPos.distSqr(blockPos2)));
        }).collect(Collectors.toCollection(Lists::newArrayList));
    }

    @Override
    protected void tick(ServerLevel serverLevel, E mob, long l) {
        if (this.chosenJump != null) {
            if (l - this.prepareJumpStart >= 40L) {
                mob.setYRot(mob.yBodyRot);
                mob.setDiscardFriction(true);
                double d = this.chosenJump.length();
                double e = d + mob.getJumpBoostPower();
                mob.setDeltaMovement(this.chosenJump.scale(e / d));
                mob.getBrain().setMemory(MemoryModuleType.LONG_JUMP_MID_JUMP, true);
                serverLevel.playSound((Player)null, mob, this.getJumpSound.apply(mob), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        } else {
            --this.findJumpTries;
            this.pickCandidate(serverLevel, mob, l);
        }

    }

    protected void pickCandidate(ServerLevel serverLevel, E mob, long l) {
        while(true) {
            if (!this.jumpCandidates.isEmpty()) {
                Optional<LongJumpToRandomPos.PossibleJump> optional = this.getJumpCandidate(serverLevel);
                if (optional.isEmpty()) {
                    continue;
                }

                LongJumpToRandomPos.PossibleJump possibleJump = optional.get();
                BlockPos blockPos = possibleJump.getJumpTarget();
                if (!this.isAcceptableLandingPosition(serverLevel, mob, blockPos)) {
                    continue;
                }

                Vec3 vec3 = Vec3.atCenterOf(blockPos);
                Vec3 vec32 = this.calculateOptimalJumpVector(mob, vec3);
                if (vec32 == null) {
                    continue;
                }

                mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(blockPos));
                PathNavigation pathNavigation = mob.getNavigation();
                Path path = pathNavigation.createPath(blockPos, 0, 8);
                if (path != null && path.canReach()) {
                    continue;
                }

                this.chosenJump = vec32;
                this.prepareJumpStart = l;
                return;
            }

            return;
        }
    }

    protected Optional<LongJumpToRandomPos.PossibleJump> getJumpCandidate(ServerLevel world) {
        Optional<LongJumpToRandomPos.PossibleJump> optional = WeightedRandom.getRandomItem(world.random, this.jumpCandidates);
        optional.ifPresent(this.jumpCandidates::remove);
        return optional;
    }

    protected boolean isAcceptableLandingPosition(ServerLevel world, E entity, BlockPos pos) {
        BlockPos blockPos = entity.blockPosition();
        int i = blockPos.getX();
        int j = blockPos.getZ();
        if (i == pos.getX() && j == pos.getZ()) {
            return false;
        } else if (!entity.getNavigation().isStableDestination(pos) && !this.acceptableLandingSpot.test(world.getBlockState(pos.below()))) {
            return false;
        } else {
            return entity.getPathfindingMalus(WalkNodeEvaluator.getBlockPathTypeStatic(entity.level, pos.mutable())) == 0.0F;
        }
    }

    @Nullable
    protected Vec3 calculateOptimalJumpVector(Mob entity, Vec3 pos) {
        List<Integer> list = Lists.newArrayList(ALLOWED_ANGLES);
        Collections.shuffle(list);

        for(int i : list) {
            Vec3 vec3 = this.calculateJumpVectorForAngle(entity, pos, i);
            if (vec3 != null) {
                return vec3;
            }
        }

        return null;
    }

    @Nullable
    private Vec3 calculateJumpVectorForAngle(Mob entity, Vec3 pos, int range) {
        Vec3 vec3 = entity.position();
        Vec3 vec32 = (new Vec3(pos.x - vec3.x, 0.0D, pos.z - vec3.z)).normalize().scale(0.5D);
        pos = pos.subtract(vec32);
        Vec3 vec33 = pos.subtract(vec3);
        float f = (float)range * (float)Math.PI / 180.0F;
        double d = Math.atan2(vec33.z, vec33.x);
        double e = vec33.subtract(0.0D, vec33.y, 0.0D).lengthSqr();
        double g = Math.sqrt(e);
        double h = vec33.y;
        double i = Math.sin((double)(2.0F * f));
        double j = 0.08D;
        double k = Math.pow(Math.cos((double)f), 2.0D);
        double l = Math.sin((double)f);
        double m = Math.cos((double)f);
        double n = Math.sin(d);
        double o = Math.cos(d);
        double p = e * 0.08D / (g * i - 2.0D * h * k);
        if (p < 0.0D) {
            return null;
        } else {
            double q = Math.sqrt(p);
            if (q > (double)this.maxJumpVelocity) {
                return null;
            } else {
                double r = q * m;
                double s = q * l;
                int t = Mth.ceil(g / r) * 2;
                double u = 0.0D;
                Vec3 vec34 = null;

                for(int v = 0; v < t - 1; ++v) {
                    u += g / (double)t;
                    double w = l / m * u - Math.pow(u, 2.0D) * 0.08D / (2.0D * p * Math.pow(m, 2.0D));
                    double x = u * o;
                    double y = u * n;
                    Vec3 vec35 = new Vec3(vec3.x + x, vec3.y + w, vec3.z + y);
                    if (vec34 != null && !this.isClearTransition(entity, vec34, vec35)) {
                        return null;
                    }

                    vec34 = vec35;
                }

                return (new Vec3(r * o, s, r * n)).scale((double)0.95F);
            }
        }
    }

    private boolean isClearTransition(Mob entity, Vec3 startPos, Vec3 endPos) {
        EntityDimensions entityDimensions = entity.getDimensions(Pose.LONG_JUMPING);
        Vec3 vec3 = endPos.subtract(startPos);
        double d = (double)Math.min(entityDimensions.width, entityDimensions.height);
        int i = Mth.ceil(vec3.length() / d);
        Vec3 vec32 = vec3.normalize();
        Vec3 vec33 = startPos;

        for(int j = 0; j < i; ++j) {
            vec33 = j == i - 1 ? endPos : vec33.add(vec32.scale(d * (double)0.9F));
            AABB aABB = entityDimensions.makeBoundingBox(vec33);
            if (!entity.level.noCollision(entity, aABB)) {
                return false;
            }
        }

        return true;
    }

    public static class PossibleJump extends WeightedEntry.IntrusiveBase {
        private final BlockPos jumpTarget;

        public PossibleJump(BlockPos pos, int weight) {
            super(weight);
            this.jumpTarget = pos;
        }

        public BlockPos getJumpTarget() {
            return this.jumpTarget;
        }
    }
}
