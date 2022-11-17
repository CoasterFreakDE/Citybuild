package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingPlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;

public class MsgCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(Commands.literal("msg").then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("message", MessageArgument.message()).executes((context) -> {
            MessageArgument.ChatMessage chatMessage = MessageArgument.getChatMessage(context, "message");

            try {
                return sendMessage(context.getSource(), EntityArgument.getPlayers(context, "targets"), chatMessage);
            } catch (Exception var3) {
                chatMessage.consume(context.getSource());
                throw var3;
            }
        }))));
        dispatcher.register(Commands.literal("tell").redirect(literalCommandNode));
        dispatcher.register(Commands.literal("w").redirect(literalCommandNode));
    }

    private static int sendMessage(CommandSourceStack source, Collection<ServerPlayer> targets, MessageArgument.ChatMessage signedMessage) {
        ChatType.Bound bound = ChatType.bind(ChatType.MSG_COMMAND_INCOMING, source);
        signedMessage.resolve(source, (message) -> {
            OutgoingPlayerChatMessage outgoingPlayerChatMessage = OutgoingPlayerChatMessage.create(message);
            boolean bl = message.isFullyFiltered();
            Entity entity = source.getEntity();
            boolean bl2 = false;

            for(ServerPlayer serverPlayer : targets) {
                ChatType.Bound bound2 = ChatType.bind(ChatType.MSG_COMMAND_OUTGOING, source).withTargetName(serverPlayer.getDisplayName());
                source.sendChatMessage(outgoingPlayerChatMessage, false, bound2);
                boolean bl3 = source.shouldFilterMessageTo(serverPlayer);
                serverPlayer.sendChatMessage(outgoingPlayerChatMessage, bl3, bound);
                bl2 |= bl && bl3 && serverPlayer != entity;
            }

            if (bl2) {
                source.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
            }

            outgoingPlayerChatMessage.sendHeadersToRemainingPlayers(source.getServer().getPlayerList());
        });
        return targets.size();
    }
}
