package dev.lupluv.cb;

import dev.lupluv.cb.clans.Clan;
import dev.lupluv.cb.commands.*;
import dev.lupluv.cb.economy.Economy;
import dev.lupluv.cb.events.*;
import dev.lupluv.cb.licence.LicenceManager;
import dev.lupluv.cb.mysql.MySQL;
import dev.lupluv.cb.stats.StatsNPC;
import dev.lupluv.cb.utils.*;
import dev.lupluv.cb.voting.VoteListener;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class Citybuild extends JavaPlugin {

    private static Citybuild plugin;
    private static FileManager fileManager;
    private static MoneyManager moneyManager;
    private static InventoryManager inventoryManager;
    private static Crafting crafting;
    private static StatsNPC statsNPC;
    public static MySQL mySQL;

    @Override
    public void onLoad() {
        plugin = this;

        // Files

        try {
            fileManager = new FileManager();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // MySQL Con
        reloadMysql();

    }

    @Override
    public void onEnable() {


        if(getServer().getPluginManager().isPluginEnabled("Vault")) {
            try {
                Plugin vault = getServer().getPluginManager().getPlugin("Vault");
                plugin.getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, new EconomyHolder(), vault, ServicePriority.High);
                plugin.getLogger().log(Level.INFO, "Hooking into Vault");
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Exception occurred while hooking into vault", exception);
            }
        }

        if(!getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getLogger().log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        LicenceManager.getInstance().doOnEnable();

        // Money

        moneyManager = new MoneyManager();
        inventoryManager = new InventoryManager();
        crafting = new Crafting();
        crafting.loadRecipes();


        // Commands

        getCommand("setwarp").setExecutor(new SetwarpCmd());
        getCommand("delwarp").setExecutor(new DelwarpCmd());
        getCommand("warp").setExecutor(new WarpCmd());
        getCommand("warps").setExecutor(new WarpsCmd());
        getCommand("spawn").setExecutor(new SpawnCmd());
        getCommand("money").setExecutor(new MoneyCmd());
        getCommand("pay").setExecutor(new PayCmd());
        getCommand("sethome").setExecutor(new SethomeCmd());
        getCommand("delhome").setExecutor(new DelhomeCmd());
        getCommand("home").setExecutor(new HomeCmd());
        getCommand("homes").setExecutor(new HomesCmd());
        getCommand("fly").setExecutor(new FlyCmd());
        getCommand("tpa").setExecutor(new TpaCmd());
        getCommand("tpahere").setExecutor(new TpahereCmd());
        getCommand("tpaccept").setExecutor(new TpacceptCmd());
        getCommand("tpdeny").setExecutor(new TpdenyCmd());
        getCommand("tpo").setExecutor(new TpoCmd());
        getCommand("tpohere").setExecutor(new TpohereCmd());
        getCommand("farmwelt").setExecutor(new FarmweltCmd());
        getCommand("shop").setExecutor(new ShopCmd());
        getCommand("gamemode").setExecutor(new GamemodeCmd());
        getCommand("cc").setExecutor(new ChatclearCmd());
        getCommand("ec").setExecutor(new EnderchestCmd());
        getCommand("feed").setExecutor(new FeedCmd());
        getCommand("heal").setExecutor(new HealCmd());
        getCommand("v").setExecutor(new VanishCmd());
        getCommand("myplot").setExecutor(new PlotsCmd());
        getCommand("vote").setExecutor(new VoteCmd());
        getCommand("event").setExecutor(new EventCmd());
        getCommand("startkick").setExecutor(new StartkickCmd());
        getCommand("ja").setExecutor(new JaCmd());
        getCommand("nein").setExecutor(new NeinCmd());
        getCommand("setstats").setExecutor(new SetstatsCmd());
        getCommand("msg").setExecutor(new MsgCmd());
        getCommand("reply").setExecutor(new ReplyCmd());
        getCommand("sign").setExecutor(new SignCmd());
        getCommand("invsee").setExecutor(new InvseeCmd());
        getCommand("payall").setExecutor(new PayallCmd());
        getCommand("clan").setExecutor(new ClanCmd());
        getCommand("adminshop").setExecutor(new AdminshopCmd());

        // Events

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerHandler(), this);
        pm.registerEvents(new ClickHandler(), this);
        pm.registerEvents(new VoteListener(), this);

        statsNPC = new StatsNPC();

        Bukkit.getScheduler().runTaskLater(this, Citybuild::startNPCScheduler, 20*10);

    }

    @Override
    public void onDisable() {

    }

    public static Citybuild getPlugin() {
        return plugin;
    }

    public static FileManager getFileManager() {
        return fileManager;
    }

    public static MoneyManager getMoneyManager() {
        return moneyManager;
    }

    public static InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public static Crafting getCrafting() {
        return crafting;
    }

    public static MySQL getMySQL() {
        return mySQL;
    }

    public static void reloadMysql(){
        if(getFileManager().getMysqlFile().exists()){
            FileConfiguration cfg = getFileManager().getMysql();
            if(mySQL != null){
                mySQL.disconnect();
            }
            mySQL = new MySQL(cfg.getString("Host"), cfg.getString("Port"), cfg.getString("Database"), cfg.getString("Username"), cfg.getString("Password"));
            mySQL.connect();
            System.out.println("Trying to connect");
            if(mySQL.isConnected()){
                System.out.println("Successfull");
                mySQL.update("CREATE TABLE IF NOT EXISTS cb_economy (uuid VARCHAR(255),name VARCHAR(255),money DOUBLE(255,20));");
                mySQL.update("CREATE TABLE IF NOT EXISTS cb_economy_banks (name VARCHAR(255),money DOUBLE(255,20));");
                mySQL.update("CREATE TABLE IF NOT EXISTS cb_clans_clan (id BIGINT(255),name VARCHAR(255),tag VARCHAR(255),color VARCHAR(255)" +
                        ",open VARCHAR(255),chat VARCHAR(255),date BIGINT(255));");
                mySQL.update("CREATE TABLE IF NOT EXISTS cb_clans_user (id BIGINT(255),uuid VARCHAR(255),name VARCHAR(255));");
                mySQL.update("CREATE TABLE IF NOT EXISTS cb_clans_member (user_id BIGINT(255),clan_id BIGINT(255),role VARCHAR(255));");
                mySQL.update("CREATE TABLE IF NOT EXISTS cb_clans_requests (user_id BIGINT(255),clan_id BIGINT(255),type VARCHAR(255),date BIGINT(255));");
            }
        }
    }

    public static void sendColoredConsoleMessage(String msg){
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    public static void replaceNPCS(){
        System.out.println("Replace NPCs");
        if(statsNPC == null) statsNPC = new StatsNPC();
        updateNpcs();
        Bukkit.broadcast(Strings.prefix + "§7Alle Stats NPCs wurden §aerfolgreich §7aktualisiert.", "cb.stats.set");
    }

    public static void startNPCScheduler(){
        System.out.println("Started Scheduler");
        Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), Citybuild::replaceNPCS, 0, 20*60);
    }

    public static void updateNpcs(){
        System.out.println("Place NPCs");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(new File("plugins//Citybuild//stats.yml"));

        dev.lupluv.cb.economy.Economy eco1 = dev.lupluv.cb.economy.Economy.getRank(1);
        dev.lupluv.cb.economy.Economy eco2 = dev.lupluv.cb.economy.Economy.getRank(2);
        dev.lupluv.cb.economy.Economy eco3 = dev.lupluv.cb.economy.Economy.getRank(3);

        System.out.println("UUID1: " + eco1.getPlayerName());
        System.out.println("UUID2: " + eco2.getPlayerName());
        System.out.println("UUID3: " + eco3.getPlayerName());

        // NPC 1
        if(cfg.getString("NPC.0.World") != null){
            Location loc = Util.getLocation(cfg, "NPC.0");
            if(eco1 != null) {
                statsNPC.updateNpc(1, "§6§lPlatz 1 §8| §e§l" + eco1.getPlayerName() + " §8| §6§l" + eco1.getMoney() + " wc", eco1.getPlayerName());
                System.out.println("Appending 1");
            }
        }

        // NPC 2
        if(cfg.getString("NPC.1.World") != null){
            Location loc = Util.getLocation(cfg, "NPC.1");
            if(eco2 != null) {
                statsNPC.updateNpc(2, "§6§lPlatz 2 §8| §e§l" + eco2.getPlayerName() + " §8| §6§l" + eco2.getMoney() + " wc", eco2.getPlayerName());
                System.out.println("Appending 2");
            }
        }

        // NPC 3
        if(cfg.getString("NPC.2.World") != null){
            Location loc = Util.getLocation(cfg, "NPC.2");
            if(eco3 != null) {
                statsNPC.updateNpc(3, "§6§lPlatz 3 §8| §e§l" + eco3.getPlayerName() + " §8| §6§l" + eco3.getMoney() + " wc", eco3.getPlayerName());
                System.out.println("Appending 3");
            }
        }
    }

}
