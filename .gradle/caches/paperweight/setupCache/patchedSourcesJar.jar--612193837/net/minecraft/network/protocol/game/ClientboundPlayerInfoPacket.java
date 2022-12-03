package net.minecraft.network.protocol.game;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.GameType;

public class ClientboundPlayerInfoPacket implements Packet<ClientGamePacketListener> {
    private final ClientboundPlayerInfoPacket.Action action;
    private final List<ClientboundPlayerInfoPacket.PlayerUpdate> entries;

    public ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action action, ServerPlayer... players) {
        this.action = action;
        this.entries = Lists.newArrayListWithCapacity(players.length);

        for(ServerPlayer serverPlayer : players) {
            this.entries.add(createPlayerUpdate(serverPlayer));
        }

    }

    public ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action action, Collection<ServerPlayer> players) {
        this.action = action;
        this.entries = Lists.newArrayListWithCapacity(players.size());

        for(ServerPlayer serverPlayer : players) {
            this.entries.add(createPlayerUpdate(serverPlayer));
        }

    }

    public ClientboundPlayerInfoPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(ClientboundPlayerInfoPacket.Action.class);
        this.entries = buf.readList(this.action::read);
    }

    private static ClientboundPlayerInfoPacket.PlayerUpdate createPlayerUpdate(ServerPlayer player) {
        ProfilePublicKey profilePublicKey = player.getProfilePublicKey();
        ProfilePublicKey.Data data = profilePublicKey != null ? profilePublicKey.data() : null;
        return new ClientboundPlayerInfoPacket.PlayerUpdate(player.getGameProfile(), player.latency, player.gameMode.getGameModeForPlayer(), player.getTabListDisplayName(), data);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        buf.writeCollection(this.entries, this.action::write);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handlePlayerInfo(this);
    }

    public List<ClientboundPlayerInfoPacket.PlayerUpdate> getEntries() {
        return this.entries;
    }

    public ClientboundPlayerInfoPacket.Action getAction() {
        return this.action;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("action", this.action).add("entries", this.entries).toString();
    }

    public static enum Action {
        ADD_PLAYER {
            @Override
            protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf) {
                GameProfile gameProfile = buf.readGameProfile();
                GameType gameType = GameType.byId(buf.readVarInt());
                int i = buf.readVarInt();
                Component component = buf.readNullable(FriendlyByteBuf::readComponent);
                ProfilePublicKey.Data data = buf.readNullable(ProfilePublicKey.Data::new);
                return new ClientboundPlayerInfoPacket.PlayerUpdate(gameProfile, i, gameType, component, data);
            }

            @Override
            protected void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry) {
                buf.writeGameProfile(entry.getProfile());
                buf.writeVarInt(entry.getGameMode().getId());
                buf.writeVarInt(entry.getLatency());
                buf.writeNullable(entry.getDisplayName(), FriendlyByteBuf::writeComponent);
                buf.writeNullable(entry.getProfilePublicKey(), (buf2, publicKeyData) -> {
                    publicKeyData.write(buf2);
                });
            }
        },
        UPDATE_GAME_MODE {
            @Override
            protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                GameType gameType = GameType.byId(buf.readVarInt());
                return new ClientboundPlayerInfoPacket.PlayerUpdate(gameProfile, 0, gameType, (Component)null, (ProfilePublicKey.Data)null);
            }

            @Override
            protected void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry) {
                buf.writeUUID(entry.getProfile().getId());
                buf.writeVarInt(entry.getGameMode().getId());
            }
        },
        UPDATE_LATENCY {
            @Override
            protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                int i = buf.readVarInt();
                return new ClientboundPlayerInfoPacket.PlayerUpdate(gameProfile, i, (GameType)null, (Component)null, (ProfilePublicKey.Data)null);
            }

            @Override
            protected void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry) {
                buf.writeUUID(entry.getProfile().getId());
                buf.writeVarInt(entry.getLatency());
            }
        },
        UPDATE_DISPLAY_NAME {
            @Override
            protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                Component component = buf.readNullable(FriendlyByteBuf::readComponent);
                return new ClientboundPlayerInfoPacket.PlayerUpdate(gameProfile, 0, (GameType)null, component, (ProfilePublicKey.Data)null);
            }

            @Override
            protected void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry) {
                buf.writeUUID(entry.getProfile().getId());
                buf.writeNullable(entry.getDisplayName(), FriendlyByteBuf::writeComponent);
            }
        },
        REMOVE_PLAYER {
            @Override
            protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                return new ClientboundPlayerInfoPacket.PlayerUpdate(gameProfile, 0, (GameType)null, (Component)null, (ProfilePublicKey.Data)null);
            }

            @Override
            protected void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry) {
                buf.writeUUID(entry.getProfile().getId());
            }
        };

        protected abstract ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf);

        protected abstract void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry);
    }

    public static class PlayerUpdate {
        private final int latency;
        private final GameType gameMode;
        private final GameProfile profile;
        @Nullable
        private final Component displayName;
        @Nullable
        private final ProfilePublicKey.Data profilePublicKey;

        public PlayerUpdate(GameProfile profile, int latency, @Nullable GameType gameMode, @Nullable Component displayName, @Nullable ProfilePublicKey.Data publicKeyData) {
            this.profile = profile;
            this.latency = latency;
            this.gameMode = gameMode;
            this.displayName = displayName;
            this.profilePublicKey = publicKeyData;
        }

        public GameProfile getProfile() {
            return this.profile;
        }

        public int getLatency() {
            return this.latency;
        }

        public GameType getGameMode() {
            return this.gameMode;
        }

        @Nullable
        public Component getDisplayName() {
            return this.displayName;
        }

        @Nullable
        public ProfilePublicKey.Data getProfilePublicKey() {
            return this.profilePublicKey;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("latency", this.latency).add("gameMode", this.gameMode).add("profile", this.profile).add("displayName", this.displayName == null ? null : Component.Serializer.toJson(this.displayName)).add("profilePublicKey", this.profilePublicKey).toString();
        }
    }
}
