package dev.lupluv.cb.namecolors;

import dev.lupluv.cb.scoreboard.ScoreboardManager;
import dev.lupluv.cb.utils.Item;
import dev.lupluv.cb.utils.Lore;
import dev.lupluv.cb.utils.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;

public class NameColorUI {

    Player player;

    Inventory inventory;

    Component invName;

    public NameColorUI(Player player) {
        this.player = player;
    }

    public void setMainGUI(){
        invName = Component.text("§6§lNamensfarben");
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
        inventory.setItem(9+9+9+9+1, glass);
        inventory.setItem(9+9+9+9+2, glass);
        inventory.setItem(9+9+9+9+3, glass);
        inventory.setItem(9+9+9+9+4, glass);
        inventory.setItem(9+9+9+9+5, glass);
        inventory.setItem(9+9+9+9+6, glass);
        inventory.setItem(9+9+9+9+7, glass);
        inventory.setItem(9+9+9+9+8, glass);

        List<NColor> nColors = List.of(NColor.values());
        int i = 0;
        for(NColor nColor : nColors){
            if(i > 21){
                break;
            }

            if(nColor != NColor.NONE) {
                NameColorSelector ncs = new NameColorSelector(player.getUniqueId());
                if (!ncs.existsByUuid()) return;
                ncs.loadByUuid();
                NColor sel = NColor.valueOf(ncs.getName_color());

                // Set item
                ItemStack is;
                if (player.hasPermission("cb.namecolor.color." + nColor.toString())) {
                    if (sel == nColor) {
                        is = Util.createCustomSkull(nColor.getValue(), ScoreboardManager.format2(nColor.format(nColor.getName())), Lore.create(
                                " ",
                                "§aAusgewählt.",
                                " "
                        ));
                        ItemMeta im = is.getItemMeta();
                        im.addEnchant(Enchantment.CHANNELING, 0, true);
                        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                        im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        is.setItemMeta(im);
                        inventory.addItem(is);
                    } else {
                        is = Util.createCustomSkull(nColor.getValue(), ScoreboardManager.format2(nColor.format(nColor.getName())), Lore.create(
                                " ",
                                "§eKlicke zum auswählen.",
                                " "
                        ));
                        inventory.addItem(is);
                    }
                } else {
                    is = Util.createCustomSkull(nColor.getValue(), ScoreboardManager.format2(nColor.format(nColor.getName())), Lore.create(
                            " ",
                            "§cNicht in deinem Inventar.",
                            " "
                    ));
                    inventory.addItem(is);
                }
            }
            i++;
        }

    }

    public void setMainGUI2(){
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
        int i = 0;
        for(NColor nColor : nColors){
            if(i > 10){
                break;
            }

            NameColorSelector ncs = new NameColorSelector(player.getUniqueId());
            if(!ncs.existsByUuid()) return;
            ncs.loadByUuid();
            NColor sel = NColor.valueOf(ncs.getName_color());

            // Set item
            ItemStack is;
            if(player.hasPermission("cb.namecolor.color." + nColor.toString())) {
                if(sel == nColor) {
                    is = Util.createCustomSkull(nColor.getValue(), ScoreboardManager.format2(nColor.format(nColor.getName())), Lore.create(
                            " ",
                            "§aAusgewählt.",
                            " "
                    ));
                    ItemMeta im = is.getItemMeta();
                    im.addEnchant(Enchantment.CHANNELING, 0, true);
                    im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    is.setItemMeta(im);
                    inventory.addItem(is);
                }else{
                    is = Util.createCustomSkull(nColor.getValue(), ScoreboardManager.format2(nColor.format(nColor.getName())), Lore.create(
                            " ",
                            "§eKlicke zum auswählen.",
                            " "
                    ));
                    inventory.addItem(is);
                }
            }else{
                is = Util.createCustomSkull(nColor.getValue(), ScoreboardManager.format2(nColor.format(nColor.getName())), Lore.create(
                        " ",
                        "§cNicht in deinem Inventar.",
                        " "
                ));
                inventory.addItem(is);
            }
            i++;
        }
    }

    public void openGUI(){
        setMainGUI();
        player.openInventory(this.inventory);
    }

    public void openGUI2(){
        setMainGUI();
        player.openInventory(this.inventory);
    }

}
