package dev.lupluv.cb.commands;

import dev.lupluv.cb.Citybuild;
import dev.lupluv.cb.annotations.RegisterCommand;
import dev.lupluv.cb.particels.Particel;
import dev.lupluv.cb.utils.Item;
import dev.lupluv.cb.utils.Lore;
import dev.lupluv.cb.utils.Strings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Objects;


@RegisterCommand(name = "effects", aliases = {"effekt", "effect"}, description = "Opens the effects menu", permissionDefault = PermissionDefault.TRUE)
public class Effects implements CommandExecutor, Listener {

    public static Inventory inv;
    public static String invname = "§6§lEffekte";
    public static String lore = "§7Rechtsklick um den Effekt auszuwählen.";
    public static String lore2 = "§cDu hast diesen Effekt nicht!";

    public static void createAndOpenInventory(Player player){

        inv = Bukkit.createInventory(null, 9 * 5, invname);

        Item mangrove = new Item(Material.MANGROVE_PLANKS);
        mangrove.setDisplayName("§c§lHerz Effekt");
        setLoreForItem(player, "cb.effekt.herzen", mangrove, (byte) 1);
        inv.setItem(11, mangrove.build());

        Item oak = new Item(Material.OAK_PLANKS);
        oak.setDisplayName("§a§lNoten Effekt");
        setLoreForItem(player, "cb.effekt.noten", oak, (byte) 2);
        inv.setItem(20, oak.build());

        Item jungel = new Item(Material.JUNGLE_PLANKS);
        jungel.setDisplayName("§2§lVilliager Effekt");
        setLoreForItem(player, "cb.effekt.villigar.happy", jungel, (byte) 3);
        inv.setItem(29, jungel.build());

        Item acacia = new Item(Material.ACACIA_PLANKS);
        acacia.setDisplayName("§8§lRauch Effekt");
        setLoreForItem(player, "cb.effekt.rauch", acacia, (byte) 4);
        inv.setItem(13, acacia.build());


        Item darkoak = new Item(Material.DARK_OAK_PLANKS);
        darkoak.setDisplayName("§4§lLava Effekt");
        setLoreForItem(player, "cb.effekt.lava", darkoak, (byte) 5);
        inv.setItem(22, darkoak.build());

        Item warpedplank = new Item(Material.WARPED_PLANKS);
        warpedplank.setDisplayName("§6§lSchaden effekt");
        setLoreForItem(player, "cb.effekt.schaden", warpedplank, (byte) 6);
        inv.setItem(31, warpedplank.build());

        Item crimson = new Item(Material.CRIMSON_PLANKS);
        crimson.setDisplayName("§9§lRedstone Effekt");
        setLoreForItem(player, "cb.effekt.redstone", crimson, (byte) 7);
        inv.setItem(15, crimson.build());


        Item spruce = new Item(Material.SPRUCE_PLANKS);
        spruce.setDisplayName("§5§lDrachen Effekt");
        setLoreForItem(player, "cb.effekt.drachen", spruce, (byte) 8);
        inv.setItem(24, spruce.build());


        Item birch = new Item(Material.BIRCH_PLANKS);
        birch.setDisplayName("§3§lMonster Effekt");
        setLoreForItem(player, "cb.effekt.monster", birch, (byte) 9);

        Item barrier = new Item(Material.BARRIER);
        barrier.setDisplayName("§c§lZurücksetzten");
        inv.setItem(40, barrier.build());

        inv.setItem(33, birch.build());

        Item item = new Item(Material.GRAY_STAINED_GLASS_PANE);
        item.setDisplayName(" ");
        for(int i = 0; i < inv.getSize(); i++){
            if(inv.getItem(i) == null || Objects.requireNonNull(inv.getItem(i)).getType() == Material.AIR){
                inv.setItem(i, item.build());
            }
        }
        player.openInventory(inv);
    }

    private static void setLoreForItem(Player player, String name, Item item, byte effectId) {
        if (!player.hasPermission(name)) {
            item.setLore(Lore.create(lore2));
            return;
        }
        NamespacedKey key = new NamespacedKey(Citybuild.getPlugin(), "effect");
        if (!player.getPersistentDataContainer().has(key, PersistentDataType.BYTE) || player.getPersistentDataContainer().get(key, PersistentDataType.BYTE) != effectId) {
            item.setLore(Lore.create(lore, " ", "§cNicht Ausgewählt"));
            return;
        }
        item.setLore(Lore.create(lore, " ", "§aAusgewählt"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player player)) return true;

        if(args.length == 0){

        createAndOpenInventory(player);
        }else{
            player.sendMessage(Strings.prefix + "§aBenutzung /effekt");
        }

        return false;
    }


    @EventHandler
    public void onClick(InventoryClickEvent e){

        if(!(e.getWhoClicked() instanceof Player player)) return;
        if(e.getCurrentItem() == null) return;

        if(e.getView().getTitle().equals(invname)) {
            e.setCancelled(true);
            NamespacedKey key = new NamespacedKey(Citybuild.getPlugin(), "effect");
            byte effect = 0;
            if(player.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                effect = player.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
            }

            switch (e.getCurrentItem().getItemMeta().getDisplayName()) {
                 case "§c§lHerz Effekt" -> {
                     if(!player.hasPermission("cb.effekt.herzen")) return;
                     if(effect == 1) {
                        player.getPersistentDataContainer().remove(key);
                        player.sendMessage(Strings.prefix + "§aDu hast den Effekt §cHerz §aentfernt.");
                        player.closeInventory();
                        return;
                     }
                    player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
                    player.sendMessage(Strings.prefix + "§aDu hast den Effekt §cHerz §aaktiviert.");
                    player.closeInventory();
                    return;
                }
                case "§a§lNoten Effekt" -> {
                    if(!player.hasPermission("cb.effekt.noten")) return;
                    if(effect == 2) {
                        player.getPersistentDataContainer().remove(key);
                        player.sendMessage(Strings.prefix + "§aDu hast den Effekt §aNoten §aentfernt.");
                        player.closeInventory();
                        return;
                    }
                    player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 2);
                    player.sendMessage(Strings.prefix + "§aDu hast den Effekt §aNoten §aaktiviert.");
                    player.closeInventory();
                    return;
                }
                case "§2§lVilliager Effekt" -> {
                    if(!player.hasPermission("cb.effekt.villigar.happy")) return;
                    if(effect == 3) {
                        player.getPersistentDataContainer().remove(key);
                        player.sendMessage(Strings.prefix + "§aDu hast den Effekt §2Villigar §aentfernt.");
                        player.closeInventory();
                        return;
                    }
                    player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 3);
                    player.sendMessage(Strings.prefix + "§aDu hast den Effekt §2Villigar §aaktiviert.");
                    player.closeInventory();
                    return;
                }
                case "§8§lRauch Effekt" -> {
                    if(!player.hasPermission("cb.effekt.rauch")) return;
                    if(effect == 4) {
                        player.getPersistentDataContainer().remove(key);
                        player.sendMessage(Strings.prefix + "§aDu hast den Effekt §8Rauch §aentfernt.");
                        player.closeInventory();
                        return;
                    }
                    player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 4);
                    player.sendMessage(Strings.prefix + "§aDu hast den Effekt §8Rauch §aaktiviert.");
                    player.closeInventory();
                    return;
                }
                case "§4§lLava Effekt" -> {
                    if(!player.hasPermission("cb.effekt.lava")) return;
                    if(effect == 5) {
                        player.getPersistentDataContainer().remove(key);
                        player.sendMessage(Strings.prefix + "§aDu hast den Effekt §4Lava §aentfernt.");
                        player.closeInventory();
                        return;
                    }
                    player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 5);
                    player.sendMessage(Strings.prefix + "§aDu hast den Effekt §4Lava §aaktiviert.");
                    player.closeInventory();
                    return;
                }
                case "§6§lSchaden effekt" -> {
                    if(!player.hasPermission("cb.effekt.schaden")) return;
                    if(effect == 6) {
                        player.getPersistentDataContainer().remove(key);
                        player.sendMessage(Strings.prefix + "§aDu hast den Effekt §6Schaden §aentfernt.");
                        player.closeInventory();
                        return;
                    }
                    player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 6);
                    player.sendMessage(Strings.prefix + "§aDu hast den Effekt §6Schaden §aaktiviert.");
                    player.closeInventory();
                    return;
                }
                case "§9§lRedstone Effekt" -> {
                    if(!player.hasPermission("cb.effekt.redstone")) return;
                    if(effect == 7) {
                        player.getPersistentDataContainer().remove(key);
                        player.sendMessage(Strings.prefix + "§aDu hast den Effekt §9Redstone §aentfernt.");
                        player.closeInventory();
                        return;
                    }
                    player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 7);
                    player.sendMessage(Strings.prefix + "§aDu hast den Effekt §9Redstone §aaktiviert.");
                    player.closeInventory();
                    return;
                }
                case "§5§lDrachen Effekt" -> {
                    if(!player.hasPermission("cb.effekt.drachen")) return;
                    if(effect == 8) {
                        player.getPersistentDataContainer().remove(key);
                        player.sendMessage(Strings.prefix + "§aDu hast den Effekt §5Drachen §aentfernt.");
                        player.closeInventory();
                        return;
                    }
                    player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 8);
                    player.sendMessage(Strings.prefix + "§aDu hast den Effekt §5Drachen §aaktiviert.");
                    player.closeInventory();
                    return;
                }
                case "§3§lMonster Effekt" -> {
                    if(!player.hasPermission("cb.effekt.monster")) return;
                    if(effect == 9) {
                        player.getPersistentDataContainer().remove(key);
                        player.sendMessage(Strings.prefix + "§aDu hast den Effekt §3Monster §aentfernt.");
                        player.closeInventory();
                        return;
                    }
                    player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 9);
                    player.sendMessage(Strings.prefix + "§aDu hast den Effekt §3Monster §aaktiviert.");
                    player.closeInventory();
                    return;
                }
                case "§c§lZurücksetzten" -> {
                    player.getPersistentDataContainer().remove(key);
                    player.sendMessage(" ");
                    player.sendMessage(Strings.prefix + "§cAlle deine Effekte wurde zurückgesetzt!");
                    player.sendMessage(" ");
                    player.closeInventory();
                    return;
                }
            }

            // Why?
            if (e.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS) {
                player.sendMessage(" ");
            }
        }
    }
}
