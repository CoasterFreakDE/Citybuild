package dev.lupluv.cb.shop;

import dev.lupluv.cb.utils.CbItem;
import dev.lupluv.cb.utils.Item;
import dev.lupluv.cb.utils.Lore;
import dev.lupluv.cb.utils.Worth;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class Adminshop {

    public Adminshop(Player p) {
        this.p = p;
    }

    Player p;

    public void open(){
        Inventory inv = Bukkit.createInventory(null, 9*6, "§6§lAdminshop");

        // Diamond
        Item diamond = new Item(Material.DIAMOND);
        diamond.setDisplayName("§7Diamant");
        diamond.setLore(Lore.create(
                " ",
                "§7Kaufen für §a" + Worth.getWorth(CbItem.DIAMOND).getBuy() + "§7 pro Item",
                "§7Verkaufen für §a" + Worth.getWorth(CbItem.DIAMOND).getSell() + "§7 pro Item",
                " ",
                "§7Linksklick um zu kaufen",
                "§7Rechtsklick um zu verkaufen",
                " "
                ));
        inv.setItem(10, diamond.build());

        // Gold
        Item gold = new Item(Material.GOLD_INGOT);
        gold.setDisplayName("§7Gold");
        gold.setLore(Lore.create(
                " ",
                "§7Kaufen für §a" + Worth.getWorth(CbItem.GOLD_INGOT).getBuy() + "§7 pro Item",
                "§7Verkaufen für §a" + Worth.getWorth(CbItem.GOLD_INGOT).getSell() + "§7 pro Item",
                " ",
                "§7Linksklick um zu kaufen",
                "§7Rechtsklick um zu verkaufen",
                " "
        ));
        inv.setItem(11, gold.build());

        // Iron
        Item iron = new Item(Material.RAW_IRON);
        iron.setDisplayName("§7Rohes Eisen");
        iron.setLore(Lore.create(
                " ",
                "§7Kaufen für §a" + Worth.getWorth(CbItem.RAW_IRON).getBuy() + "§7 pro Item",
                "§7Verkaufen für §a" + Worth.getWorth(CbItem.RAW_IRON).getSell() + "§7 pro Item",
                " ",
                "§7Linksklick um zu kaufen",
                "§7Rechtsklick um zu verkaufen",
                " "
        ));
        inv.setItem(12, iron.build());

        // Copper
        Item copper = new Item(Material.COPPER_INGOT);
        copper.setDisplayName("§7Kupfer");
        copper.setLore(Lore.create(
                " ",
                "§7Kaufen für §a" + Worth.getWorth(CbItem.COPPER_INGOT).getBuy() + "§7 pro Item",
                "§7Verkaufen für §a" + Worth.getWorth(CbItem.COPPER_INGOT).getSell() + "§7 pro Item",
                " ",
                "§7Linksklick um zu kaufen",
                "§7Rechtsklick um zu verkaufen",
                " "
        ));
        inv.setItem(13, copper.build());

        // Coal
        Item coal = new Item(Material.COAL);
        coal.setDisplayName("§7Kohle");
        coal.setLore(Lore.create(
                " ",
                "§7Kaufen für §a" + Worth.getWorth(CbItem.COAL).getBuy() + "§7 pro Item",
                "§7Verkaufen für §a" + Worth.getWorth(CbItem.COAL).getSell() + "§7 pro Item",
                " ",
                "§7Linksklick um zu kaufen",
                "§7Rechtsklick um zu verkaufen",
                " "
        ));
        inv.setItem(14, coal.build());

        for(int i = 0; i < inv.getSize(); i++){
            if(inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR){
                Item item = new Item(Material.GRAY_STAINED_GLASS_PANE);
                item.setDisplayName(" ");
                inv.setItem(i, item.build());
            }
        }

        p.openInventory(inv);
    }

}
