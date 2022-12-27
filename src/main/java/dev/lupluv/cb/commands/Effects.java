package dev.lupluv.cb.commands;

import dev.lupluv.cb.particels.Particel;
import dev.lupluv.cb.utils.Item;
import dev.lupluv.cb.utils.Lore;
import dev.lupluv.cb.utils.Strings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;


public class Effects implements CommandExecutor, Listener {

    public static Inventory inv;
    public static String invname = "§6§lEffekte";
    public static String lore = "§7Rechtsklick um den Effekt auszuwählen.";
    public static String lore2 = "§cDu hast diesen Effekt nicht!";

    public static void createAndOpenInventory(Player player){

        inv = Bukkit.createInventory(null, 9*5, invname);

        Item mangrove = new Item(Material.MANGROVE_PLANKS);
        mangrove.setDisplayName("§c§lHerz Effekt");
        if(!player.hasPermission("cb.effekt.herzen")){
            mangrove.setLore(Lore.create(lore2));
        }else{
            mangrove.setLore(Lore.create(lore));
        }
        inv.setItem(11, mangrove.build());

        Item oak = new Item(Material.OAK_PLANKS);
        oak.setDisplayName("§a§lNoten Effekt");
        if(!player.hasPermission("cb.effekt.noten")){
            oak.setLore(Lore.create(lore2));
        }else{
            oak.setLore(Lore.create(lore));
        }
        inv.setItem(20, oak.build());



        Item jungel = new Item(Material.JUNGLE_PLANKS);
        jungel.setDisplayName("§2§lVilliager Effekt");
        if(!player.hasPermission("cb.effekt.villiager.happy")){
            jungel.setLore(Lore.create(lore2));
        }else{
            jungel.setLore(Lore.create(lore));
        }
        inv.setItem(29, jungel.build());





        Item acacia = new Item(Material.ACACIA_PLANKS);
        acacia.setDisplayName("§8§lRauch Effekt");
        if(!player.hasPermission("cb.effekt.rauch")){
            acacia.setLore(Lore.create(lore2));
        }else{
            acacia.setLore(Lore.create(lore));
        }
        inv.setItem(13, acacia.build());


        Item darkoak = new Item(Material.DARK_OAK_PLANKS);
        darkoak.setDisplayName("§4§lLava Effekt");
        if(!player.hasPermission("cb.effekt.lava")){
            darkoak.setLore(Lore.create(lore2));
        }else{
            darkoak.setLore(Lore.create(lore));
        }
        inv.setItem(22, darkoak.build());







        Item warpedplank = new Item(Material.WARPED_PLANKS);
        warpedplank.setDisplayName("§6§lSchaden effekt");
        if(!player.hasPermission("cb.effekt.schaden")){
            warpedplank.setLore(Lore.create(lore2));
        }else{
            warpedplank.setLore(Lore.create(lore));
        }
        inv.setItem(31, warpedplank.build());



        Item crimson = new Item(Material.CRIMSON_PLANKS);
        crimson.setDisplayName("§9§lRedstone Effekt");
        if(!player.hasPermission("cb.effekt.redstone")){
            crimson.setLore(Lore.create(lore2));
        }else{
            crimson.setLore(Lore.create(lore));
        }
        inv.setItem(15, crimson.build());


        Item spruce = new Item(Material.SPRUCE_PLANKS);
        spruce.setDisplayName("§5§lDrachen Effekt");
        if(!player.hasPermission("cb.effekt.drachen")){
            spruce.setLore(Lore.create(lore2));
        }else{
            spruce.setLore(Lore.create(lore));
        }
        inv.setItem(24, spruce.build());


        Item birch = new Item(Material.BIRCH_PLANKS);
        birch.setDisplayName("§3§lMonster Effekt");
        if(!player.hasPermission("cb.effekt.monster")){
            birch.setLore(Lore.create(lore2));
        }else{
            birch.setLore(Lore.create(lore));
        }
        inv.setItem(33, birch.build());

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

        if(!(sender instanceof Player player)) return true;
        if(!(player.hasPermission("cb.effekte"))) return true;
        if(args.length == 0){


        }else{
            player.sendMessage(Strings.prefix + "§aBenutzung /effekt");
        }

        return false;
    }


    @EventHandler
    public void onClick(InventoryClickEvent e){

        if(!(e.getWhoClicked() instanceof Player)) return;
        if(e.getCurrentItem() == null) return;
        Player pl = (Player) e.getWhoClicked();

        if(e.getView().getTitle().equals(invname)) {
            e.setCancelled(true);
            switch (e.getCurrentItem().getItemMeta().getDisplayName()){

         case "§c§lHerz Effekt" -> {

            if(Particel.has_one_effect.contains(pl)){

                if(Particel.haseffect1.contains(pl)){
                    Particel.haseffect1.remove(pl);
                    pl.sendMessage(Strings.prefix + "§cDer Effekt wurde dir erfolgreich entfernt.");
                }else{
                    pl.sendMessage(Strings.prefix + "§cDu hast bereits ein Effekt ausgewählt");
                }


            }else{
                Particel.haseffect1.add(pl);
            }

             break;

         }
        case "§a§lNoten Effekt" -> {



             break;
        }
        case "§2§lVilliager Effekt" -> {

             break;
        }

        case "§8§lRauch Effekt" -> {


             break;
        }
        case "§4§lLava Effekt" -> {



             break;
        }
        case "§6§lSchaden effekt" -> {


             break;
        }
        case "§9§lRedstone Effekt" -> {


             break;
        }
        case "§5§lDrachen Effekt" -> {


             break;
        }
        case "§3§lMonster Effekt" -> {


             break;
        }





            }
            switch (e.getCurrentItem().getType()){

                case BLACK_STAINED_GLASS:
                    pl.sendMessage(" ");
                    break;

            }
        }

    }


}
