package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingPlayerChatMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;

public class TeamMsgCommand {
    private static final Style SUGGEST_STYLE = Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.type.team.hover"))).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/teammsg "));
    private static final SimpleCommandExceptionType ERROR_NOT_ON_TEAM = new SimpleCommandExceptionType(Component.translatable("commands.teammsg.failed.noteam"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(Commands.literal("teammsg").then(Commands.argument("message", MessageArgument.message()).executes((context) -> {
            MessageArgument.ChatMessage chatMessage = MessageArgument.getChatMessage(context, "message");

            try {
                return sendMessage(context.getSource(), chatMessage);
            } catch (Exception var3) {
                chatMessage.consume(context.getSource());
                throw var3;
            }
        })));
        dispatcher.register(Commands.literal("tm").redirect(literalCommandNode));
    }

    private static int sendMessage(CommandSourceStack source, MessageArgument.ChatMessage signedMessage) throws CommandSyntaxException {
        Entity entity = source.getEntityOrException();
        PlayerTeam playerTeam = (PlayerTeam)entity.getTeam();
        if (playerTeam == null) {
            throw ERROR_NOT_ON_TEAM.create();
        } else {
            Component component = playerTeam.getFormattedDisplayName().withStyle(SUGGEST_STYLE);
            ChatType.Bound bound = ChatType.bind(ChatType.TEAM_MSG_COMMAND_INCOMING, source).withTargetName(component);
            ChatType.Bound bound2 = ChatType.bind(ChatType.TEAM_MSG_COMMAND_OUTGOING, source).withTargetName(component);
            List<ServerPlayer> list = source.getServer().getPlayerList().getPlayers().stream().filter((player) -> {
                return player == entity || player.getTeam() == playerTeam;
            }).toList();
            signedMessage.resolve(source, (message) -> {
                OutgoingPlayerChatMessage outgoingPlayerChatMessage = OutgoingPlayerChatMessage.create(message);
                boolean bl = message.isFullyFiltered();
                boolean bl2 = false;

                for(ServerPlayer serverPlayer : list) {
                    ChatType.Bound bound3 = serverPlayer == entity ? bound2 : bound;
                    boolean bl3 = source.shouldFilterMessageTo(serverPlayer);
                    serverPlayer.sendChatMessage(outgoingPlayerChatMessage, bl3, bound3);
                    bl2 |= bl && bl3 && serverPlayer != entity;
                }

                if (bl2) {
                    source.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
                }

                outgoingPlayerChatMessage.sendHeadersToRemainingPlayers(source.getServer().getPlayerList());
            });
            return list.size();
        }
    }
}
