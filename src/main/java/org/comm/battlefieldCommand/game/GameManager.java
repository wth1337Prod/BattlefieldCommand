package org.comm.battlefieldCommand.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.arena.Arena;
import org.comm.battlefieldCommand.game.arena.ArenaSetupManager;
import org.comm.battlefieldCommand.game.arena.capture.CaptureFlag;
import org.comm.battlefieldCommand.game.map.MapGenerator;
import org.comm.battlefieldCommand.game.player.BCPlayer;
import org.comm.battlefieldCommand.game.player.ClassType;
import org.comm.battlefieldCommand.game.scoreboard.ScoreboardManager;
import org.comm.battlefieldCommand.game.team.TeamManager;
import org.comm.battlefieldCommand.game.team.TeamType;
import org.comm.battlefieldCommand.utils.MessageUtils;
import org.comm.battlefieldCommand.voice.VoiceManager;

import java.util.*;

public class GameManager {
    private final BattlefieldCommand plugin;
    private Arena arena;
    private GameState gameState;
    private ArenaSetupManager arenaSetupManager;
    private TeamManager teamManager;
    private ScoreboardManager scoreboardManager;
    private MapGenerator mapGenerator;
    private VoiceManager voiceManager;
    
    private final Map<UUID, BCPlayer> players = new HashMap<>();
    private final List<UUID> spectators = new ArrayList<>();
    
    
    private int gameTimerTaskId = -1;
    private int respawnTimerTaskId = -1;
    private int flagCaptureTimerTaskId = -1;
    private int gameTime = 0;
    private int maxGameTime = 1200; 
    
    public GameManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
        this.gameState = GameState.INACTIVE;
        this.arenaSetupManager = new ArenaSetupManager(plugin);
        this.teamManager = new TeamManager(plugin);
        this.scoreboardManager = new ScoreboardManager(plugin);
        this.mapGenerator = new MapGenerator(plugin);
        
        try {
            this.voiceManager = plugin.getVoiceManager();
            if (this.voiceManager == null) {
                plugin.getLogger().warning("VoiceManager не был инициализирован. Голосовой чат будет отключен.");
            }
        } catch (Exception e) {
            this.voiceManager = null;
            plugin.getLogger().warning("Ошибка при инициализации VoiceManager: " + e.getMessage());
            plugin.getLogger().warning("Голосовой чат будет отключен.");
        }
    }
    
    /**
     * Запускает процесс настройки арены
     */
    public void setupArena(Player player) {
        if (gameState != GameState.INACTIVE) {
            player.sendMessage(ChatColor.RED + "Невозможно настроить арену, пока идет игра!");
            return;
        }
        
        arenaSetupManager.startSetup(player);
        player.sendMessage(ChatColor.GREEN + "Начат процесс настройки арены. Используйте команды инструмента для выбора точек.");
    }
    
    /**
     * Проверяет, настроена ли арена
     */
    public boolean hasArenaSetup() {
        
        if (arena != null && arena.isValid()) {
            return true;
        }
        
        
        if (arenaSetupManager.hasSetupComplete()) {
            loadArenaFromConfig();
            return arena != null && arena.isValid();
        }
        
        return false;
    }
    
    /**
     * Загружает арену из конфигурации
     */
    public void loadArenaFromConfig() {
        plugin.getLogger().info("Загрузка арены из конфигурации...");
        arena = arenaSetupManager.createArena();
        if (arena != null) {
            plugin.getLogger().info("Арена успешно загружена: " + arena.getName());
        } else {
            plugin.getLogger().warning("Не удалось загрузить арену из конфигурации");
        }
    }
    
    /**
     * Присоединяет игрока к игре
     */
    public void joinGame(Player player, ClassType classType) {
        if (!isGameActive() && !isLobbyActive()) {
            player.sendMessage(ChatColor.RED + "Нет активной игры или лобби!");
            return;
        }
        
        
        if (players.containsKey(player.getUniqueId())) {
            BCPlayer bcPlayer = players.get(player.getUniqueId());
            bcPlayer.setClassType(classType);
            player.sendMessage(ChatColor.GREEN + "Вы сменили класс на " + classType.getDisplayName() + "!");
            
            
            updatePlayerAppearance(player);
            return;
        }
        
        
        TeamType teamType = determineTeamForPlayer();
        
        
        BCPlayer bcPlayer = new BCPlayer(player.getUniqueId(), player.getName(), teamType, classType);
        players.put(player.getUniqueId(), bcPlayer);
        
        
        teleportPlayerToTeamSpawn(player, teamType);
        
        
        if (voiceManager != null) {
            try {
                voiceManager.assignPlayerToVoiceChannel(player);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при добавлении игрока в голосовой канал: " + e.getMessage());
            }
        }
        
        
        updatePlayerAppearance(player);
        
        player.sendMessage(ChatColor.GREEN + "Вы присоединились к команде " + teamType.getDisplayName() + 
                           " в качестве " + classType.getDisplayName() + "!");
    }
    
    /**
     * Удаляет игрока из игры
     */
    public void leaveGame(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!players.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "Вы не участвуете в игре!");
            return;
        }
        
        
        players.remove(playerId);
        spectators.remove(playerId);
        
        
        player.getInventory().clear();
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.SURVIVAL);
        
        
        Location serverLobby = plugin.getConfigManager().getMainConfig().getLocation("server.lobby");
        if (serverLobby != null) {
            player.teleport(serverLobby);
        }
        
        
        if (voiceManager != null && voiceManager.isApiAvailable()) {
            try {
                
                voiceManager.setPlayerVoiceChannel(player, org.comm.battlefieldCommand.voice.VoiceChannel.ALL);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при изменении голосового канала игрока: " + e.getMessage());
            }
        }
        
        
        MessageUtils.broadcastMessage(ChatColor.YELLOW + player.getName() + " покинул игру");
        
        
        scoreboardManager.updateScoreboard();
    }
    
    /**
     * Отправляет игрока в режим наблюдателя
     */
    public void enterSpectatorMode(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (spectators.contains(playerId)) {
            player.sendMessage(ChatColor.RED + "Вы уже находитесь в режиме наблюдателя!");
            return;
        }
        
        
        if (players.containsKey(playerId)) {
            players.remove(playerId);
        }
        
        
        spectators.add(playerId);
        
        
        player.setGameMode(GameMode.SPECTATOR);
        
        
        if (arena != null) {
            player.teleport(arena.getCenter());
        }
        
        player.sendMessage(ChatColor.GREEN + "Вы вошли в режим наблюдателя. Используйте /bc leave чтобы выйти.");
        
        
        scoreboardManager.updateScoreboard();
    }
    
    /**
     * Меняет класс игрока
     */
    public void changePlayerClass(Player player, ClassType classType) {
        UUID playerId = player.getUniqueId();
        
        if (!players.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "Вы не участвуете в игре!");
            return;
        }
        
        BCPlayer bcPlayer = players.get(playerId);
        bcPlayer.setClassType(classType);
        
        
        updatePlayerAppearance(player);
        
        player.sendMessage(ChatColor.GREEN + "Ваш класс изменен на " + ChatColor.AQUA + classType.getDisplayName());
        
        
        if (gameState == GameState.RUNNING) {
            schedulePlayerRespawn(player);
        }
    }
    
    /**
     * Запускает игру, если арена настроена
     */
    public void startGame() {
        if (!hasArenaSetup()) {
            plugin.getLogger().warning("Невозможно запустить игру: арена не настроена!");
            return;
        }
        
        if (isGameActive()) {
            plugin.getLogger().warning("Игра уже запущена!");
            return;
        }
        
        gameState = GameState.RUNNING;
        gameTime = 0;
        maxGameTime = plugin.getConfigManager().getMainConfig().getInt("game.time_limit", 1200);
        
        
        if (voiceManager != null) {
            try {
                voiceManager.setupVoiceGroups();
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при настройке голосовых групп: " + e.getMessage());
            }
        }
        
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(ChatColor.GREEN + "Игра началась!");
        }
        
        
        startGameTimer();
        
        
        startFlagCaptureTimer();
        
        
        startRespawnTimer();
        
        
        scoreboardManager.updateScoreboard();
        
        
        MessageUtils.broadcastTitle(ChatColor.GREEN + "Игра началась!", 
                ChatColor.YELLOW + "Захватывайте контрольные точки и уничтожайте врагов", 
                10, 70, 20);
    }
    
    /**
     * Останавливает текущую игру
     */
    public void stopGame() {
        if (!isGameActive()) {
            plugin.getLogger().warning("Невозможно остановить игру: нет активной игры!");
            return;
        }
        
        plugin.getLogger().info("Останавливаем игру...");
        gameState = GameState.INACTIVE;
        
        
        if (plugin.getVoiceManager() != null) {
            try {
                plugin.getLogger().info("Очищаем голосовые группы...");
                plugin.getVoiceManager().cleanupVoiceGroups();
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка при очистке голосовых групп: " + e.getMessage());
                plugin.getLogger().severe("Игра будет остановлена, но могут возникнуть проблемы с голосовым чатом");
            }
        } else {
            plugin.getLogger().info("VoiceManager не найден, пропускаем очистку голосовых групп");
        }
        
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(ChatColor.RED + "Игра остановлена!");
        }
        
        
        stopAllTimers();
        
        
        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                teleportToLobby(player);
            }
        }
        
        
        for (UUID spectatorId : spectators) {
            Player spectator = Bukkit.getPlayer(spectatorId);
            if (spectator != null && spectator.isOnline()) {
                spectator.setGameMode(GameMode.SURVIVAL);
                teleportToLobby(spectator);
            }
        }
        
        
        players.clear();
        spectators.clear();
        
        
        if (gameTime > 200) {
            displayGameResults();
        }
        
        
        scoreboardManager.hideScoreboard();
        
        
        MessageUtils.broadcastTitle(ChatColor.RED + "Игра окончена!", "", 10, 70, 20);
        
        plugin.getLogger().info("Игра успешно остановлена");
    }
    
    /**
     * Перезапускает игру
     */
    public void restartGame() {
        stopGame();
        startGame();
    }
    
    /**
     * Отправляет информацию о текущей игре
     */
    public void sendGameInfo(CommandSender sender) {
        if (gameState != GameState.RUNNING) {
            sender.sendMessage(ChatColor.RED + "Игра не активна!");
            return;
        }
        
        int teamACount = 0;
        int teamBCount = 0;
        
        for (BCPlayer bcPlayer : players.values()) {
            if (bcPlayer.getTeamType() == TeamType.TEAM_A) {
                teamACount++;
            } else if (bcPlayer.getTeamType() == TeamType.TEAM_B) {
                teamBCount++;
            }
        }
        
        int teamAScore = teamManager.getTeamScore(TeamType.TEAM_A);
        int teamBScore = teamManager.getTeamScore(TeamType.TEAM_B);
        
        int minutesRemaining = (maxGameTime - gameTime) / 1200;
        int secondsRemaining = ((maxGameTime - gameTime) % 1200) / 20;
        
        sender.sendMessage(ChatColor.GOLD + "=== Информация об игре ===");
        sender.sendMessage(ChatColor.YELLOW + "Время: " + ChatColor.WHITE + 
                String.format("%02d:%02d", minutesRemaining, secondsRemaining));
        sender.sendMessage(TeamType.TEAM_A.getChatColor() + TeamType.TEAM_A.getDisplayName() + 
                ChatColor.WHITE + ": " + teamAScore + " очков, " + teamACount + " игроков");
        sender.sendMessage(TeamType.TEAM_B.getChatColor() + TeamType.TEAM_B.getDisplayName() + 
                ChatColor.WHITE + ": " + teamBScore + " очков, " + teamBCount + " игроков");
        
        
        if (arena != null) {
            sender.sendMessage(ChatColor.YELLOW + "Контрольные точки:");
            for (CaptureFlag flag : arena.getCapturePoints()) {
                sender.sendMessage("  " + flag.getTeamOwner().getChatColor() + 
                        flag.getName() + ChatColor.WHITE + " - " + 
                        (flag.isBeingCaptured() ? "захватывается" : "под контролем"));
            }
        }
    }
    
    /**
     * Проверяет, участвует ли игрок в игре
     */
    public boolean isPlayerInGame(Player player) {
        return players.containsKey(player.getUniqueId());
    }
    
    /**
     * Проверяет, активна ли игра
     */
    public boolean isGameActive() {
        return gameState == GameState.RUNNING;
    }
    
    /**
     * Останавливает все запущенные таймеры
     */
    private void stopAllTimers() {
        if (gameTimerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(gameTimerTaskId);
            gameTimerTaskId = -1;
        }
        
        if (respawnTimerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(respawnTimerTaskId);
            respawnTimerTaskId = -1;
        }
        
        if (flagCaptureTimerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(flagCaptureTimerTaskId);
            flagCaptureTimerTaskId = -1;
        }
    }
    
    /**
     * Запускает таймер игры
     */
    private void startGameTimer() {
        gameTimerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            gameTime++;
            
            
            if (gameTime % 20 == 0) {
                scoreboardManager.updateScoreboard();
            }
            
            
            if (gameTime >= maxGameTime) {
                endGame();
                return;
            }
            
            
            int targetScore = plugin.getConfigManager().getMainConfig().getInt("game.target_score", 500);
            if (teamManager.getTeamScore(TeamType.TEAM_A) >= targetScore || 
                    teamManager.getTeamScore(TeamType.TEAM_B) >= targetScore) {
                endGame();
            }
        }, 1L, 1L);
    }
    
    /**
     * Запускает таймер захвата флагов
     */
    private void startFlagCaptureTimer() {
        if (arena == null) {
            return;
        }
        
        flagCaptureTimerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            
            for (CaptureFlag flag : arena.getCapturePoints()) {
                TeamType owner = flag.getTeamOwner();
                if (owner != TeamType.NEUTRAL) {
                    int controlPoints = plugin.getConfigManager().getMainConfig().getInt("game.control_points", 1);
                    teamManager.addPoints(owner, controlPoints);
                }
                
                
                flag.processCaptureProgress();
            }
        }, 20L, 20L);
    }
    
    /**
     * Запускает таймер респауна игроков
     */
    private void startRespawnTimer() {
        int respawnInterval = plugin.getConfigManager().getMainConfig().getInt("game.respawn_interval", 200);
        int respawnGroupSize = plugin.getConfigManager().getMainConfig().getInt("game.respawn_group_size", 5);
        
        respawnTimerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            List<UUID> teamARespawn = new ArrayList<>();
            List<UUID> teamBRespawn = new ArrayList<>();
            
            
            for (BCPlayer bcPlayer : players.values()) {
                if (bcPlayer.isWaitingForRespawn()) {
                    if (bcPlayer.getTeamType() == TeamType.TEAM_A) {
                        teamARespawn.add(bcPlayer.getPlayerId());
                    } else if (bcPlayer.getTeamType() == TeamType.TEAM_B) {
                        teamBRespawn.add(bcPlayer.getPlayerId());
                    }
                }
            }
            
            
            for (int i = 0; i < Math.min(respawnGroupSize, teamARespawn.size()); i++) {
                UUID playerId = teamARespawn.get(i);
                respawnPlayer(playerId, TeamType.TEAM_A);
            }
            
            for (int i = 0; i < Math.min(respawnGroupSize, teamBRespawn.size()); i++) {
                UUID playerId = teamBRespawn.get(i);
                respawnPlayer(playerId, TeamType.TEAM_B);
            }
        }, respawnInterval, respawnInterval);
    }
    
    /**
     * Завершает игру и определяет победителя
     */
    private void endGame() {
        int teamAScore = teamManager.getTeamScore(TeamType.TEAM_A);
        int teamBScore = teamManager.getTeamScore(TeamType.TEAM_B);
        
        TeamType winnerTeam;
        if (teamAScore > teamBScore) {
            winnerTeam = TeamType.TEAM_A;
        } else if (teamBScore > teamAScore) {
            winnerTeam = TeamType.TEAM_B;
        } else {
            winnerTeam = TeamType.NEUTRAL;
        }
        
        
        if (winnerTeam != TeamType.NEUTRAL) {
            MessageUtils.broadcastTitle(
                    winnerTeam.getChatColor() + winnerTeam.getDisplayName() + " победила!",
                    ChatColor.YELLOW + "Счет: " + TeamType.TEAM_A.getChatColor() + teamAScore + 
                    ChatColor.WHITE + " - " + TeamType.TEAM_B.getChatColor() + teamBScore,
                    10, 70, 20);
        } else {
            MessageUtils.broadcastTitle(
                    ChatColor.YELLOW + "Ничья!",
                    ChatColor.YELLOW + "Счет: " + teamAScore + " - " + teamBScore,
                    10, 70, 20);
        }
        
        
        stopGame();
    }
    
    /**
     * Планирует респаун игрока при следующей волне
     */
    public void schedulePlayerRespawn(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!players.containsKey(playerId)) {
            return;
        }
        
        BCPlayer bcPlayer = players.get(playerId);
        bcPlayer.setWaitingForRespawn(true);
        
        
        player.setGameMode(GameMode.SPECTATOR);
        
        player.sendMessage(ChatColor.YELLOW + "Вы появитесь при следующей волне респауна.");
        
        
        if (arena != null) {
            CaptureFlag teamFlag = arena.getTeamMainFlag(bcPlayer.getTeamType());
            if (teamFlag != null) {
                player.teleport(teamFlag.getLocation());
            }
        }
    }
    
    /**
     * Респаунит игрока
     */
    private void respawnPlayer(UUID playerId, TeamType teamType) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        
        BCPlayer bcPlayer = players.get(playerId);
        if (bcPlayer == null) {
            return;
        }
        
        bcPlayer.setWaitingForRespawn(false);
        
        
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);
        
        
        teleportToTeamSpawn(player, teamType);
        
        
        updatePlayerAppearance(player);
        
        
        player.sendMessage(ChatColor.GREEN + "Вы возродились!");
    }
    
    /**
     * Отображает результаты игры
     */
    private void displayGameResults() {
        int teamAScore = teamManager.getTeamScore(TeamType.TEAM_A);
        int teamBScore = teamManager.getTeamScore(TeamType.TEAM_B);
        
        
        MessageUtils.broadcastMessage(ChatColor.GOLD + "=== Результаты игры ===");
        MessageUtils.broadcastMessage(
                TeamType.TEAM_A.getChatColor() + TeamType.TEAM_A.getDisplayName() + 
                ChatColor.WHITE + ": " + teamAScore + " очков");
        MessageUtils.broadcastMessage(
                TeamType.TEAM_B.getChatColor() + TeamType.TEAM_B.getDisplayName() + 
                ChatColor.WHITE + ": " + teamBScore + " очков");
        
        
        MessageUtils.broadcastMessage(ChatColor.GOLD + "=== Лучшие игроки ===");
        
        
        List<BCPlayer> sortedPlayers = new ArrayList<>(players.values());
        sortedPlayers.sort((p1, p2) -> Integer.compare(p2.getKills(), p1.getKills()));
        
        
        for (int i = 0; i < Math.min(5, sortedPlayers.size()); i++) {
            BCPlayer player = sortedPlayers.get(i);
            MessageUtils.broadcastMessage(
                    (i + 1) + ". " + 
                    player.getTeamType().getChatColor() + player.getPlayerName() + 
                    ChatColor.WHITE + ": " + player.getKills() + " убийств, " + 
                    player.getDeaths() + " смертей, " + 
                    player.getCaptures() + " захватов");
        }
    }
    
    /**
     * Телепортирует игрока на спавн команды
     */
    private void teleportToTeamSpawn(Player player, TeamType teamType) {
        if (arena == null) {
            return;
        }
        
        if (teamType == TeamType.TEAM_A) {
            player.teleport(arena.getTeamASpawn());
        } else if (teamType == TeamType.TEAM_B) {
            player.teleport(arena.getTeamBSpawn());
        } else {
            player.teleport(arena.getCenter());
        }
    }
    
    /**
     * Телепортирует игрока в лобби
     */
    private void teleportToLobby(Player player) {
        Location lobby = null;
        
        
        if (arena != null && arena.getLobbySpawn() != null) {
            lobby = arena.getLobbySpawn();
        } else {
            
            lobby = plugin.getConfigManager().getMainConfig().getLocation("server.lobby");
        }
        
        
        if (lobby != null) {
            player.teleport(lobby);
        }
    }
    
    /**
     * Обновляет инвентарь и внешний вид игрока в соответствии с его классом
     */
    private void updatePlayerAppearance(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!players.containsKey(playerId)) {
            return;
        }
        
        BCPlayer bcPlayer = players.get(playerId);
        ClassType classType = bcPlayer.getClassType();
        
        
        player.getInventory().clear();
        
        
        switch (classType) {
            case ASSAULT:
                
                org.comm.battlefieldCommand.game.EquipmentManager.equipAssault(player, bcPlayer.getTeamType());
                break;
            case SNIPER:
                
                org.comm.battlefieldCommand.game.EquipmentManager.equipSniper(player, bcPlayer.getTeamType());
                break;
            case ENGINEER:
                
                org.comm.battlefieldCommand.game.EquipmentManager.equipEngineer(player, bcPlayer.getTeamType());
                break;
        }
    }
    
    /**
     * Выключает все процессы игры при выключении плагина
     */
    public void shutdown() {
        stopAllTimers();
        
        
        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.getInventory().clear();
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        
        for (UUID spectatorId : spectators) {
            Player spectator = Bukkit.getPlayer(spectatorId);
            if (spectator != null && spectator.isOnline()) {
                spectator.setGameMode(GameMode.SURVIVAL);
            }
        }
        
        
        players.clear();
        spectators.clear();
        
        
        scoreboardManager.hideScoreboard();
    }
    
    
    public Arena getArena() {
        return arena;
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    public TeamManager getTeamManager() {
        return teamManager;
    }
    
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    public Map<UUID, BCPlayer> getPlayers() {
        return players;
    }
    
    public BCPlayer getPlayer(UUID playerId) {
        return players.get(playerId);
    }
    
    /**
     * Состояния игры
     */
    public enum GameState {
        INACTIVE,
        WAITING,
        RUNNING,
        ENDING
    }

    /**
     * Возвращает локацию спавна для указанной команды
     * @param teamType тип команды
     * @return локация спавна или null, если арена не настроена
     */
    public Location getTeamSpawnLocation(TeamType teamType) {
        if (arena == null) {
            return null;
        }
        
        switch (teamType) {
            case TEAM_A:
                return arena.getTeamASpawn();
            case TEAM_B:
                return arena.getTeamBSpawn();
            default:
                return arena.getCenter(); 
        }
    }

    /**
     * Возвращает команду игрока
     * @param player игрок Bukkit
     * @return тип команды игрока или null, если игрок не в игре
     */
    public TeamType getPlayerTeam(Player player) {
        if (player == null) return null;
        
        UUID playerId = player.getUniqueId();
        BCPlayer bcPlayer = players.get(playerId);
        
        return (bcPlayer != null) ? bcPlayer.getTeamType() : null;
    }

    /**
     * Проверяет, активно ли лобби
     */
    public boolean isLobbyActive() {
        return gameState == GameState.WAITING;
    }

    /**
     * Определяет команду для игрока на основе баланса
     */
    private TeamType determineTeamForPlayer() {
        return teamManager.getSmallestTeam();
    }

    /**
     * Телепортирует игрока на спавн его команды
     */
    private void teleportPlayerToTeamSpawn(Player player, TeamType teamType) {
        Location spawnLocation = getTeamSpawnLocation(teamType);
        if (spawnLocation != null) {
            player.teleport(spawnLocation);
        } else {
            player.sendMessage(ChatColor.RED + "Ошибка: точка спавна не настроена!");
        }
    }

    /**
     * Проверяет, находится ли игрок в зоне захвата флага
     */
    public void checkPlayerInCaptureZone(Player player, Location location) {
        if (arena == null || !isGameActive()) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        BCPlayer bcPlayer = players.get(playerId);
        if (bcPlayer == null) {
            return;
        }
        
        TeamType team = bcPlayer.getTeamType();
        
        
        for (CaptureFlag flag : arena.getCapturePoints()) {
            if (flag.isLocationInCaptureRadius(location)) {
                
                flag.registerPlayerInZone(player, team);
                
                
                if (flag.getTeamOwner() != team) {
                    
                    long now = System.currentTimeMillis();
                    if (now - bcPlayer.getLastCaptureNotificationTime() > 5000) {
                        bcPlayer.setLastCaptureNotificationTime(now);
                        player.sendMessage(ChatColor.GOLD + "Вы захватываете " + flag.getName() + "!");
                    }
                }
                
                return;
            }
        }
    }
    
    /**
     * Выдает награду за серию убийств
     */
    public void giveKillstreakReward(Player player, int killStreak) {
        if (!isGameActive() || !players.containsKey(player.getUniqueId())) {
            return;
        }
        
        
        switch (killStreak) {
            case 3:
                
                plugin.getKillstreakManager().giveReconDrone(player);
                MessageUtils.broadcastMessage(
                    player.getName() + ChatColor.GOLD + " получает разведывательный дрон за серию из 3 убийств!");
                break;
            case 5:
                
                plugin.getKillstreakManager().giveEnhancedArmor(player);
                MessageUtils.broadcastMessage(
                    player.getName() + ChatColor.GOLD + " получает усиленный бронежилет за серию из 5 убийств!");
                break;
            case 7:
                
                plugin.getKillstreakManager().giveAirStrike(player);
                MessageUtils.broadcastMessage(
                    player.getName() + ChatColor.GOLD + " получает авиаудар за серию из 7 убийств!");
                break;
        }
    }

    /**
     * Получает список наблюдателей
     */
    public List<UUID> getSpectators() {
        return spectators;
    }
    
    /**
     * Возвращает текущее игровое время в тиках
     */
    public int getGameTime() {
        return gameTime;
    }
    
    /**
     * Возвращает менеджер экипировки игроков
     */
    public org.comm.battlefieldCommand.equipment.EquipmentManager getEquipmentManager() {
        
        return plugin.getEquipmentManager();
    }
    
    /**
     * Обновляет очки на табло
     */
    public void updateScoreboardScore() {
        if (scoreboardManager != null) {
            scoreboardManager.updateScoreboard();
        }
    }
    
    /**
     * Возвращает менеджер настройки арены
     */
    public ArenaSetupManager getArenaSetupManager() {
        return arenaSetupManager;
    }
    
    public MapGenerator getMapGenerator() {
        return mapGenerator;
    }
} 