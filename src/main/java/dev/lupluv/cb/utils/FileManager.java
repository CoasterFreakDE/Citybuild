package dev.lupluv.cb.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FileManager {

    public FileManager() throws IOException {
        loadFiles();
    }

    public void loadFiles() throws IOException {
        File folder = new File("plugins//Citybuild");
        File warpsFolder = new File("plugins//Citybuild//Warps");
        File configFile = new File("plugins//Citybuild//config.yml");
        File moneyFile = new File("plugins//Citybuild//money.yml");
        File homesFile = new File("plugins//Citybuild//homes.yml");
        File mysqlFile = new File("plugins//Citybuild//mysql.yml");
        File chestShopFile = new File("plugins//Citybuild//chestshops.yml");
        File voteFile = new File("plugins//Citybuild//votes.yml");
        File elevatorFile = new File("plugins//Citybuild//elevators.yml");
        File randFile = new File("plugins//Citybuild//rand.yml");
        File statsFile = new File("plugins//Citybuild//stats.yml");
        if(!folder.exists()) folder.mkdir();
        if(!warpsFolder.exists()) warpsFolder.mkdir();
        if(!configFile.exists()){
            configFile.createNewFile();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
            cfg.set("Licence", "wonderbuild47298734validd892374923");
            cfg.save(configFile);
        }
        if(!moneyFile.exists()){
            moneyFile.createNewFile();
        }
        if(!homesFile.exists()){
            homesFile.createNewFile();
        }
        if(!mysqlFile.exists()){
            mysqlFile.createNewFile();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(mysqlFile);
            cfg.set("Host", "localhost");
            cfg.set("Port", "3306");
            cfg.set("Database", "database");
            cfg.set("Username", "username");
            cfg.set("Password", "password");
            cfg.save(mysqlFile);
        }
        if(!chestShopFile.exists()){
            chestShopFile.createNewFile();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(chestShopFile);
        }
        if(!voteFile.exists()){
            voteFile.createNewFile();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(voteFile);
        }
        if(!elevatorFile.exists()){
            elevatorFile.createNewFile();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(elevatorFile);
        }
        if(!randFile.exists()){
            randFile.createNewFile();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(randFile);
        }
        if(!statsFile.exists()){
            statsFile.createNewFile();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(statsFile);
        }
    }

    public File getMysqlFile(){
        return new File("plugins//Citybuild//mysql.yml");
    }

    public FileConfiguration getMysql(){
        return YamlConfiguration.loadConfiguration(getMysqlFile());
    }

    public String getLicence(){
        File configFile = new File("plugins//Citybuild//config.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        return cfg.getString("Licence");
    }

}
