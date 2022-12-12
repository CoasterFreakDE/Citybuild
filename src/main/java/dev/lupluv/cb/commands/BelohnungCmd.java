package dev.lupluv.cb.commands;

import dev.lupluv.cb.utils.Item;
import dev.lupluv.cb.utils.Strings;
import org.apache.logging.log4j.core.util.SystemMillisClock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

public class BelohnungCmd implements CommandExecutor, Listener {

    public Inventory inv;
    public String invname = "§6§lBelohnung";

    public static File BelohnungsFile;
    public static FileConfiguration cfg;



    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if(args.length == 0){

            createInv(player);

        }else{
            player.sendMessage(Strings.prefix + "Benutzung: /belohnung");
        }

        return false;
    }

    public void createInv(Player player){

        inv = Bukkit.createInventory(null, 9*3, invname);

        ItemStack goldblock = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta goldblockmeta = goldblock.getItemMeta();
        goldblockmeta.setDisplayName("§c§lTages Belohnung");
        ArrayList<String> lore1 = new ArrayList<>();
        lore1.add("§7Hole dir deine Tagesbelohnung");
        goldblockmeta.setLore(lore1);
        goldblock.setItemMeta(goldblockmeta);

        ItemStack diamond = new ItemStack(Material.DIAMOND_BLOCK);
        ItemMeta diamondmeta = diamond.getItemMeta();
        diamondmeta.setDisplayName("§c§lWochen Belohnung");
        ArrayList<String> lore2 = new ArrayList<>();
        lore2.add("§7Hole dir deine Wochenbelohnung");
        diamondmeta.setLore(lore2);
        diamond.setItemMeta(diamondmeta);

        inv.setItem(12, goldblock);
        inv.setItem(14, diamond);

        Item item = new Item(Material.GRAY_STAINED_GLASS_PANE);
        item.setDisplayName(" ");
        for(int i = 0; i < inv.getSize(); i++){
            if(inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR){
                inv.setItem(i, item.build());
            }
        }

        player.openInventory(inv);

    }

    @EventHandler
    public void onClick(InventoryClickEvent e){

        if(!(e.getWhoClicked() instanceof Player)) return;
        if(e.getCurrentItem() == null) return;
        Player pl = (Player) e.getWhoClicked();

        if(e.getView().getTitle().equals(invname)) {
            e.setCancelled(true);
            switch (e.getCurrentItem().getItemMeta().getDisplayName()){

                case "§c§lTages Belohnung":

                    long milli = System.currentTimeMillis();

                    getTagesBelohnung(pl, milli);

                    break;
                case "§c§lWochen Belohnung":

                    long milli2 = System.currentTimeMillis();

                    getWochenBelohnung(pl, milli2);

                    break;

            }
            switch (e.getCurrentItem().getType()){

                case BLACK_STAINED_GLASS:
                    pl.sendMessage(" ");
                    break;

            }
        }

    }

    public void getTagesBelohnung(Player player, long Millisconds){
//500
     player.sendMessage(Strings.prefix + "Diese Fuktion ist noch nicht fertig!");


    }

    public void getWochenBelohnung(Player player, long Milliseconds){
//3000
        player.sendMessage(Strings.prefix + "Diese Fuktion ist noch nicht fertig!");

    }


}
