package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.players.PlayerList;

public class EmoteCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("me").then(Commands.argument("action", MessageArgument.message()).executes((context) -> {
            MessageArgument.ChatMessage chatMessage = MessageArgument.getChatMessage(context, "action");
            CommandSourceStack commandSourceStack = context.getSource();
            PlayerList playerList = commandSourceStack.getServer().getPlayerList();
            chatMessage.resolve(commandSourceStack, (message) -> {
                playerList.broadcastChatMessage(message, commandSourceStack, ChatType.bind(ChatType.EMOTE_COMMAND, commandSourceStack));
            });
            return 1;
        })));
    }
}
