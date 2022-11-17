package dev.lupluv.cb.commands;

import dev.lupluv.cb.economy.Economy;
import dev.lupluv.cb.utils.Strings;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PayallCmd implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(sender instanceof Player){
            Player p = (Player) sender;
            if(!p.hasPermission("cb.pay.all")){
                p.sendMessage(Strings.noPerms);
            }else{
                if(args.length == 1){
                    try {
                        double amount = Double.parseDouble(args[0]);
                        double balance = Economy.getBalance(p.getUniqueId());
                        if(balance >= Bukkit.getOnlinePlayers().size()*amount){
                            Bukkit.getOnlinePlayers().forEach(all ->{
                                p.performCommand("pay " + all.getName() + " " + (long) amount);
                            });
                        }else{
                            p.sendMessage(Strings.prefix + "§cDu hast nicht genug Geld dafür.");
                        }
                    }catch (NumberFormatException e){
                        p.sendMessage(Strings.prefix + "§cBitte gebe einen Wert an.");
                    }
                }else{
                    p.sendMessage(Strings.prefix + "§7Benutzung: /payall §a<Spieler>");
                }
            }
        }

        return false;
    }
}
