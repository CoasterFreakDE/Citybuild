package dev.lupluv.cb.particels;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.ArrayList;

public class Particel implements Listener {

    public static ArrayList<Player> haseffect1 = new ArrayList<Player>();
    public static ArrayList<Player> haseffect2 = new ArrayList<Player>();
    public static ArrayList<Player> haseffect3 = new ArrayList<Player>();
    public static ArrayList<Player> haseffect4 = new ArrayList<Player>();
    public static ArrayList<Player> haseffect5 = new ArrayList<Player>();
    public static ArrayList<Player> haseffect6 = new ArrayList<Player>();
    public static ArrayList<Player> haseffect7 = new ArrayList<Player>();
    public static ArrayList<Player> haseffect8 = new ArrayList<Player>();
    public static ArrayList<Player> haseffect9 = new ArrayList<Player>();

   @EventHandler
   public void onMove(PlayerMoveEvent event){


       Player player = event.getPlayer();
if(haseffect2.contains(player))
       if(haseffect1.contains(player)){
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation(), 2, 0.3, 0.3,0.3);
       }


   }

}
