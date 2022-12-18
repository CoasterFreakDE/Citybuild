package dev.lupluv.cb.commands;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import dev.lupluv.cb.economy.Economy;
import dev.lupluv.cb.utils.*;
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

public class RangshopCmd implements CommandExecutor, Listener {

    public static Inventory inv;
    public static String invname = "§6§lRangshop";

    public void createInv(Player player){

        inv = Bukkit.createInventory(null, 9*3, invname);

        ItemStack nethscrape = new ItemStack(Material.NETHERITE_SCRAP);
        ItemMeta nethscrapemeta = nethscrape.getItemMeta();
        nethscrapemeta.setDisplayName("§6§lPremium");
        ArrayList<String> lore1 = new ArrayList<>();
        lore1.add(" ");
        lore1.add("§7Der §6Premium §7Rang kostet §a100.000 §7Coins");
        lore1.add("§7§8(§7Rechtsclick zum Kaufen§8)");
        lore1.add(" ");
        lore1.add("§7Um zu sehen was der Rang kann benutzte §a/ranginfo");
        lore1.add(" ");
        nethscrapemeta.setLore(lore1);
        nethscrape.setItemMeta(nethscrapemeta);

        ItemStack amtcluster = new ItemStack(Material.AMETHYST_CLUSTER);
        ItemMeta amtclustermeta = amtcluster.getItemMeta();
        amtclustermeta.setDisplayName("§9§lTitan");
        amtclustermeta.setLore(Lore.create(
                " ",
                "§7Der §9Titan §7Rang kostet §a600.000 §7Coins",
                "§7§8(§7Rechtsclick zum Kaufen§8)",
                " ",
                "§7Um zu sehen was der Rang kann benutzte §a/ranginfo",
                " "
        ));
        amtcluster.setItemMeta(amtclustermeta);

        ItemStack emerlad = new ItemStack(Material.EMERALD);
        ItemMeta emerladmeta = emerlad.getItemMeta();
        emerladmeta.setDisplayName("§2§lPlatin");
        emerladmeta.setLore(Lore.create(
                " ",
                "§7Der §2Platin §7Rang kostet §a225.000 §7Coins",
                "§7§8(§7Rechtsclick zum Kaufen§8)",
                " ",
                "§7Um zu sehen was der Rang kann benutzte §a/ranginfo",
                " "
//8,13,16
        ));
        emerlad.setItemMeta(emerladmeta);

        inv.setItem(10, nethscrape);
        inv.setItem(13, emerlad);
        inv.setItem(16, amtcluster);

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if(args.length == 0){

            createInv(player);

        }else{
            player.sendMessage(Strings.prefix + "Benutzung /ranshop");
        }

        return false;
    }

    public void giveRank(Player player, String group){
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "shop_manager give " + player + " " + group);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){

        if(!(e.getWhoClicked() instanceof Player)) return;
        if(e.getCurrentItem() == null) return;
        Player pl = (Player) e.getWhoClicked();

        if(e.getView().getTitle().equals(invname)) {
            e.setCancelled(true);
            IPermissionUser user = CloudNetDriver.getInstance().getPermissionManagement().getUser(pl.getUniqueId());
            switch (e.getCurrentItem().getItemMeta().getDisplayName()){

                case "§6§lPremium":

                    if(Economy.getBalance(pl.getUniqueId()) >= 100000){

                        if(!Economy.withdrawPlayer(pl.getUniqueId(), 100000).transactionSuccess()){
                            return;
                        }

                        //Ränge vergeben
                        //user.addGroup("premium");

                        giveRank(pl, "premium");
                    }else{
                        pl.sendMessage(Strings.prefix + "Du hast leider nicht genügend Geld um dir diesen Rang zu kaufen");
                        pl.closeInventory();
                    }

                    break;
                case "§9§lTitan":

                    if(Economy.getBalance(pl.getUniqueId()) >= 600000){


                        if(!Economy.withdrawPlayer(pl.getUniqueId(), 600000).transactionSuccess()){
                            return;
                        }
                        //Ränge vergeben
                        //user.addGroup("titan");
                        giveRank(pl, "titan");

                    }else{
                        pl.sendMessage(Strings.prefix + "Du hast leider nicht genügend Geld um dir diesen Rang zu kaufen");
                        pl.closeInventory();
                    }

                    break;
                case "§2§lPlatin":

                    if(Economy.getBalance(pl.getUniqueId()) >= 225000){

                        if(!Economy.withdrawPlayer(pl.getUniqueId(), 225000).transactionSuccess()){
                            return;
                        }

                        //Ränge vergeben
                        //user.addGroup("platin");
                        giveRank(pl, "platin");


                    }else{
                        pl.sendMessage(Strings.prefix + "Du hast leider nicht genügend Geld um dir diesen Rang zu kaufen");
                        pl.closeInventory();
                    }

                    break;

            }
            switch (e.getCurrentItem().getType()){

                case BLACK_STAINED_GLASS:
                    pl.sendMessage(" ");
                    break;

            }
        }


    }



}