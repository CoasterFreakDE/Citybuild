package dev.lupluv.cb.commands;

import dev.lupluv.cb.utils.Item;
import dev.lupluv.cb.utils.Lore;
import dev.lupluv.cb.utils.Strings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class RanginfoCmd implements CommandExecutor, Listener {

    private Inventory inv;
    public static String invname = "§a§lRanginfo";

    public void createInv(Player player){

        inv = Bukkit.createInventory(null, 9*4, invname);
        ItemStack nethscrape = new ItemStack(Material.NETHERITE_SCRAP);
        ItemMeta nethscrapemeta = nethscrape.getItemMeta();
        nethscrapemeta.setDisplayName("§6§lPremium");
        ArrayList<String> lore1 = new ArrayList<>();
        lore1.add(" ");
        lore1.add("§7§lBefehle:");
        lore1.add("§7• §8/werkbank §7öffne jederzeit die Werkbank");
        lore1.add("§7• §8/invsee §7lässt dich in andere Inventare schauen");
        lore1.add("§7• §8/payall §7erlaubt dir allen Spielern jede Stunde Coins zu zahlen");
        lore1.add(" ");
        lore1.add("§7§lAnderes:");
        lore1.add("§7• Du erhälst den §6§lPremium §8Prefix");
        lore1.add("§7• Du erhälst mehrer Plot Ränder & Wände");
        lore1.add("§7• Du kannst 4 Homes setzten");
        lore1.add("§7• Du kannst farbig schreiben (Alle ColorCode mit &)");
        lore1.add("§7• Du kannst 3 Plots besitzten");
        lore1.add(" ");
        nethscrapemeta.setLore(lore1);
        nethscrape.setItemMeta(nethscrapemeta);

        //9
        ItemStack amtcluster = new ItemStack(Material.AMETHYST_CLUSTER);
        ItemMeta amtclustermeta = amtcluster.getItemMeta();
        amtclustermeta.setDisplayName("§9§lTitan");
        amtclustermeta.setLore(Lore.create(
                " ",
                "§7§lBefehle:",
                "§7• §8/msgtoggle §7Schalte Direktnachrichten aus",
                "§7• §8/amboss §7Öffne jederzeit den Amboss",
                "§7• §8/heal §7Heile dich alle 3 Stunden",
                "§7• §8/repair §7Repariere alle 3 Stunden ein Item",
                "§7• §8/startmute §7Mute böse Spieler",
                "§7• §8/p time §7Setze deine eigene Zeit",
                " ",
                "§7§lAnderes:",
                "§7• Du erhälst den §9§lTitan §7Prefix",
                "§7• Du erhälst mehr Plot Ränder & Wände",
                "§7• Du erhälst den Schnelligkeit & kein Hunger Perk",
                "§7• (Bestimmte Effekte)",
                "§7• (Bestimmte Haustiere)",
                "§7• Du kannst 7 Homes setzen",
                "§7• Du kannst 7 Plots besitzen",
                "§7• §lDu erhälst alle Funktionen vom Premium und Platin Rang",
                " "
        ));
        amtcluster.setItemMeta(amtclustermeta);
        //2
        ItemStack emerlad = new ItemStack(Material.EMERALD);
        ItemMeta emerladmeta = emerlad.getItemMeta();
        emerladmeta.setDisplayName("§2§lPlatin");
        emerladmeta.setLore(Lore.create(
                " ",
                "§7§lBefehle:",
                "§7• §8/msgtoggle §7Schaltet Direktnachrichten aus",
                "§7• §8/amboss §7Öffne jederzeit den Amboss",
                "§7• §8/heal §7Heile dich alle 3 Stunden",
                "§7• §8/repair §7Repariere alle 3 Stunden ein Item",
                "§7• §8/startmute §7Mute böse Spieler",
                "§7• §8/p time §7Setzte deine eigene Zeit",
                " ",
                "§7§lAnderes:",
                "§7• Du erhälst den §2§lTitan §7Prefix",
                "§7• Du erhälst mehr Pot, Ränder & Wände",
                "§7• Du erhälst den Schnelligkeit & kein Hunger Perk",
                "§7• (Bestimmte Effekte)",
                "§7• (Bestimmte Haustiere)",
                "§7• Du kannst 7 Homes setzen",
                "§7• Du kannst 7 Plots besitzen",
                "§7• §lDu erhälst alle Funktionen vom Premium und Platin Rang"

        ));
        emerlad.setItemMeta(emerladmeta);
        //d
        ItemStack diamond = new ItemStack(Material.DIAMOND);
        ItemMeta diamondmeta = diamond.getItemMeta();
        diamondmeta.setDisplayName("§d§lWonder");
        diamondmeta.setLore(Lore.create(
                " ",
                "§7§lBefehle:",
                "§7• §8/startkick §7Kicke einen bösen Spieler vom Server",
                "§7• §8/plotholo §7Erstelle dir ein eigenes Hologram auf deinem Plot",
                "§7• §8/paytoggle §7Deaktiviere Zahlungen an dich",
                "§7• §8/premium §7Verschenke Premium 7 Tage",
                "§7• §8/skull §7Gebe dir alle 4 Tage einen Spielerkopf",
                " ",
                "§7§lAnderes:",
                "§7• Du erhälst den §c§lWonder §7Prefix",
                "§7• Du erhälst alle Plot Ränder & Wände",
                "§7• (Bestimmte Effekte)",
                "§7• (Bestimmte Haustiere)",
                "§7• Du kannst 12 Homes setzen",
                "§7• Du kannst 12 Plots besitzen",
                "§7• §lDu erhälst alle Funktionen vom Premium, Platin und Titan Rang"
        ));
        diamond.setItemMeta(diamondmeta);

        //12, 14,
        //21, 23,
        //35

        inv.setItem(12, nethscrape);
        inv.setItem(14, amtcluster);
        inv.setItem(21, emerlad);
        inv.setItem(23, diamond);

        Item item = new Item(Material.GRAY_STAINED_GLASS_PANE);
        item.setDisplayName(" ");
        for(int i = 0; i < inv.getSize(); i++){
            if(inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR){
                inv.setItem(i, item.build());
            }
        }

        player.openInventory(inv);


    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        if(!(sender instanceof  Player)) return true;


    if(args.length == 0){

        createInv(player);

    }else{
        player.sendMessage(Strings.prefix + "§7Benutzung: /ranginfo");
    }


        return false;
    }

    @EventHandler
    public void invclick(InventoryClickEvent e){

        if(!(e.getWhoClicked() instanceof Player)) return;;

        Player pl = (Player) e.getWhoClicked();

        if(e.getView().getTitle().equals(invname)) {
            e.setCancelled(true);



        }

    }

}
