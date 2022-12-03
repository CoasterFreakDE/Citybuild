package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;

public abstract class NodeEvaluator {
    protected PathNavigationRegion level;
    protected Mob mob;
    protected final Int2ObjectMap<Node> nodes = new Int2ObjectOpenHashMap<>();
    protected int entityWidth;
    protected int entityHeight;
    protected int entityDepth;
    protected boolean canPassDoors;
    protected boolean canOpenDoors;
    protected boolean canFloat;

    public void prepare(PathNavigationRegion cachedWorld, Mob entity) {
        this.level = cachedWorld;
        this.mob = entity;
        this.nodes.clear();
        this.entityWidth = Mth.floor(entity.getBbWidth() + 1.0F);
        this.entityHeight = Mth.floor(entity.getBbHeight() + 1.0F);
        this.entityDepth = Mth.floor(entity.getBbWidth() + 1.0F);
    }

    public void done() {
        this.level = null;
        this.mob = null;
    }

    @Nullable
    protected Node getNode(BlockPos pos) {
        return this.getNode(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable
    protected Node getNode(int x, int y, int z) {
        return this.nodes.computeIfAbsent(Node.createHash(x, y, z), (l) -> {
            return new Node(x, y, z);
        });
    }

    @Nullable
    public abstract Node getStart();

    @Nullable
    public abstract Target getGoal(double x, double y, double z);

    @Nullable
    protected Target getTargetFromNode(@Nullable Node node) {
        return node != null ? new Target(node) : null;
    }

    public abstract int getNeighbors(Node[] successors, Node node);

    public abstract BlockPathTypes getBlockPathType(BlockGetter world, int x, int y, int z, Mob mob, int sizeX, int sizeY, int sizeZ, boolean canOpenDoors, boolean canEnterOpenDoors);

    public abstract BlockPathTypes getBlockPathType(BlockGetter world, int x, int y, int z);

    public void setCanPassDoors(boolean canEnterOpenDoors) {
        this.canPassDoors = canEnterOpenDoors;
    }

    public void setCanOpenDoors(boolean canOpenDoors) {
        this.canOpenDoors = canOpenDoors;
    }

    public void setCanFloat(boolean canSwim) {
        this.canFloat = canSwim;
    }

    public boolean canPassDoors() {
        return this.canPassDoors;
    }

    public boolean canOpenDoors() {
        return this.canOpenDoors;
    }

    public boolean canFloat() {
        return this.canFloat;
    }
}
