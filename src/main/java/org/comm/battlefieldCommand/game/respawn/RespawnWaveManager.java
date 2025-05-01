package org.comm.battlefieldCommand.game.respawn;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.player.BCPlayer;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.*;

/**
 * Управляет волновой системой респавна игроков
 */
public class RespawnWaveManager {
    private final BattlefieldCommand plugin;
    
    
    private int waveIntervalSeconds = 15; 
    private int maxPlayersPerWave = 5;    
    private int respawnProtectionSeconds = 5; 
    
    
    private final Map<TeamType, Queue<UUID>> respawnQueues = new HashMap<>();
    
    
    private BukkitTask respawnWaveTask = null;
    private int secondsUntilNextWave = 0;
    
    public RespawnWaveManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
        
        
        for (TeamType teamType : TeamType.values()) {
            if (teamType != TeamType.NEUTRAL) {
                respawnQueues.put(teamType, new LinkedList<>());
            }
        }
        
        
        loadConfig();
    }
    
    /**
     * Загружает настройки из конфигурации
     */
    private void loadConfig() {
        waveIntervalSeconds = plugin.getConfigManager().getMainConfig().getInt("respawn.wave_interval_seconds", 15);
        maxPlayersPerWave = plugin.getConfigManager().getMainConfig().getInt("respawn.max_players_per_wave", 5);
        respawnProtectionSeconds = plugin.getConfigManager().getMainConfig().getInt("respawn.protection_seconds", 5);
    }
    
    /**
     * Запускает систему волнового респавна
     */
    public void startRespawnWaves() {
        
        stopRespawnWaves();
        
        
        secondsUntilNextWave = waveIntervalSeconds;
        
        respawnWaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                secondsUntilNextWave--;
                
                
                if (secondsUntilNextWave <= 0) {
                    processRespawnWave();
                    secondsUntilNextWave = waveIntervalSeconds;
                }
                
                
                notifyPlayersInQueue();
            }
        }.runTaskTimer(plugin, 20L, 20L); 
    }
    
    /**
     * Останавливает систему волнового респавна
     */
    public void stopRespawnWaves() {
        if (respawnWaveTask != null && !respawnWaveTask.isCancelled()) {
            respawnWaveTask.cancel();
            respawnWaveTask = null;
        }
        
        
        for (Queue<UUID> queue : respawnQueues.values()) {
            queue.clear();
        }
    }
    
    /**
     * Добавляет игрока в очередь на респавн
     */
    public void addPlayerToRespawnQueue(Player player, BCPlayer bcPlayer) {
        if (player == null || bcPlayer == null) {
            return;
        }
        
        TeamType teamType = bcPlayer.getTeamType();
        
        
        Queue<UUID> queue = respawnQueues.get(teamType);
        if (queue != null && !queue.contains(player.getUniqueId())) {
            queue.add(player.getUniqueId());
            bcPlayer.setWaitingForRespawn(true);
            
            
            player.sendMessage(ChatColor.YELLOW + "Вы погибли! Респавн через " + 
                    secondsUntilNextWave + getSecondsEnding(secondsUntilNextWave));
            
            
            enableSpectatorMode(player);
        }
    }
    
    /**
     * Включает режим зрителя для игрока, ожидающего респавн
     */
    private void enableSpectatorMode(Player player) {
        
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.hidePlayer(plugin, player);
            }
        }
        
        
        player.setAllowFlight(true);
        player.setFlying(true);
        
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, false, false));
        
        
        player.sendMessage(ChatColor.GRAY + "Режим зрителя активирован. Вы можете свободно перемещаться до респавна.");
    }
    
    /**
     * Выключает режим зрителя для игрока
     */
    private void disableSpectatorMode(Player player) {
        
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showPlayer(plugin, player);
        }
        
        
        player.setFlying(false);
        player.setAllowFlight(false);
        
        
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }
    
    /**
     * Обрабатывает одну волну респавна
     */
    private void processRespawnWave() {
        
        for (Map.Entry<TeamType, Queue<UUID>> entry : respawnQueues.entrySet()) {
            TeamType teamType = entry.getKey();
            Queue<UUID> queue = entry.getValue();
            
            int playersToRespawn = Math.min(queue.size(), maxPlayersPerWave);
            
            if (playersToRespawn > 0) {
                
                broadcastRespawnWaveStart(teamType, playersToRespawn);
                
                
                for (int i = 0; i < playersToRespawn; i++) {
                    UUID playerId = queue.poll();
                    if (playerId != null) {
                        respawnPlayer(playerId, teamType);
                    }
                }
            }
        }
    }
    
    /**
     * Возрождает конкретного игрока
     */
    private void respawnPlayer(UUID playerId, TeamType teamType) {
        Player player = Bukkit.getPlayer(playerId);
        
        if (player == null || !player.isOnline()) {
            return;
        }
        
        BCPlayer bcPlayer = plugin.getGameManager().getPlayer(playerId);
        
        if (bcPlayer == null) {
            return;
        }
        
        
        disableSpectatorMode(player);
        
        
        Location spawnLocation = plugin.getGameManager().getTeamSpawnLocation(teamType);
        
        if (spawnLocation != null) {
            
            player.teleport(spawnLocation);
            
            
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            
            
            for (PotionEffect effect : player.getActivePotionEffects()) {
                if (isPotionEffectNegative(effect.getType())) {
                    player.removePotionEffect(effect.getType());
                }
            }
            
            
            if (respawnProtectionSeconds > 0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 
                        respawnProtectionSeconds * 20, 5, false, false));
            }
            
            
            player.sendMessage(ChatColor.GREEN + "Вы возродились! Защита после респавна: " + 
                    respawnProtectionSeconds + getSecondsEnding(respawnProtectionSeconds));
            
            
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            
            bcPlayer.setWaitingForRespawn(false);
            
            
            plugin.getGameManager().getEquipmentManager().equipPlayerByClass(player, bcPlayer.getClassType());
        }
    }
    
    /**
     * Уведомляет игроков в очереди о времени до респавна
     */
    private void notifyPlayersInQueue() {
        
        for (Map.Entry<TeamType, Queue<UUID>> entry : respawnQueues.entrySet()) {
            Queue<UUID> queue = entry.getValue();
            
            for (UUID playerId : queue) {
                Player player = Bukkit.getPlayer(playerId);
                
                if (player != null && player.isOnline()) {
                    
                    if (secondsUntilNextWave <= 5) {
                        player.sendMessage(ChatColor.YELLOW + "Респавн через " + 
                                secondsUntilNextWave + getSecondsEnding(secondsUntilNextWave));
                        
                        
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);
                    }
                }
            }
        }
    }
    
    /**
     * Оповещает о начале волны респавна
     */
    private void broadcastRespawnWaveStart(TeamType teamType, int playersCount) {
        
        for (Player player : plugin.getGameManager().getTeamManager().getTeamPlayers(teamType)) {
            player.sendMessage(teamType.getChatColor() + "Волна респавна: " + 
                    playersCount + " " + getPlayersEnding(playersCount) + " возрождается!");
        }
    }
    
    /**
     * Получает правильное окончание для слова "секунд"
     */
    private String getSecondsEnding(int seconds) {
        if (seconds % 100 >= 11 && seconds % 100 <= 19) {
            return " секунд";
        } else if (seconds % 10 == 1) {
            return " секунду";
        } else if (seconds % 10 >= 2 && seconds % 10 <= 4) {
            return " секунды";
        } else {
            return " секунд";
        }
    }
    
    /**
     * Получает правильное окончание для слова "игрок"
     */
    private String getPlayersEnding(int players) {
        if (players % 100 >= 11 && players % 100 <= 19) {
            return "игроков";
        } else if (players % 10 == 1) {
            return "игрок";
        } else if (players % 10 >= 2 && players % 10 <= 4) {
            return "игрока";
        } else {
            return "игроков";
        }
    }
    
    /**
     * Проверяет, является ли эффект зелья негативным
     */
    private boolean isPotionEffectNegative(PotionEffectType type) {
        return type == PotionEffectType.POISON ||
               type == PotionEffectType.WEAKNESS ||
               type == PotionEffectType.SLOWNESS ||
               type == PotionEffectType.NAUSEA ||
               type == PotionEffectType.BLINDNESS ||
               type == PotionEffectType.WITHER ||
               type == PotionEffectType.HUNGER ||
               type == PotionEffectType.MINING_FATIGUE;
    }
    
    /**
     * Возвращает время до следующей волны респавна
     */
    public int getSecondsUntilNextWave() {
        return secondsUntilNextWave;
    }
    
    /**
     * Возвращает количество игроков в очереди респавна для указанной команды
     */
    public int getQueueSize(TeamType teamType) {
        Queue<UUID> queue = respawnQueues.get(teamType);
        return queue != null ? queue.size() : 0;
    }
    
    /**
     * Проверяет, находится ли игрок в очереди на респавн
     */
    public boolean isPlayerInRespawnQueue(UUID playerId) {
        for (Queue<UUID> queue : respawnQueues.values()) {
            if (queue.contains(playerId)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Останавливает все процессы и очищает очереди
     */
    public void shutdown() {
        stopRespawnWaves();
        
        
        for (Queue<UUID> queue : respawnQueues.values()) {
            for (UUID playerId : queue) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    disableSpectatorMode(player);
                }
            }
        }
    }
} 