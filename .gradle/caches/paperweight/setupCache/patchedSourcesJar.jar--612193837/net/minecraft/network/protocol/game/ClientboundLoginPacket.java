package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public record ClientboundLoginPacket(int playerId, boolean hardcore, GameType gameType, @Nullable GameType previousGameType, Set<ResourceKey<Level>> levels, RegistryAccess.Frozen registryHolder, ResourceKey<DimensionType> dimensionType, ResourceKey<Level> dimension, long seed, int maxPlayers, int chunkRadius, int simulationDistance, boolean reducedDebugInfo, boolean showDeathScreen, boolean isDebug, boolean isFlat, Optional<GlobalPos> lastDeathLocation) implements Packet<ClientGamePacketListener> {
    public ClientboundLoginPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readBoolean(), GameType.byId(buf.readByte()), GameType.byNullableId(buf.readByte()), buf.readCollection(Sets::newHashSetWithExpectedSize, (b) -> {
            return b.readResourceKey(Registry.DIMENSION_REGISTRY);
        }), buf.readWithCodec(RegistryAccess.NETWORK_CODEC).freeze(), buf.readResourceKey(Registry.DIMENSION_TYPE_REGISTRY), buf.readResourceKey(Registry.DIMENSION_REGISTRY), buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readOptional(FriendlyByteBuf::readGlobalPos));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.playerId);
        buf.writeBoolean(this.hardcore);
        buf.writeByte(this.gameType.getId());
        buf.writeByte(GameType.getNullableId(this.previousGameType));
        buf.writeCollection(this.levels, FriendlyByteBuf::writeResourceKey);
        buf.writeWithCodec(RegistryAccess.NETWORK_CODEC, this.registryHolder);
        buf.writeResourceKey(this.dimensionType);
        buf.writeResourceKey(this.dimension);
        buf.writeLong(this.seed);
        buf.writeVarInt(this.maxPlayers);
        buf.writeVarInt(this.chunkRadius);
        buf.writeVarInt(this.simulationDistance);
        buf.writeBoolean(this.reducedDebugInfo);
        buf.writeBoolean(this.showDeathScreen);
        buf.writeBoolean(this.isDebug);
        buf.writeBoolean(this.isFlat);
        buf.writeOptional(this.lastDeathLocation, FriendlyByteBuf::writeGlobalPos);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleLogin(this);
    }
}
