package dev.lupluv.cb.scorebaord;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import dev.lupluv.cb.Citybuild;
import dev.lupluv.cb.economy.Economy;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
//import net.melion.rgbchat.api.RGBApi;
import net.melion.rgbchat.api.RGBApi;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreboardManager {
    private static ScoreboardManager instance;
    public static ScoreboardManager getInstance() {
        if(instance == null){
            instance = new ScoreboardManager();
        }
        return instance;
    }

    private static final Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");

    public static String format(String s){
        Matcher match = pattern.matcher(s);
        while(match.find()){
            String color = s.substring(match.start(), match.end());
            s = s.replace(color, net.md_5.bungee.api.ChatColor.of(color) + "");
            match = pattern.matcher(s);
        }
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String format2(String s){
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', RGBApi.INSTANCE.toColoredMessage(s));
    }

    public static void updateScoreboard(Player p){
        IPlayerManager pm = BridgePlayerManager.getInstance();
        ICloudPlayer cp = pm.getOnlinePlayer(p.getUniqueId());
        IPermissionUser u = CloudNetDriver.getInstance().getPermissionManagement().getUser(p.getUniqueId());
        String serverName = cp.getConnectedService().getServerName();
        Scoreboard scoreboard = p.getScoreboard();
        Team profile;
        Team server;
        Team coins;
        Team online;
        if(scoreboard.getTeam("profile") != null) profile = scoreboard.getTeam("profile"); else profile = scoreboard.registerNewTeam("profile");
        if(scoreboard.getTeam("server") != null) server = scoreboard.getTeam("server"); else server = scoreboard.registerNewTeam("server");
        if(scoreboard.getTeam("coins") != null) coins = scoreboard.getTeam("coins"); else coins = scoreboard.registerNewTeam("coins");
        if(scoreboard.getTeam("online") != null) online = scoreboard.getTeam("online"); else online = scoreboard.registerNewTeam("online");

        profile.setPrefix("§e" + format2(getPrefix(p) + getColor(p) + p.getName()));
        server.setPrefix("§b" + serverName);
        coins.setPrefix("§e" + Economy.getBalance(p.getUniqueId()) + " §6❂");
        online.setPrefix("§a" + Bukkit.getOnlinePlayers().size());
    }

    public static String getPrefix(Player player){
        return CloudNetDriver.getInstance().getPermissionManagement()
                .getHighestPermissionGroup(
                        CloudNetDriver.getInstance().getPermissionManagement()
                                .getUser(
                                        player.getUniqueId()
                                )
                )
                .getPrefix();
    }

    public static String getColor(Player player){
        return CloudNetDriver.getInstance().getPermissionManagement()
                .getHighestPermissionGroup(
                        CloudNetDriver.getInstance().getPermissionManagement()
                                .getUser(
                                        player.getUniqueId()
                                )
                )
                .getColor();
    }

    public void startScoreboardTask(){
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Citybuild.getPlugin(), new Runnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(all ->{
                    updateScoreboard(all);
                });
            }
        }, 0, 20);
    }

    public void updateNameTags(Player player) {
        this.updateNameTags(player, null);
    }

    public void updateNameTags(Player player, Function<Player, IPermissionGroup> playerIPermissionGroupFunction) {
        this.updateNameTags(player, playerIPermissionGroupFunction, null);
    }

    public void updateNameTags(Player player, Function<Player, IPermissionGroup> playerIPermissionGroupFunction,
                               Function<Player, IPermissionGroup> allOtherPlayerPermissionGroupFunction) {
        Preconditions.checkNotNull(player);

        IPermissionUser playerPermissionUser = CloudNetDriver.getInstance().getPermissionManagement()
                .getUser(player.getUniqueId());
        AtomicReference<IPermissionGroup> playerPermissionGroup = new AtomicReference<>(
                playerIPermissionGroupFunction != null ? playerIPermissionGroupFunction.apply(player) : null);

        if (playerPermissionUser != null && playerPermissionGroup.get() == null) {
            playerPermissionGroup
                    .set(CloudNetDriver.getInstance().getPermissionManagement().getHighestPermissionGroup(playerPermissionUser));

            if (playerPermissionGroup.get() == null) {
                playerPermissionGroup.set(CloudNetDriver.getInstance().getPermissionManagement().getDefaultPermissionGroup());
            }
        }

        int sortIdLength = CloudNetDriver.getInstance().getPermissionManagement().getGroups().stream()
                .map(IPermissionGroup::getSortId)
                .map(String::valueOf)
                .mapToInt(String::length)
                .max()
                .orElse(0);

        this.initScoreboard(player);

        Bukkit.getOnlinePlayers().forEach(all -> {
            this.initScoreboard(all);

            if (playerPermissionGroup.get() != null) {
                this.addTeamEntry(player, all, playerPermissionGroup.get(), sortIdLength);
            }

            IPermissionUser targetPermissionUser = CloudNetDriver.getInstance().getPermissionManagement()
                    .getUser(all.getUniqueId());
            IPermissionGroup targetPermissionGroup =
                    allOtherPlayerPermissionGroupFunction != null ? allOtherPlayerPermissionGroupFunction.apply(all) : null;

            if (targetPermissionUser != null && targetPermissionGroup == null) {
                targetPermissionGroup = CloudNetDriver.getInstance().getPermissionManagement()
                        .getHighestPermissionGroup(targetPermissionUser);

                if (targetPermissionGroup == null) {
                    targetPermissionGroup = CloudNetDriver.getInstance().getPermissionManagement().getDefaultPermissionGroup();
                }
            }

            if (targetPermissionGroup != null) {
                this.addTeamEntry(all, player, targetPermissionGroup, sortIdLength);
            }
        });
    }

    private void addTeamEntry(Player target, Player all, IPermissionGroup permissionGroup, int highestSortIdLength) {
        int sortIdLength = String.valueOf(permissionGroup.getSortId()).length();
        String teamName = (
                highestSortIdLength == sortIdLength ?
                        permissionGroup.getSortId() :
                        String.format("%0" + highestSortIdLength + "d", permissionGroup.getSortId())
        ) + permissionGroup.getName();

        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = all.getScoreboard().getTeam(teamName);
        if (team == null) {
            team = all.getScoreboard().registerNewTeam(teamName);
        }

        String prefix = format2(permissionGroup.getPrefix());
        String color = permissionGroup.getColor();
        String suffix = permissionGroup.getSuffix().replaceAll("&", "§");

        try {
            Method method = team.getClass().getDeclaredMethod("setColor", ChatColor.class);
            method.setAccessible(true);

            if (color != null && !color.isEmpty()) {
                ChatColor chatColor = ChatColor.getByChar(color.replaceAll("&", "").replaceAll("§", ""));
                if (chatColor != null) {
                    method.invoke(team, chatColor);
                }
            } else {
                color = ChatColor.getLastColors(prefix.replace('&', '§'));
                if (!color.isEmpty()) {
                    ChatColor chatColor = ChatColor.getByChar(color.replaceAll("&", "").replaceAll("§", ""));
                    if (chatColor != null) {
                        permissionGroup.setColor(color);
                        CloudNetDriver.getInstance().getPermissionManagement().updateGroup(permissionGroup);
                        method.invoke(team, chatColor);
                    }
                }
            }
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException | InvocationTargetException exception) {
            exception.printStackTrace();
        }

        team.setPrefix(prefix);

        team.setSuffix(suffix);

        team.addEntry(target.getName());

        target.setDisplayName(permissionGroup.getDisplay() + target.getName());
    }

    private void initScoreboard(Player all) {
        if (all.getScoreboard().equals(all.getServer().getScoreboardManager().getMainScoreboard())) {
            Scoreboard scoreboard = all.getServer().getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("citybuild", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName("§6§lWONDERBUILD.NET");
            Team profile;
            Team server;
            Team coins;
            Team online;
            if(scoreboard.getTeam("profile") != null) profile = scoreboard.getTeam("profile"); else profile = scoreboard.registerNewTeam("profile");
            if(scoreboard.getTeam("server") != null) server = scoreboard.getTeam("server"); else server = scoreboard.registerNewTeam("server");
            if(scoreboard.getTeam("coins") != null) coins = scoreboard.getTeam("coins"); else coins = scoreboard.registerNewTeam("coins");
            if(scoreboard.getTeam("online") != null) online = scoreboard.getTeam("online"); else online = scoreboard.registerNewTeam("online");
            objective.getScore(" ").setScore(11);
            objective.getScore("§fProfil§7:").setScore(10);
            objective.getScore("§1").setScore(9);
            objective.getScore("  ").setScore(8);
            objective.getScore("§fServer§7:").setScore(7);
            objective.getScore("§3").setScore(6);
            objective.getScore("    ").setScore(5);
            objective.getScore("§fBargeld§7:").setScore(4);
            objective.getScore("§4").setScore(3);
            objective.getScore("     ").setScore(2);
            objective.getScore("§fOnline§7:").setScore(1);
            objective.getScore("§5").setScore(0);
            profile.addEntry("§1");
            server.addEntry("§3");
            coins.addEntry("§4");
            online.addEntry("§5");

            all.setScoreboard(scoreboard);
        }
    }

}
