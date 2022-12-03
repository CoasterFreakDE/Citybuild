package net.minecraft.network.protocol.game;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.Validate;

public class ClientboundSoundEntityPacket implements Packet<ClientGamePacketListener> {
    private final SoundEvent sound;
    private final SoundSource source;
    private final int id;
    private final float volume;
    private final float pitch;
    private final long seed;

    public ClientboundSoundEntityPacket(SoundEvent sound, SoundSource category, Entity entity, float volume, float pitch, long seed) {
        Validate.notNull(sound, "sound");
        this.sound = sound;
        this.source = category;
        this.id = entity.getId();
        this.volume = volume;
        this.pitch = pitch;
        this.seed = seed;
    }

    public ClientboundSoundEntityPacket(FriendlyByteBuf buf) {
        this.sound = buf.readById(Registry.SOUND_EVENT);
        this.source = buf.readEnum(SoundSource.class);
        this.id = buf.readVarInt();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.seed = buf.readLong();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeId(Registry.SOUND_EVENT, this.sound);
        buf.writeEnum(this.source);
        buf.writeVarInt(this.id);
        buf.writeFloat(this.volume);
        buf.writeFloat(this.pitch);
        buf.writeLong(this.seed);
    }

    public SoundEvent getSound() {
        return this.sound;
    }

    public SoundSource getSource() {
        return this.source;
    }

    public int getId() {
        return this.id;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    public long getSeed() {
        return this.seed;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSoundEntityEvent(this);
    }
}
