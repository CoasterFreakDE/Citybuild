package net.minecraft.network.protocol.game;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ClientboundRespawnPacket implements Packet<ClientGamePacketListener> {
    private final ResourceKey<DimensionType> dimensionType;
    private final ResourceKey<Level> dimension;
    private final long seed;
    private final GameType playerGameType;
    @Nullable
    private final GameType previousPlayerGameType;
    private final boolean isDebug;
    private final boolean isFlat;
    private final boolean keepAllPlayerData;
    private final Optional<GlobalPos> lastDeathLocation;

    public ClientboundRespawnPacket(ResourceKey<DimensionType> dimensionType, ResourceKey<Level> dimension, long sha256Seed, GameType gameMode, @Nullable GameType previousGameMode, boolean debugWorld, boolean flatWorld, boolean keepPlayerAttributes, Optional<GlobalPos> lastDeathPos) {
        this.dimensionType = dimensionType;
        this.dimension = dimension;
        this.seed = sha256Seed;
        this.playerGameType = gameMode;
        this.previousPlayerGameType = previousGameMode;
        this.isDebug = debugWorld;
        this.isFlat = flatWorld;
        this.keepAllPlayerData = keepPlayerAttributes;
        this.lastDeathLocation = lastDeathPos;
    }

    public ClientboundRespawnPacket(FriendlyByteBuf buf) {
        this.dimensionType = buf.readResourceKey(Registry.DIMENSION_TYPE_REGISTRY);
        this.dimension = buf.readResourceKey(Registry.DIMENSION_REGISTRY);
        this.seed = buf.readLong();
        this.playerGameType = GameType.byId(buf.readUnsignedByte());
        this.previousPlayerGameType = GameType.byNullableId(buf.readByte());
        this.isDebug = buf.readBoolean();
        this.isFlat = buf.readBoolean();
        this.keepAllPlayerData = buf.readBoolean();
        this.lastDeathLocation = buf.readOptional(FriendlyByteBuf::readGlobalPos);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceKey(this.dimensionType);
        buf.writeResourceKey(this.dimension);
        buf.writeLong(this.seed);
        buf.writeByte(this.playerGameType.getId());
        buf.writeByte(GameType.getNullableId(this.previousPlayerGameType));
        buf.writeBoolean(this.isDebug);
        buf.writeBoolean(this.isFlat);
        buf.writeBoolean(this.keepAllPlayerData);
        buf.writeOptional(this.lastDeathLocation, FriendlyByteBuf::writeGlobalPos);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleRespawn(this);
    }

    public ResourceKey<DimensionType> getDimensionType() {
        return this.dimensionType;
    }

    public ResourceKey<Level> getDimension() {
        return this.dimension;
    }

    public long getSeed() {
        return this.seed;
    }

    public GameType getPlayerGameType() {
        return this.playerGameType;
    }

    @Nullable
    public GameType getPreviousPlayerGameType() {
        return this.previousPlayerGameType;
    }

    public boolean isDebug() {
        return this.isDebug;
    }

    public boolean isFlat() {
        return this.isFlat;
    }

    public boolean shouldKeepAllPlayerData() {
        return this.keepAllPlayerData;
    }

    public Optional<GlobalPos> getLastDeathLocation() {
        return this.lastDeathLocation;
    }
}
