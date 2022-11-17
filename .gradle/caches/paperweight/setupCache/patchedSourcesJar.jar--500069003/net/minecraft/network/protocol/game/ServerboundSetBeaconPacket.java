package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffect;

public class ServerboundSetBeaconPacket implements Packet<ServerGamePacketListener> {
    private final Optional<MobEffect> primary;
    private final Optional<MobEffect> secondary;

    public ServerboundSetBeaconPacket(Optional<MobEffect> optional, Optional<MobEffect> optional2) {
        this.primary = optional;
        this.secondary = optional2;
    }

    public ServerboundSetBeaconPacket(FriendlyByteBuf buf) {
        this.primary = buf.readOptional((friendlyByteBuf) -> {
            return friendlyByteBuf.readById(Registry.MOB_EFFECT);
        });
        this.secondary = buf.readOptional((friendlyByteBuf) -> {
            return friendlyByteBuf.readById(Registry.MOB_EFFECT);
        });
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(this.primary, (friendlyByteBuf, mobEffect) -> {
            friendlyByteBuf.writeId(Registry.MOB_EFFECT, mobEffect);
        });
        buf.writeOptional(this.secondary, (friendlyByteBuf, mobEffect) -> {
            friendlyByteBuf.writeId(Registry.MOB_EFFECT, mobEffect);
        });
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSetBeaconPacket(this);
    }

    public Optional<MobEffect> getPrimary() {
        return this.primary;
    }

    public Optional<MobEffect> getSecondary() {
        return this.secondary;
    }
}
