package org.comm.battlefieldCommand.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.GameManager;
import org.comm.battlefieldCommand.game.killstreak.KillstreakItems;
import org.comm.battlefieldCommand.game.killstreak.KillstreakManager;
import org.comm.battlefieldCommand.game.player.BCPlayer;

/**
 * Слушатель событий для системы киллстриков
 */
public class KillstreakListener implements Listener {
    private final BattlefieldCommand plugin;
    private final GameManager gameManager;
    private final KillstreakManager killstreakManager;
    
    public KillstreakListener(BattlefieldCommand plugin, KillstreakManager killstreakManager) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.killstreakManager = killstreakManager;
    }
    
    /**
     * Обрабатывает убийство игрока для засчитывания киллстрика
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        
        if (killer == null || killer.equals(victim)) {
            
            killstreakManager.resetKillstreak(victim);
            return;
        }
        
        
        if (!gameManager.isPlayerInGame(victim) || !gameManager.isPlayerInGame(killer)) {
            return;
        }
        
        
        BCPlayer victimPlayer = gameManager.getPlayer(victim.getUniqueId());
        BCPlayer killerPlayer = gameManager.getPlayer(killer.getUniqueId());
        
        if (victimPlayer != null && killerPlayer != null) {
            
            victimPlayer.incrementDeaths();
            killerPlayer.incrementKills();
            
            
            killstreakManager.resetKillstreak(victim);
            
            
            killstreakManager.incrementKillstreak(killer, killerPlayer);
        }
    }
    
    /**
     * Обрабатывает использование предмета киллстрика
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        
        
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        
        
        if (KillstreakItems.isAirStrikeMarker(item)) {
            
            handleAirStrikeMarker(player, event);
        }
    }
    
    /**
     * Обрабатывает использование маркера авиаудара
     */
    private void handleAirStrikeMarker(Player player, PlayerInteractEvent event) {
        
        event.setCancelled(true);
        
        
        if (event.getClickedBlock() != null) {
            Location targetLocation = event.getClickedBlock().getLocation().add(0, 1, 0);
            
            
            killstreakManager.callAirStrike(player, targetLocation);
            
            
            ItemStack item = event.getItem();
            if (item != null) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Целевой блок не найден. Наведитесь на блок для вызова авиаудара.");
        }
    }
    
    /**
     * Отслеживает урон для подсчета киллстриков (для случаев, когда PlayerDeathEvent не срабатывает)
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = null;
        
        
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }
        
        
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        
        
        if (victim.getHealth() - event.getFinalDamage() <= 0) {
            
            
            
            
            if (!gameManager.isPlayerInGame(victim) || !gameManager.isPlayerInGame(attacker)) {
                return;
            }
            
            
            BCPlayer victimPlayer = gameManager.getPlayer(victim.getUniqueId());
            BCPlayer attackerPlayer = gameManager.getPlayer(attacker.getUniqueId());
            
            if (victimPlayer != null && attackerPlayer != null) {
                
                
                victimPlayer.incrementDeaths();
                attackerPlayer.incrementKills();
                
                
                killstreakManager.resetKillstreak(victim);
                
                
                killstreakManager.incrementKillstreak(attacker, attackerPlayer);
            }
        }
    }
} 