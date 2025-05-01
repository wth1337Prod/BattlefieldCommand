package org.comm.battlefieldCommand.game;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.player.BCPlayer;
import org.comm.battlefieldCommand.game.team.TeamType;
import org.comm.battlefieldCommand.utils.MessageUtils;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.comm.battlefieldCommand.game.arena.ArenaSetupManager;

import java.util.UUID;

/**
 * Класс слушателя для обработки игровых событий Battlefield Command
 */
public class GameListener implements Listener {
    private final BattlefieldCommand plugin;
    private final GameManager gameManager;
    private final ArenaSetupManager arenaSetupManager;
    
    
    
    private boolean waitingForFlagInfo = false;
    
    
    private boolean waitingForMapGenerationInfo = false;
    
    public GameListener(BattlefieldCommand plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.arenaSetupManager = gameManager.getArenaSetupManager();
    }
    
    /**
     * Обрабатывает событие входа игрока на сервер
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        
        player.sendMessage(ChatColor.GREEN + "Добро пожаловать на Battlefield Command!");
        player.sendMessage(ChatColor.YELLOW + "Используйте /bc join для входа в игру");
        
        
        if (gameManager.isGameActive()) {
            player.sendMessage(ChatColor.GOLD + "Игра уже идёт! Присоединяйтесь!");
        }
    }
    
    /**
     * Обрабатывает событие выхода игрока с сервера
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        
        if (gameManager.isPlayerInGame(player)) {
            gameManager.leaveGame(player);
        }
    }
    
    /**
     * Обрабатывает событие смерти игрока
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        
        
        event.setDeathMessage(null);
        
        
        if (!gameManager.isPlayerInGame(victim)) {
            return;
        }
        
        
        Player killer = victim.getKiller();
        BCPlayer victimBCPlayer = gameManager.getPlayer(victim.getUniqueId());
        
        if (killer != null && gameManager.isPlayerInGame(killer)) {
            
            BCPlayer killerBCPlayer = gameManager.getPlayer(killer.getUniqueId());
            
            
            handlePlayerKill(killer, victim, killerBCPlayer, victimBCPlayer);
        } else {
            
            MessageUtils.broadcastToPlayers(
                    plugin.getServer().getOnlinePlayers(),
                    ChatColor.RED + victim.getName() + " погиб");
            
            
            if (victimBCPlayer != null) {
                victimBCPlayer.addDeath();
            }
        }
        
        
        gameManager.schedulePlayerRespawn(victim);
    }
    
    /**
     * Обрабатывает событие респауна игрока
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        
        
        player.setGameMode(GameMode.SPECTATOR);
        
        
        TeamType team = gameManager.getPlayerTeam(player);
        if (team != null) {
            event.setRespawnLocation(gameManager.getTeamSpawnLocation(team));
        }
    }
    
    /**
     * Обрабатывает урон между игроками
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        
        
        if (!gameManager.isPlayerInGame(victim)) {
            return;
        }
        
        
        Entity damager = event.getDamager();
        Player attacker = null;
        
        
        if (damager instanceof Player) {
            attacker = (Player) damager;
        } else if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player) {
                attacker = (Player) source;
            }
        }
        
        
        if (attacker != null && gameManager.isPlayerInGame(attacker)) {
            
            TeamType attackerTeam = gameManager.getPlayerTeam(attacker);
            TeamType victimTeam = gameManager.getPlayerTeam(victim);
            
            
            if (attackerTeam == victimTeam && !plugin.getConfigManager().getMainConfig().getBoolean("game.friendly_fire", false)) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "Огонь по своим запрещен!");
            }
        }
    }
    
    /**
     * Обрабатывает движение игрока для захвата флагов
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        
        if (!gameManager.isGameActive() || !gameManager.isPlayerInGame(event.getPlayer())) {
            return;
        }
        
        
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && 
            event.getFrom().getBlockY() == event.getTo().getBlockY() && 
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        
        
        gameManager.checkPlayerInCaptureZone(event.getPlayer(), event.getTo());
    }
    
    /**
     * Обработка убийства игрока другим игроком
     */
    private void handlePlayerKill(Player killer, Player victim, BCPlayer killerBCPlayer, BCPlayer victimBCPlayer) {
        
        killerBCPlayer.addKill();
        
        
        victimBCPlayer.addDeath();
        
        
        int killPoints = plugin.getConfigManager().getMainConfig().getInt("game.kill_points", 10);
        gameManager.getTeamManager().addPoints(killer.getUniqueId(), killPoints, "убийство противника");
        
        
        int killStreak = killerBCPlayer.getKillStreak();
        if (killStreak == 3 || killStreak == 5 || killStreak == 7) {
            
            gameManager.giveKillstreakReward(killer, killStreak);
        }
        
        
        String message = killerBCPlayer.getTeamType().getChatColor() + killer.getName() + 
                         ChatColor.WHITE + " убил " + 
                         victimBCPlayer.getTeamType().getChatColor() + victim.getName();
        
        MessageUtils.broadcastToPlayers(plugin.getServer().getOnlinePlayers(), message);
    }

    /**
     * Обрабатывает чат для ввода дополнительной информации
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        
        if (arenaSetupManager.isPlayerInSetupMode(player) && waitingForFlagInfo) {
            waitingForFlagInfo = false;
            event.setCancelled(true);
            
            
            boolean success = arenaSetupManager.handleFlagInfoMessage(player, message);
            
            if (!success) {
                waitingForFlagInfo = true; 
            }
        }
        
        
        if (arenaSetupManager.isPlayerInSetupMode(player) && waitingForMapGenerationInfo) {
            waitingForMapGenerationInfo = false;
            event.setCancelled(true);
            
            
            boolean success = arenaSetupManager.handleMapGenerationMessage(player, message);
            
            if (!success) {
                waitingForMapGenerationInfo = true; 
            }
        }
    }

    /**
     * Обрабатывает использование предметов для настройки арены
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        
        if (!arenaSetupManager.isPlayerInSetupMode(player)) {
            return;
        }
        
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            ItemStack item = event.getItem();
            boolean handled = arenaSetupManager.handleBlockClick(player, event.getClickedBlock().getLocation(), item);
            
            if (handled) {
                event.setCancelled(true);
                
                
                if (item != null && item.hasItemMeta() && 
                    item.getItemMeta().getDisplayName().contains("Контрольная точка")) {
                    waitingForFlagInfo = true;
                }
                
                
                if (item != null && item.hasItemMeta() && 
                    item.getItemMeta().getDisplayName().contains("Сгенерировать арену")) {
                    waitingForMapGenerationInfo = true;
                }
            }
        }
    }
} 