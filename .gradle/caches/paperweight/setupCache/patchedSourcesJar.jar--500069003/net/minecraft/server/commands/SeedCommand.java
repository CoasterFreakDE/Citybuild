package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;

public class SeedCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
        dispatcher.register(Commands.literal("seed").requires((source) -> {
            return !dedicated || source.hasPermission(2);
        }).executes((context) -> {
            long l = context.getSource().getLevel().getSeed();
            Component component = ComponentUtils.wrapInSquareBrackets(Component.literal(String.valueOf(l)).withStyle((style) -> {
                return style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(l))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click"))).withInsertion(String.valueOf(l));
            }));
            context.getSource().sendSuccess(Component.translatable("commands.seed.success", component), false);
            return (int)l;
        }));
    }
}
