package dev.lupluv.cb.commands;

import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.location.World;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.implementations.BreakFlag;
import com.plotsquared.core.plot.flag.implementations.FlyFlag;
import com.plotsquared.core.plot.flag.implementations.PlaceFlag;
import com.plotsquared.core.plot.flag.implementations.PvpFlag;
import dev.lupluv.cb.utils.FlagType;
import dev.lupluv.cb.utils.Item;
import dev.lupluv.cb.utils.Strings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

public class FlagCmd implements CommandExecutor, Listener {

    public Inventory inv;
    public final String invname = "§6§lFlags";

    public void createInv(Player player){

        inv = Bukkit.createInventory(null, 9*1, invname);

        ItemStack stonesword = new ItemStack(Material.STONE_SWORD);
        ItemMeta stoneswordmeta = stonesword.getItemMeta();
        stoneswordmeta.setDisplayName("§a§lPVP");
        ArrayList<String> lore1 = new ArrayList<>();
        if(getFlag(FlagType.PVP, player)){
            lore1.add("§7Status §8» §aaktiviert");
        }else{
            lore1.add("§7Status §8» §cdeaktiviert");
        }
        stoneswordmeta.setLore(lore1);
        stonesword.setItemMeta(stoneswordmeta);

        ItemStack stonepickaxe = new ItemStack(Material.STONE_PICKAXE);
        ItemMeta stonepickaxedmeta = stonepickaxe.getItemMeta();
        stonepickaxedmeta.setDisplayName("§a§lZerstören");
        ArrayList<String> lore2 = new ArrayList<>();
        if(getFlag(FlagType.BREAK, player)) {
            lore2.add("§7Status §8» §aaktiviert");
        }else{
            lore2.add("§7Status §8» §cdeaktiviert");
        }
        stonepickaxedmeta.setLore(lore2);
        stonepickaxe.setItemMeta(stonepickaxedmeta);

        ItemStack dirt = new ItemStack(Material.DIRT);
        ItemMeta dirtmeta = dirt.getItemMeta();
        dirtmeta.setDisplayName("§a§lPlatzieren");
        ArrayList<String> lore3 = new ArrayList<>();
        if(getFlag(FlagType.PLACE, player)){
            lore3.add("§7Status §8» §aaktiviert");
        }else{
            lore3.add("§7Status §8» §cdeaktiviert");
        }
        dirtmeta.setLore(lore3);
        dirt.setItemMeta(dirtmeta);

        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta feathermeta = feather.getItemMeta();
        feathermeta.setDisplayName("§a§lFliegen");
        ArrayList<String> lore4 = new ArrayList<>();
        if(getFlag(FlagType.FLY, player)){
            lore4.add("§7Status §8» §aaktiviert");
        }else{
            lore4.add("§7Status §8» §cdeaktiviert");
        }
        feathermeta.setLore(lore4);
        feather.setItemMeta(feathermeta);

        //2,3,5,6

        inv.setItem(2, stonesword);
        inv.setItem(3, stonepickaxe);
        inv.setItem(5, dirt);
        inv.setItem(6, feather);

        Item item = new Item(Material.GRAY_STAINED_GLASS_PANE);
        item.setDisplayName(" ");
        for(int i = 0; i < inv.getSize(); i++){
            if(inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR){
                inv.setItem(i, item.build());
            }
        }

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if(player.hasPermission("cb.flag")){
            if(args.length == 0){
                if(isInPlot(player)){
                    createInv(player);
                }else{
                    player.sendMessage(Strings.prefix + "§cDu hast kein Zugriff auf das Plot!");
                }
            }else{
                player.sendMessage(Strings.prefix + "Benutzung: /flag");
            }
        }else {
            player.sendMessage(Strings.prefix + Strings.noPerms);
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

                case "§a§lPVP":

                    if(getFlag(FlagType.PVP, pl)){
                        setFlag(FlagType.PVP, pl, false);
                    }else{
                        setFlag(FlagType.PVP, pl, true);
                    }

                    break;
                case "§a§lZerstören":

                    if(getFlag(FlagType.BREAK, pl)){
                        setFlag(FlagType.BREAK, pl, false);
                    }else{
                        setFlag(FlagType.BREAK, pl, true);

                    }

                    break;
                case "§a§lPlatzieren":

                    if(getFlag(FlagType.PLACE, pl)){
                        setFlag(FlagType.PLACE, pl, false);

                    }else{
                        setFlag(FlagType.PLACE, pl, true);
                    }

                    break;
                case "§a§lFliegen":

                    if(getFlag(FlagType.FLY, pl)){
                        setFlag(FlagType.FLY, pl, false);
                    }else{
                        setFlag(FlagType.FLY, pl, true);
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

    public static void setFlag(FlagType flagType, Player player, boolean bool){
        PlotAPI api = new PlotAPI();
        PlotPlayer pp = api.wrapPlayer(player.getUniqueId());
        Plot plot = api.getPlotSquared().getPlotAreaManager().getPlotAreaByString("cb").getPlot(pp.getLocation());
        if(plot != null){
            switch (flagType){
                case PVP ->{
                    plot.setFlag(PvpFlag.class, String.valueOf(bool));
                    break;
                }
                case FLY -> {
                    plot.setFlag(FlyFlag.class, String.valueOf(bool));
                    break;
                }
                case BREAK -> {
                    plot.setFlag(BreakFlag.class, String.valueOf(bool));
                    break;
                }
                case PLACE -> {
                    plot.setFlag(PlaceFlag.class, String.valueOf(bool));
                    break;
                }
            }
        }
    }

    public static boolean getFlag(FlagType flagType, Player player){
        PlotAPI api = new PlotAPI();
        PlotPlayer pp = api.wrapPlayer(player.getUniqueId());
        Plot plot = api.getPlotSquared().getPlotAreaManager().getPlotAreaByString("cb").getPlot(pp.getLocation());
        if(plot != null){
            switch (flagType){
                case PVP ->{
                    return plot.getFlag(PvpFlag.class).booleanValue();

                }
                case PLACE -> {
                    // return plot.getFlag(PlaceFlag.class);

                }
                case FLY -> {
                    return plot.getFlag(FlyFlag.class).compareTo(FlyFlag.FlyStatus.ENABLED) == 1;
                }
            }
        }
        return true;
    }

    public static boolean isInPlot(Player player){
        PlotAPI api = new PlotAPI();
        PlotPlayer pp = api.wrapPlayer(player.getUniqueId());
        Plot plot = api.getPlotSquared().getPlotAreaManager().getPlotAreaByString("cb").getPlot(pp.getLocation());
        if(plot != null){
            if(plot.isAdded(player.getUniqueId()) || player.hasPermission("cb.edit.flags.others")){
                return true;
            }
        }
        return false;
    }

}
