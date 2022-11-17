package net.minecraft.sounds;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

public class SoundEvent {
    public static final Codec<SoundEvent> CODEC = ResourceLocation.CODEC.xmap(SoundEvent::new, (soundEvent) -> {
        return soundEvent.location;
    });
    private final ResourceLocation location;
    private final float range;
    private final boolean newSystem;

    public SoundEvent(ResourceLocation id) {
        this(id, 16.0F, false);
    }

    public SoundEvent(ResourceLocation id, float distanceToTravel) {
        this(id, distanceToTravel, true);
    }

    private SoundEvent(ResourceLocation id, float distanceToTravel, boolean useStaticDistance) {
        this.location = id;
        this.range = distanceToTravel;
        this.newSystem = useStaticDistance;
    }

    public ResourceLocation getLocation() {
        return this.location;
    }

    public float getRange(float volume) {
        if (this.newSystem) {
            return this.range;
        } else {
            return volume > 1.0F ? 16.0F * volume : 16.0F;
        }
    }
}
