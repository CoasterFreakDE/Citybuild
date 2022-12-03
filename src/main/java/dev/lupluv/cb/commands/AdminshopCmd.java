package dev.lupluv.cb.commands;

import dev.lupluv.cb.shop.Adminshop;
import dev.lupluv.cb.utils.Strings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AdminshopCmd implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if(!player.hasPermission("cb.adminshop")){
            player.sendMessage(Strings.noPerms);
            return true;
        }

        if(args.length != 0){
            player.sendMessage(Strings.prefix + "§7Benutzung: §a/adminshop");
            return true;
        }

        new Adminshop(player).open();

        return false;
    }
}
