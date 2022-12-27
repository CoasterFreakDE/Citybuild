package dev.lupluv.cb.commands;

import dev.lupluv.cb.particels.Particel;
import dev.lupluv.cb.utils.Strings;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Effects implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player player)) return true;
        if(!(player.hasPermission("cb.effekte"))) return true;
        if(args.length == 0){

            if(Particel.haseffect1.contains(player)){
                Particel.haseffect1.remove(player);
                player.sendMessage(Strings.prefix + "§cDu hast deine Effekte deaktiviert!");
            }else{
                Particel.haseffect1.add(player);
                player.sendMessage(Strings.prefix + "§aDu hast deine Effekte aktiviert");
            }

        }else{
            player.sendMessage(Strings.prefix + "§aBenutzung /effekt");
        }

        return false;
    }
}
