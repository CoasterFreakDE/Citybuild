package dev.lupluv.cb.commands;

import dev.lupluv.cb.moderation.Startkick;
import dev.lupluv.cb.utils.Strings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JaCmd implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(sender instanceof Player){
            Player p = (Player) sender;
            if(args.length == 1){
                String name = args[0];
                Startkick startkick = Startkick.getStartKick(name);
                if(startkick != null){
                    if(!startkick.hasVoted(p)){
                        startkick.addYes(p);
                        p.sendMessage(Strings.prefix + "§7Du hast §aerfolgreich §7deine Stimme abgegeben.");
                    }else{
                        p.sendMessage(Strings.prefix + "§cDu hast bereits deine Stimme abgegeben.");
                    }
                }else{
                    p.sendMessage(Strings.prefix + "§cDieser Startkick existiert nicht.");
                }
            }else{
                p.sendMessage(Strings.prefix + "§7Benutzung: /ja §a<Spieler>");
            }
        }

        return false;
    }
}
