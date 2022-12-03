package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class ClientboundUpdateMobEffectPacket implements Packet<ClientGamePacketListener> {
    private static final short LONG_DURATION_THRESHOLD = Short.MAX_VALUE;
    private static final int FLAG_AMBIENT = 1;
    private static final int FLAG_VISIBLE = 2;
    private static final int FLAG_SHOW_ICON = 4;
    private final int entityId;
    private final MobEffect effect;
    private final byte effectAmplifier;
    private final int effectDurationTicks;
    private final byte flags;
    @Nullable
    private final MobEffectInstance.FactorData factorData;

    public ClientboundUpdateMobEffectPacket(int entityId, MobEffectInstance effect) {
        this.entityId = entityId;
        this.effect = effect.getEffect();
        this.effectAmplifier = (byte)(effect.getAmplifier() & 255);
        this.effectDurationTicks = effect.getDuration();
        byte b = 0;
        if (effect.isAmbient()) {
            b = (byte)(b | 1);
        }

        if (effect.isVisible()) {
            b = (byte)(b | 2);
        }

        if (effect.showIcon()) {
            b = (byte)(b | 4);
        }

        this.flags = b;
        this.factorData = effect.getFactorData().orElse((MobEffectInstance.FactorData)null);
    }

    public ClientboundUpdateMobEffectPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.effect = buf.readById(Registry.MOB_EFFECT);
        this.effectAmplifier = buf.readByte();
        this.effectDurationTicks = buf.readVarInt();
        this.flags = buf.readByte();
        this.factorData = buf.readNullable((buf2) -> {
            return buf2.readWithCodec(MobEffectInstance.FactorData.CODEC);
        });
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeId(Registry.MOB_EFFECT, this.effect);
        buf.writeByte(this.effectAmplifier);
        buf.writeVarInt(this.effectDurationTicks);
        buf.writeByte(this.flags);
        buf.writeNullable(this.factorData, (buf2, factorCalculationData) -> {
            buf2.writeWithCodec(MobEffectInstance.FactorData.CODEC, factorCalculationData);
        });
    }

    public boolean isSuperLongDuration() {
        return this.effectDurationTicks >= 32767;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleUpdateMobEffect(this);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public MobEffect getEffect() {
        return this.effect;
    }

    public byte getEffectAmplifier() {
        return this.effectAmplifier;
    }

    public int getEffectDurationTicks() {
        return this.effectDurationTicks;
    }

    public boolean isEffectVisible() {
        return (this.flags & 2) == 2;
    }

    public boolean isEffectAmbient() {
        return (this.flags & 1) == 1;
    }

    public boolean effectShowsIcon() {
        return (this.flags & 4) == 4;
    }

    @Nullable
    public MobEffectInstance.FactorData getFactorData() {
        return this.factorData;
    }
}
