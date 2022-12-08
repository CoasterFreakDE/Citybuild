package dev.lupluv.cb.commands;

import dev.lupluv.cb.utils.Item;
import dev.lupluv.cb.utils.Lore;
import dev.lupluv.cb.utils.Strings;
import dev.lupluv.cb.utils.Util;
import net.md_5.bungee.api.ChatColor;
import net.melion.rgbchat.api.RGBApi;
import org.bukkit.Bukkit;
import org.bukkit.Color;
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
import org.jetbrains.annotations.NotNull;

public class SocialCmd implements CommandExecutor, Listener {

    private Inventory inv;
    public static String invname = "§6§lSocial";

    public static final String discord_value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQu" +
            "bmV0L3RleHR1cmUvYTNiMTgzYjE0OGI5YjRlMmIxNTgzMzRhZmYzYjViYjZjMmMyZGJiYzRkNjdmNzZhN2JlODU2Njg3YTJiNjIzIn19fQ==";

    public static final String insta_value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTBkNDY0MTg2ZTFhNTBkZGFhMT" +
            "RiZTIyNTk2MTFhNGU4NDU4NTE1YTUzNjdhOTM4OWE5Y2M3Yzg5Yzk0YTkzYiJ9fX0=";

    public static final String tiktok_value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3Jh" +
            "ZnQubmV0L3RleHR1cmUvZWYyZjZjNTEzYWMyNjk2MDE5ZmUzYjY2ZGRiNmNkZDFlNzc5ZWM3YzUyMGJkMDU4ZDYwNmU4YmJlN2ExMTkxZCJ9fX0=";

    public static final String twitter_value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQu" +
            "bmV0L3RleHR1cmUvNmFkNDZhNDIyYWU1OTYwM2ZkODg5YzI1MzQ0ZmY2N2JjODQzYWY4ZWU1MTg5MzJjMmUyYWQwN2NkYmY5MzliMyJ9fX0=";

    public static final String youtube_value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubm" +
            "V0L3RleHR1cmUvYmI3Njg4ZGE0NjU4NmI4NTlhMWNkZTQwY2FlMWNkYmMxNWFiZTM1NjE1YzRiYzUyOTZmYWQwOTM5NDEwNWQwIn19fQ==";

    public static final String teamspeak_value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQub" +
            "mV0L3RleHR1cmUvOWQ1MmYwYTZmNDNlZjdhMTU2NGViZmEyMGFkOGM4ZTdmNjk0YTFmMjZjMmJhZTMwMzc2ZGJiM2NhMzE2MzZlYiJ9fX0=";

    public static final String money_value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleH" +
            "R1cmUvZDE3N2E5NGUyYmQwOGJiZDgzOTQ0NjU4YjM0MjAyYjY5YWMzNmMyOGE4NmQyOThjZmNjMDQ0ZWYyOGIzMDlkYiJ9fX0=";
    
    public static final String twitch_value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3R" +
            "leHR1cmUvZWQyYjA1YWJlNDk3NmMwYWYxZjU0YjhkMzRlNjU2ZDE0MzgzMTc0ZmY1YzZlYmE2MTNhOGE4NDA1NDk4Y2IxNSJ9fX0=";

    public void createInv(Player player){
        inv = Bukkit.createInventory(null, 9*5, invname);
        ItemStack discord = Util.createCustomSkull(discord_value, "§6§lDiscord", Lore.create(
                "§7Hier ist unser §6Discord:",
                "§bhttps://discord.gg/hNGSNppdDD"
        ));
        ItemStack insta = Util.createCustomSkull(insta_value, "§a§lInstgram", Lore.create(
                "§7Hier ist unser §aInstagram:",
                "§bhttps://www.instagram.com/wonderbuild_net/?next=%2F"
        ));
        ItemStack tiktok = Util.createCustomSkull(tiktok_value, "§c§lTikTok", Lore.create(
                "§7Hier ist unser §cTikTok:",
                "§bhttps://www.tiktok.com/@wonderbuild.net"
        ));
        ItemStack twitter = Util.createCustomSkull(twitter_value, "§9§lTwitter", Lore.create(
                "§7Hier ist unser §9Twitter:",
                "§bhttps://twitter.com/wonderbuild_net"
        ));
        ItemStack youtube = Util.createCustomSkull(youtube_value, "§5§lYoutube", Lore.create(
                "§7Hier ist unser §5YouTube:",
                "§bhttps://www.youtube.com/watch?v=u0Hh4uspJsM"
        ));
        ItemStack teamspeak = Util.createCustomSkull(teamspeak_value, "§2§lTeamSpeak", Lore.create(
                "§7Hier ist unser §2Teamspeak:",
                "§bwonderbuild.net"
        ));
        ItemStack money = Util.createCustomSkull(money_value, "§e§lStore", Lore.create(
                "§7Hier ist unser §eStore:",
                "§bhttps://store.wonderbuild.net"
        ));
        ItemStack twitch = Util.createCustomSkull(twitch_value, "§b§lTwitch", Lore.create(
                "§7Hier ist unser §bTwitch:",
                "§bhttps://twitch.com/wonderbuild"
        ));

        inv.setItem(10, discord);
        inv.setItem(12, insta);
        inv.setItem(14,tiktok);
        inv.setItem(16, twitter);
        inv.setItem(28, youtube);
        inv.setItem(30, teamspeak);
        inv.setItem(32, money);
        inv.setItem(34, twitch);

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
        player.sendMessage(Strings.prefix + "§7Benutzung: /social");
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
