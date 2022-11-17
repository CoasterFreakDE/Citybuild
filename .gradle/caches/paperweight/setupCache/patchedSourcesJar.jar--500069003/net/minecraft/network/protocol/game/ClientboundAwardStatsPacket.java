package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;

public class ClientboundAwardStatsPacket implements Packet<ClientGamePacketListener> {
    private final Object2IntMap<Stat<?>> stats;

    public ClientboundAwardStatsPacket(Object2IntMap<Stat<?>> stats) {
        this.stats = stats;
    }

    public ClientboundAwardStatsPacket(FriendlyByteBuf buf) {
        this.stats = buf.readMap(Object2IntOpenHashMap::new, (bufx) -> {
            StatType<?> statType = bufx.readById(Registry.STAT_TYPE);
            return readStatCap(buf, statType);
        }, FriendlyByteBuf::readVarInt);
    }

    private static <T> Stat<T> readStatCap(FriendlyByteBuf buf, StatType<T> statType) {
        return statType.get(buf.readById(statType.getRegistry()));
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleAwardStats(this);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeMap(this.stats, ClientboundAwardStatsPacket::writeStatCap, FriendlyByteBuf::writeVarInt);
    }

    private static <T> void writeStatCap(FriendlyByteBuf buf, Stat<T> stat) {
        buf.writeId(Registry.STAT_TYPE, stat.getType());
        buf.writeId(stat.getType().getRegistry(), stat.getValue());
    }

    public Map<Stat<?>, Integer> getStats() {
        return this.stats;
    }
}
