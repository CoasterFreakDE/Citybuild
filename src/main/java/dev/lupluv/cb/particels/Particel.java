package dev.lupluv.cb.particels;

import dev.lupluv.cb.Citybuild;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

public class Particel implements Listener {
    
   @EventHandler
   public void onMove(PlayerMoveEvent event){
       Player player = event.getPlayer();
       NamespacedKey key = new NamespacedKey(Citybuild.getPlugin(), "effect");
       if(!player.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;
       byte effect = player.getPersistentDataContainer().get(key, PersistentDataType.BYTE);

       switch (effect) {
           case (byte) 1 -> player.getWorld().spawnParticle(Particle.HEART, player.getLocation(), 2, 0.3, 0.3, 0.3);
           case (byte) 2 -> player.getWorld().spawnParticle(Particle.NOTE, player.getLocation(), 2, 0.3, 0.3, 0.3);
           case (byte) 3 ->
                   player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation(), 2, 0.3, 0.3, 0.3);
           case (byte) 4 ->
                   player.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, player.getLocation(), 2, 0.3, 0.3, 0.3);
           case (byte) 5 -> player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 2, 0.3, 0.3, 0.3);
           case (byte) 6 ->
                   player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 2, 0.3, 0.3, 0.3);
           case (byte) 7 ->
                   player.getWorld().spawnParticle(Particle.FALLING_LAVA, player.getLocation(), 2, 0.3, 0.3, 0.3);
           case (byte) 8 ->
                   player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 2, 0.3, 0.3, 0.3);
           case (byte) 9 -> player.getWorld().spawnParticle(Particle.SPELL_MOB, player.getLocation(), 2, 0.3, 0.3, 0.3);
       }
   }
}
