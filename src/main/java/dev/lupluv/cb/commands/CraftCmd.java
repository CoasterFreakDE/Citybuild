package dev.lupluv.cb.commands;

import dev.lupluv.cb.utils.Strings;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;

public class CraftCmd implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player player)) return true;

        if(!player.hasPermission("cb.craft")){
            player.sendMessage(Strings.noPerms);
            return true;
        }

        if(args.length == 0){
            player.openInventory(Bukkit.createInventory(null, InventoryType.CRAFTING));
        }

        return false;
    }
}
