package dev.lupluv.cb.events;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import dev.lupluv.cb.Citybuild;
import dev.lupluv.cb.utils.Home;
import dev.lupluv.cb.utils.InventoryManager;
import dev.lupluv.cb.utils.Strings;
import dev.lupluv.cb.utils.Warp;
import dev.lupluv.cb.voting.VoteAPI;
import dev.lupluv.cb.voting.VoteFetcher;
import dev.lupluv.cb.voting.VoteSite;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class ClickHandler implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(e.getCurrentItem() == null) return;
        if(e.getCurrentItem().getType() == Material.AIR) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        Inventory inv = e.getClickedInventory();
        ItemStack item = e.getCurrentItem();
        Material mat = item.getType();

        if(title.equalsIgnoreCase("§6§lWarps")){
            e.setCancelled(true);
            String w = "fehler";
            if(item.getItemMeta().getDisplayName().equalsIgnoreCase("§6§lBank")){
                w = "bank";
            }else if(item.getItemMeta().getDisplayName().equalsIgnoreCase("§6§lLaufbursche")){
                w = "laufbursche";
            }else if(item.getItemMeta().getDisplayName().equalsIgnoreCase("§6§lFarmwelten")){
                w = "farmwelt";
            }else if(item.getItemMeta().getDisplayName().equalsIgnoreCase("§6§lCasino")){
                w = "casino";
            }else if(item.getItemMeta().getDisplayName().equalsIgnoreCase("§6§lCrates")){
                w = "crates";
            }else if(item.getItemMeta().getDisplayName().equalsIgnoreCase("§6§lIndustriehaus")){
                w = "industriehaus";
            }else if(item.getItemMeta().getDisplayName().equalsIgnoreCase("§6§lItem Shop")){
                w = "item-shop";
            }
            p.performCommand("warp " + w);
        }else if(title.equalsIgnoreCase("§6§lWarps Admin")){
            if(mat == Material.PAPER){
                String name = item.getItemMeta().getDisplayName().replace("§c§l", "");
                Warp w = new Warp(name);
                if(w.exists()){
                    w.load();
                    w.teleport(p);
                }else{
                    p.sendMessage(Strings.prefix + "§cDer Warp '" + name + "' existiert nicht.");
                }
            }
        }else if(title.equalsIgnoreCase("§6Deine Homes")){
            e.setCancelled(true);
            if(mat == Material.WHITE_BED){
                Home h = new Home(p.getUniqueId(), item.getItemMeta().getDisplayName().replace("§c§l", ""));
                if(h.exists()){
                    p.performCommand("home " + h.getName());
                }
            }else if(mat == Material.GREEN_WOOL){
                p.openInventory(Citybuild.getInventoryManager().getHomeCreationInventory(p));
            }
        }else if(title.equalsIgnoreCase("§6§lPlots")){
            e.setCancelled(true);
            if(item.getItemMeta().getDisplayName().equalsIgnoreCase("§6Plot erstellen")){
                p.closeInventory();
                p.performCommand("plot auto");
            }else if(item.getItemMeta().getDisplayName().equalsIgnoreCase("§6Home")){
                p.openInventory(Citybuild.getInventoryManager().getPlotsHomeInventory(p));
            }
        }else if(title.equalsIgnoreCase("§6§lHome")){
            e.setCancelled(true);
            if(mat == Material.SUNFLOWER){
                String[] parts = item.getLore().get(0).replace("§7ID §8• §7", "").split(":");
                p.performCommand("plot visit " + parts[0] + "," + parts[1]);
            }
        }else if(title.equalsIgnoreCase("§6§lVote Belohnungen")){
            e.setCancelled(true);
            if(item.getItemMeta().getDisplayName().contains("Vote 1")){
                int hasVoted;
                VoteFetcher vf1 = new VoteFetcher(VoteSite.vote1, p.getName());
                if(!vf1.exists()) {
                    hasVoted = 0;
                }else{
                    vf1.load();
                    hasVoted = vf1.getResult();
                }
                if(hasVoted == 0) {
                    // Has not voted and not claimed
                    p.closeInventory();
                    p.sendMessage(Strings.prefix + "§7Vote jetzt unter: §6https://vote.wonderbuild.net");
                }else if(hasVoted == 1){
                    // Has voted but not claimed
                    vf1.setClaimed(true);
                    vf1.update();
                    p.closeInventory();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate giveKey vote " + p.getName() + " 1");
                    p.sendMessage(Strings.prefix + "§7Du hast deine §6Belohnung §7erhalten.");
                }else if(hasVoted == 2){
                    // Has voted and claimed
                    p.closeInventory();
                    p.sendMessage(Strings.prefix + "§7Vote jetzt unter: §6https://vote.wonderbuild.net");
                }
            }
        }

    }

}
