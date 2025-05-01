package org.comm.battlefieldCommand.listeners.player;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.GameManager;
import org.comm.battlefieldCommand.game.arena.Arena;
import org.comm.battlefieldCommand.game.arena.ArenaSetupManager;
import org.comm.battlefieldCommand.game.arena.capture.CaptureFlag;
import org.comm.battlefieldCommand.game.player.BCPlayer;
import org.comm.battlefieldCommand.game.team.TeamType;
import org.comm.battlefieldCommand.utils.MessageUtils;

/**
 * Обрабатывает события, связанные с игроками
 */
public class PlayerListener implements Listener {
    private final BattlefieldCommand plugin;
    private final GameManager gameManager;
    
    public PlayerListener(BattlefieldCommand plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }
    
    /**
     * Обрабатывает событие входа игрока на сервер
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        
        if (gameManager.isGameActive()) {
            MessageUtils.sendInfoMessage(player, "На сервере идет игра Battlefield Command!");
            MessageUtils.sendInfoMessage(player, "Используйте /bc join чтобы присоединиться.");
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
        
        
        ArenaSetupManager setupManager = gameManager.getArenaSetupManager();
        if (setupManager != null && setupManager.isPlayerInSetupMode(player)) {
            setupManager.stopSetup(player);
        }
    }
    
    /**
     * Обрабатывает событие смерти игрока
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        
        
        BCPlayer bcPlayer = gameManager.getPlayer(player.getUniqueId());
        if (bcPlayer == null) {
            return;
        }
        
        
        event.setDeathMessage(null);
        
        
        event.setKeepInventory(true);
        event.getDrops().clear();
        
        
        bcPlayer.addDeath();
        
        
        Player killer = player.getKiller();
        
        if (killer != null && gameManager.isPlayerInGame(killer)) {
            
            BCPlayer killerBCPlayer = gameManager.getPlayer(killer.getUniqueId());
            
            if (killerBCPlayer != null) {
                
                killerBCPlayer.addKill();
                
                
                gameManager.getTeamManager().addPoints(killerBCPlayer.getTeamType(), 5);
                
                
                MessageUtils.broadcastMessage(
                        killerBCPlayer.getTeamType().getChatColor() + killer.getName() + 
                        ChatColor.YELLOW + " убил " + 
                        bcPlayer.getTeamType().getChatColor() + player.getName());
                
                
                MessageUtils.sendActionBar(killer, 
                        ChatColor.GREEN + "+1 убийство | " + 
                        ChatColor.GOLD + "+5 очков команде | " + 
                        ChatColor.AQUA + "Серия: " + killerBCPlayer.getKillStreak());
                
                
                checkKillstreak(killer, killerBCPlayer);
            }
        } else {
            
            MessageUtils.broadcastMessage(
                    bcPlayer.getTeamType().getChatColor() + player.getName() + 
                    ChatColor.YELLOW + " умер");
        }
        
        
        gameManager.schedulePlayerRespawn(player);
    }
    
    /**
     * Проверяет и обрабатывает достижение киллстрика
     */
    private void checkKillstreak(Player player, BCPlayer bcPlayer) {
        int killStreak = bcPlayer.getKillStreak();
        
        
        if (killStreak == 3) {
            MessageUtils.broadcastMessage(
                    bcPlayer.getTeamType().getChatColor() + player.getName() + 
                    ChatColor.YELLOW + " совершил " + ChatColor.GOLD + "3 убийства подряд!");
            
            MessageUtils.sendTitle(player, 
                    ChatColor.GOLD + "3 убийства подряд!", 
                    ChatColor.YELLOW + "Доступна способность: воздушная бомбардировка", 
                    10, 40, 10);
        } else if (killStreak == 5) {
            MessageUtils.broadcastMessage(
                    bcPlayer.getTeamType().getChatColor() + player.getName() + 
                    ChatColor.YELLOW + " совершил " + ChatColor.GOLD + "5 убийств подряд!");
            
            MessageUtils.sendTitle(player, 
                    ChatColor.GOLD + "5 убийств подряд!", 
                    ChatColor.YELLOW + "Доступна способность: разведывательный дрон", 
                    10, 40, 10);
        } else if (killStreak == 7) {
            MessageUtils.broadcastMessage(
                    bcPlayer.getTeamType().getChatColor() + player.getName() + 
                    ChatColor.YELLOW + " совершил " + ChatColor.GOLD + "7 убийств подряд!");
            
            MessageUtils.sendTitle(player, 
                    ChatColor.GOLD + "7 убийств подряд!", 
                    ChatColor.YELLOW + "Доступна способность: усиленный бронежилет", 
                    10, 40, 10);
        }
    }
    
    /**
     * Обрабатывает респаун игрока
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        
        if (gameManager.isPlayerInGame(player)) {
            BCPlayer bcPlayer = gameManager.getPlayer(player.getUniqueId());
            
            if (bcPlayer != null && bcPlayer.isWaitingForRespawn()) {
                
                Arena arena = gameManager.getArena();
                if (arena != null) {
                    if (bcPlayer.getTeamType() == TeamType.TEAM_A) {
                        event.setRespawnLocation(arena.getTeamASpawn());
                    } else if (bcPlayer.getTeamType() == TeamType.TEAM_B) {
                        event.setRespawnLocation(arena.getTeamBSpawn());
                    }
                }
            }
        }
    }
    
    /**
     * Обрабатывает получение урона игроком
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            
            
            if (!gameManager.isPlayerInGame(player)) {
                return;
            }
            
            
            if (!gameManager.isGameActive()) {
                event.setCancelled(true);
                return;
            }
            
            
            BCPlayer bcPlayer = gameManager.getPlayer(player.getUniqueId());
            if (bcPlayer != null && bcPlayer.isWaitingForRespawn()) {
                
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Обрабатывает нанесение урона игроком другой сущности
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        
        Player attacker = null;
        
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            ProjectileSource source = projectile.getShooter();
            
            if (source instanceof Player) {
                attacker = (Player) source;
            }
        }
        
        if (attacker == null) {
            return;
        }
        
        
        if (!gameManager.isPlayerInGame(attacker)) {
            return;
        }
        
        
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player target = (Player) event.getEntity();
        
        
        if (!gameManager.isPlayerInGame(target)) {
            return;
        }
        
        
        BCPlayer attackerBCPlayer = gameManager.getPlayer(attacker.getUniqueId());
        BCPlayer targetBCPlayer = gameManager.getPlayer(target.getUniqueId());
        
        if (attackerBCPlayer == null || targetBCPlayer == null) {
            return;
        }
        
        
        if (attackerBCPlayer.getTeamType() == targetBCPlayer.getTeamType()) {
            event.setCancelled(true);
            return;
        }
        
        
        if (targetBCPlayer.isWaitingForRespawn()) {
            event.setCancelled(true);
            return;
        }
        
        
        if (event.getDamager() instanceof Projectile && attacker.getInventory().getItemInMainHand().getType() == Material.BOW) {
            
            event.setDamage(event.getDamage() * 2);
            
            
            MessageUtils.sendActionBar(attacker, ChatColor.RED + "Критическое попадание!");
        }
    }
    
    /**
     * Обрабатывает попадание снаряда
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        
        
        if (projectile.getType() == EntityType.EGG) {
            
            Location location = projectile.getLocation();
            
            
            location.getWorld().createExplosion(location, 2F, false, false);
            
            
            projectile.remove();
        } else if (projectile.getType() == EntityType.SNOWBALL) {
            
            if (event.getHitEntity() != null && event.getHitEntity() instanceof Player) {
                Player target = (Player) event.getHitEntity();
                
                
                if (gameManager.isPlayerInGame(target)) {
                    markPlayer(target);
                }
            }
        }
    }
    
    /**
     * Помечает игрока для легкого обнаружения
     */
    private void markPlayer(Player player) {
        
        MessageUtils.sendActionBar(player, ChatColor.RED + "Вас пометили!");
        
        
        player.setGlowing(true);
        
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.setGlowing(false);
            }
        }, 100L); 
    }
    
    /**
     * Обрабатывает движение игрока для контроля контрольных точек
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        
        if (!gameManager.isPlayerInGame(player) || !gameManager.isGameActive()) {
            return;
        }
        
        
        BCPlayer bcPlayer = gameManager.getPlayer(player.getUniqueId());
        if (bcPlayer == null || bcPlayer.isWaitingForRespawn()) {
            return;
        }
        
        
        Arena arena = gameManager.getArena();
        if (arena == null) {
            return;
        }
        
        
        for (CaptureFlag flag : arena.getCapturePoints()) {
            
            if (flag.isPlayerInRadius(player)) {
                
                flag.addPlayerInRadius(player, bcPlayer.getTeamType());
                
                
                String status;
                if (flag.getTeamOwner() == bcPlayer.getTeamType()) {
                    status = ChatColor.GREEN + "Контролируется вашей командой";
                } else if (flag.getTeamOwner() == TeamType.NEUTRAL) {
                    status = ChatColor.YELLOW + "Нейтральная";
                } else {
                    status = ChatColor.RED + "Контролируется вражеской командой";
                }
                
                String captureStatus = "";
                if (flag.isBeingCaptured()) {
                    if (flag.getCapturingTeam() == bcPlayer.getTeamType()) {
                        captureStatus = ChatColor.GREEN + " (Захват: " + flag.getCapturePercentage() + "%)";
                    } else {
                        captureStatus = ChatColor.RED + " (Захват: " + flag.getCapturePercentage() + "%)";
                    }
                }
                
                MessageUtils.sendActionBar(player, 
                        ChatColor.GOLD + flag.getName() + ": " + status + captureStatus);
            } else {
                
                flag.removePlayerFromRadius(player);
            }
        }
    }
    
    /**
     * Обрабатывает использование предметов игроком
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();
        
        
        ArenaSetupManager setupManager = gameManager.getArenaSetupManager();
        if (setupManager != null && setupManager.isPlayerInSetupMode(player)) {
            if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                
                if (setupManager.handleBlockClick(player, event.getClickedBlock().getLocation(), item)) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        
        
        if (!gameManager.isPlayerInGame(player) || !gameManager.isGameActive()) {
            return;
        }
        
        
        BCPlayer bcPlayer = gameManager.getPlayer(player.getUniqueId());
        if (bcPlayer == null || bcPlayer.isWaitingForRespawn()) {
            return;
        }
        
        
        if (item != null && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            handleItemUse(player, item, bcPlayer);
        }
    }
    
    /**
     * Обрабатывает использование предмета игроком
     */
    private void handleItemUse(Player player, ItemStack item, BCPlayer bcPlayer) {
        
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = item.getItemMeta().getDisplayName();
        
        
        if (itemName.contains("Рывок")) {
            
            
        } else if (itemName.contains("Невидимость")) {
            
            bcPlayer.setInvisible(true);
            player.sendMessage(ChatColor.AQUA + "Вы стали невидимым на 10 секунд!");
            
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (bcPlayer.isInvisible()) {
                    bcPlayer.setInvisible(false);
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.YELLOW + "Эффект невидимости закончился!");
                    }
                }
            }, 200L); 
        }
    }
    
    /**
     * Блокирует разрушение блоков в игре
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        
        if (gameManager.isPlayerInGame(player)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Обрабатывает размещение блоков в игре
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        
        
        BCPlayer bcPlayer = gameManager.getPlayer(player.getUniqueId());
        if (bcPlayer == null) {
            return;
        }
        
        
        if (bcPlayer.getClassType().getId().equals("engineer")) {
            
            if (block.getType() == Material.STONE_PRESSURE_PLATE) {
                
                player.sendMessage(ChatColor.GREEN + "Вы установили мину!");
                return;
            }
        }
        
        
        event.setCancelled(true);
    }
    
    /**
     * Обрабатывает чат игроков в режиме настройки арены
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        
        ArenaSetupManager setupManager = gameManager.getArenaSetupManager();
        if (setupManager != null && setupManager.isPlayerInSetupMode(player)) {
            
            if (setupManager.getSetupSession(player).getLastClickedLocation() != null) {
                
                boolean handled = setupManager.handleFlagInfoMessage(player, event.getMessage());
                
                if (handled) {
                    event.setCancelled(true);
                }
            }
        }
    }
} 