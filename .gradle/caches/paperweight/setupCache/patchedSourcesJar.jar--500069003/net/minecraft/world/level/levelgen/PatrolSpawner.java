package net.minecraft.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class PatrolSpawner implements CustomSpawner {
    private int nextTick;

    @Override
    public int tick(ServerLevel world, boolean spawnMonsters, boolean spawnAnimals) {
        if (!spawnMonsters) {
            return 0;
        } else if (!world.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
            return 0;
        } else {
            RandomSource randomSource = world.random;
            --this.nextTick;
            if (this.nextTick > 0) {
                return 0;
            } else {
                this.nextTick += 12000 + randomSource.nextInt(1200);
                long l = world.getDayTime() / 24000L;
                if (l >= 5L && world.isDay()) {
                    if (randomSource.nextInt(5) != 0) {
                        return 0;
                    } else {
                        int i = world.players().size();
                        if (i < 1) {
                            return 0;
                        } else {
                            Player player = world.players().get(randomSource.nextInt(i));
                            if (player.isSpectator()) {
                                return 0;
                            } else if (world.isCloseToVillage(player.blockPosition(), 2)) {
                                return 0;
                            } else {
                                int j = (24 + randomSource.nextInt(24)) * (randomSource.nextBoolean() ? -1 : 1);
                                int k = (24 + randomSource.nextInt(24)) * (randomSource.nextBoolean() ? -1 : 1);
                                BlockPos.MutableBlockPos mutableBlockPos = player.blockPosition().mutable().move(j, 0, k);
                                int m = 10;
                                if (!world.hasChunksAt(mutableBlockPos.getX() - 10, mutableBlockPos.getZ() - 10, mutableBlockPos.getX() + 10, mutableBlockPos.getZ() + 10)) {
                                    return 0;
                                } else {
                                    Holder<Biome> holder = world.getBiome(mutableBlockPos);
                                    if (holder.is(BiomeTags.WITHOUT_PATROL_SPAWNS)) {
                                        return 0;
                                    } else {
                                        int n = 0;
                                        int o = (int)Math.ceil((double)world.getCurrentDifficultyAt(mutableBlockPos).getEffectiveDifficulty()) + 1;

                                        for(int p = 0; p < o; ++p) {
                                            ++n;
                                            mutableBlockPos.setY(world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mutableBlockPos).getY());
                                            if (p == 0) {
                                                if (!this.spawnPatrolMember(world, mutableBlockPos, randomSource, true)) {
                                                    break;
                                                }
                                            } else {
                                                this.spawnPatrolMember(world, mutableBlockPos, randomSource, false);
                                            }

                                            mutableBlockPos.setX(mutableBlockPos.getX() + randomSource.nextInt(5) - randomSource.nextInt(5));
                                            mutableBlockPos.setZ(mutableBlockPos.getZ() + randomSource.nextInt(5) - randomSource.nextInt(5));
                                        }

                                        return n;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    return 0;
                }
            }
        }
    }

    private boolean spawnPatrolMember(ServerLevel world, BlockPos pos, RandomSource random, boolean captain) {
        BlockState blockState = world.getBlockState(pos);
        if (!NaturalSpawner.isValidEmptySpawnBlock(world, pos, blockState, blockState.getFluidState(), EntityType.PILLAGER)) {
            return false;
        } else if (!PatrollingMonster.checkPatrollingMonsterSpawnRules(EntityType.PILLAGER, world, MobSpawnType.PATROL, pos, random)) {
            return false;
        } else {
            PatrollingMonster patrollingMonster = EntityType.PILLAGER.create(world);
            if (patrollingMonster != null) {
                if (captain) {
                    patrollingMonster.setPatrolLeader(true);
                    patrollingMonster.findPatrolTarget();
                }

                patrollingMonster.setPos((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
                patrollingMonster.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, (SpawnGroupData)null, (CompoundTag)null);
                world.addFreshEntityWithPassengers(patrollingMonster);
                return true;
            } else {
                return false;
            }
        }
    }
}
