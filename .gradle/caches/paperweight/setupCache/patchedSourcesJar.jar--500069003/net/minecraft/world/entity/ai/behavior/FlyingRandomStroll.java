package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.phys.Vec3;

public class FlyingRandomStroll extends RandomStroll {
    public FlyingRandomStroll(float speed) {
        this(speed, true);
    }

    public FlyingRandomStroll(float speed, boolean strollInsideWater) {
        super(speed, strollInsideWater);
    }

    @Override
    protected Vec3 getTargetPos(PathfinderMob entity) {
        Vec3 vec3 = entity.getViewVector(0.0F);
        return AirAndWaterRandomPos.getPos(entity, this.maxHorizontalDistance, this.maxVerticalDistance, -2, vec3.x, vec3.z, (double)((float)Math.PI / 2F));
    }
}
