package dev.lupluv.cb.namecolors;

import dev.lupluv.cb.utils.Item;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class NameColorUI {

    Player player;

    Inventory inventory;

    Component invName;

    public NameColorUI(Player player) {
        this.player = player;
    }

    public void setMainGUI(){
        invName = Component.text("§6§lNamesfarben");
        inventory = Bukkit.createInventory(null, 9*5, invName);
        Item glassItem = new Item(Material.GRAY_STAINED_GLASS_PANE);
        glassItem.setDisplayName(" ");
        ItemStack glass = glassItem.build();

        inventory.setItem(0, glass);
        inventory.setItem(1, glass);
        inventory.setItem(2, glass);
        inventory.setItem(3, glass);
        inventory.setItem(4, glass);
        inventory.setItem(5, glass);
        inventory.setItem(6, glass);
        inventory.setItem(7, glass);
        inventory.setItem(8, glass);
        inventory.setItem(9, glass);

        // 2nd
        inventory.setItem(9+8, glass);
        inventory.setItem(9+9, glass);
        // 3rd
        inventory.setItem(9+9+8, glass);
        inventory.setItem(9+9+9, glass);
        // 4th
        inventory.setItem(9+9+9+8, glass);
        inventory.setItem(9+9+9+9, glass);
        // 5th
        inventory.setItem(9+9+9+9+8, glass);

        List<NColor> nColors = List.of(NColor.values());
    }

    public void openGUI(){

    }

}
