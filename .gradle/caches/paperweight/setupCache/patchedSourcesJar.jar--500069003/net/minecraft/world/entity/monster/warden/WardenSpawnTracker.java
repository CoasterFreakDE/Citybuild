package net.minecraft.world.entity.monster.warden;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WardenSpawnTracker {
    public static final Codec<WardenSpawnTracker> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks_since_last_warning").orElse(0).forGetter((manager) -> {
            return manager.ticksSinceLastWarning;
        }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("warning_level").orElse(0).forGetter((manager) -> {
            return manager.warningLevel;
        }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooldown_ticks").orElse(0).forGetter((manager) -> {
            return manager.cooldownTicks;
        })).apply(instance, WardenSpawnTracker::new);
    });
    public static final int MAX_WARNING_LEVEL = 4;
    private static final double PLAYER_SEARCH_RADIUS = 16.0D;
    private static final int WARNING_CHECK_DIAMETER = 48;
    private static final int DECREASE_WARNING_LEVEL_EVERY_INTERVAL = 12000;
    private static final int WARNING_LEVEL_INCREASE_COOLDOWN = 200;
    private int ticksSinceLastWarning;
    private int warningLevel;
    private int cooldownTicks;

    public WardenSpawnTracker(int ticksSinceLastWarning, int warningLevel, int cooldownTicks) {
        this.ticksSinceLastWarning = ticksSinceLastWarning;
        this.warningLevel = warningLevel;
        this.cooldownTicks = cooldownTicks;
    }

    public void tick() {
        if (this.ticksSinceLastWarning >= 12000) {
            this.decreaseWarningLevel();
            this.ticksSinceLastWarning = 0;
        } else {
            ++this.ticksSinceLastWarning;
        }

        if (this.cooldownTicks > 0) {
            --this.cooldownTicks;
        }

    }

    public void reset() {
        this.ticksSinceLastWarning = 0;
        this.warningLevel = 0;
        this.cooldownTicks = 0;
    }

    public static OptionalInt tryWarn(ServerLevel serverLevel, BlockPos blockPos, ServerPlayer serverPlayer) {
        if (hasNearbyWarden(serverLevel, blockPos)) {
            return OptionalInt.empty();
        } else {
            List<ServerPlayer> list = getNearbyPlayers(serverLevel, blockPos);
            if (!list.contains(serverPlayer)) {
                list.add(serverPlayer);
            }

            if (list.stream().anyMatch((serverPlayerx) -> {
                return serverPlayerx.getWardenSpawnTracker().onCooldown();
            })) {
                return OptionalInt.empty();
            } else {
                Optional<WardenSpawnTracker> optional = list.stream().map(Player::getWardenSpawnTracker).max(Comparator.comparingInt((manager) -> {
                    return manager.warningLevel;
                }));
                WardenSpawnTracker wardenSpawnTracker = optional.get();
                wardenSpawnTracker.increaseWarningLevel();
                list.forEach((serverPlayerx) -> {
                    serverPlayerx.getWardenSpawnTracker().copyData(wardenSpawnTracker);
                });
                return OptionalInt.of(wardenSpawnTracker.warningLevel);
            }
        }
    }

    private boolean onCooldown() {
        return this.cooldownTicks > 0;
    }

    private static boolean hasNearbyWarden(ServerLevel serverLevel, BlockPos blockPos) {
        AABB aABB = AABB.ofSize(Vec3.atCenterOf(blockPos), 48.0D, 48.0D, 48.0D);
        return !serverLevel.getEntitiesOfClass(Warden.class, aABB).isEmpty();
    }

    private static List<ServerPlayer> getNearbyPlayers(ServerLevel world, BlockPos pos) {
        Vec3 vec3 = Vec3.atCenterOf(pos);
        Predicate<ServerPlayer> predicate = (player) -> {
            return player.position().closerThan(vec3, 16.0D);
        };
        return world.getPlayers(predicate.and(LivingEntity::isAlive).and(EntitySelector.NO_SPECTATORS));
    }

    private void increaseWarningLevel() {
        if (!this.onCooldown()) {
            this.ticksSinceLastWarning = 0;
            this.cooldownTicks = 200;
            this.setWarningLevel(this.getWarningLevel() + 1);
        }

    }

    private void decreaseWarningLevel() {
        this.setWarningLevel(this.getWarningLevel() - 1);
    }

    public void setWarningLevel(int warningLevel) {
        this.warningLevel = Mth.clamp(warningLevel, 0, 4);
    }

    public int getWarningLevel() {
        return this.warningLevel;
    }

    private void copyData(WardenSpawnTracker other) {
        this.warningLevel = other.warningLevel;
        this.cooldownTicks = other.cooldownTicks;
        this.ticksSinceLastWarning = other.ticksSinceLastWarning;
    }
}
