package org.comm.battlefieldCommand.game.team;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.player.BCPlayer;
import org.comm.battlefieldCommand.game.player.ClassType;
import org.comm.battlefieldCommand.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Управляет командами и игроками в них
 */
public class TeamManager {
    private final BattlefieldCommand plugin;
    
    
    private final Map<UUID, BCPlayer> players = new HashMap<>();
    
    
    private final Map<TeamType, Set<UUID>> teamPlayers = new HashMap<>();
    
    
    private Scoreboard scoreboardA;
    private Scoreboard scoreboardB;
    
    
    private int teamAScore = 0;
    private int teamBScore = 0;
    
    public TeamManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
        
        
        for (TeamType teamType : TeamType.values()) {
            teamPlayers.put(teamType, new HashSet<>());
        }
        
        
        initializeTeamScoreboards();
    }
    
    /**
     * Инициализирует скорборды для команд
     */
    private void initializeTeamScoreboards() {
        scoreboardA = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboardB = Bukkit.getScoreboardManager().getNewScoreboard();
        
        
        Team teamA = scoreboardA.registerNewTeam(TeamType.TEAM_A.getId());
        Team teamB_forA = scoreboardA.registerNewTeam(TeamType.TEAM_B.getId());
        
        teamA.setColor(TeamType.TEAM_A.getChatColor());
        teamA.setAllowFriendlyFire(false);
        teamA.setCanSeeFriendlyInvisibles(true);
        teamA.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        
        teamB_forA.setColor(TeamType.TEAM_B.getChatColor());
        teamB_forA.setAllowFriendlyFire(false);
        teamB_forA.setCanSeeFriendlyInvisibles(false);
        teamB_forA.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        
        
        Team teamA_forB = scoreboardB.registerNewTeam(TeamType.TEAM_A.getId());
        Team teamB = scoreboardB.registerNewTeam(TeamType.TEAM_B.getId());
        
        teamA_forB.setColor(TeamType.TEAM_A.getChatColor());
        teamA_forB.setAllowFriendlyFire(false);
        teamA_forB.setCanSeeFriendlyInvisibles(false);
        teamA_forB.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        
        teamB.setColor(TeamType.TEAM_B.getChatColor());
        teamB.setAllowFriendlyFire(false);
        teamB.setCanSeeFriendlyInvisibles(true);
        teamB.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
    }
    
    /**
     * Добавляет игрока в команду
     */
    public BCPlayer addPlayer(Player player, TeamType teamType, ClassType classType) {
        UUID playerId = player.getUniqueId();
        
        
        removePlayer(player);
        
        
        BCPlayer bcPlayer = new BCPlayer(playerId, player.getName(), teamType, classType);
        players.put(playerId, bcPlayer);
        
        
        teamPlayers.get(teamType).add(playerId);
        
        
        updatePlayerScoreboard(player, teamType);
        
        
        equipPlayer(player, teamType, classType);
        
        
        player.sendMessage(ChatColor.GREEN + "Вы присоединились к " + teamType.getChatColor() + 
                teamType.getDisplayName() + ChatColor.GREEN + " в качестве " + 
                classType.getDisplayName() + "!");
        
        
        broadcastToTeam(teamType, ChatColor.GREEN + player.getName() + 
                " присоединился к вашей команде как " + classType.getDisplayName() + "!");
        
        return bcPlayer;
    }
    
    /**
     * Удаляет игрока из команды
     */
    public void removePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        BCPlayer bcPlayer = players.get(playerId);
        
        if (bcPlayer != null) {
            TeamType teamType = bcPlayer.getTeamType();
            teamPlayers.get(teamType).remove(playerId);
            
            
            removeFromTeamScoreboard(player);
            
            
            broadcastToTeam(teamType, ChatColor.YELLOW + player.getName() + 
                    " покинул вашу команду.");
            
            players.remove(playerId);
        }
    }
    
    /**
     * Обновляет скорборд игрока в зависимости от его команды
     */
    private void updatePlayerScoreboard(Player player, TeamType teamType) {
        removeFromTeamScoreboard(player);
        
        if (teamType == TeamType.TEAM_A) {
            scoreboardA.getTeam(TeamType.TEAM_A.getId()).addEntry(player.getName());
            scoreboardB.getTeam(TeamType.TEAM_A.getId()).addEntry(player.getName());
            player.setScoreboard(scoreboardA);
        } else if (teamType == TeamType.TEAM_B) {
            scoreboardA.getTeam(TeamType.TEAM_B.getId()).addEntry(player.getName());
            scoreboardB.getTeam(TeamType.TEAM_B.getId()).addEntry(player.getName());
            player.setScoreboard(scoreboardB);
        }
    }
    
    /**
     * Удаляет игрока из скорбордов команд
     */
    private void removeFromTeamScoreboard(Player player) {
        if (scoreboardA.getTeam(TeamType.TEAM_A.getId()) != null) {
            scoreboardA.getTeam(TeamType.TEAM_A.getId()).removeEntry(player.getName());
        }
        if (scoreboardA.getTeam(TeamType.TEAM_B.getId()) != null) {
            scoreboardA.getTeam(TeamType.TEAM_B.getId()).removeEntry(player.getName());
        }
        if (scoreboardB.getTeam(TeamType.TEAM_A.getId()) != null) {
            scoreboardB.getTeam(TeamType.TEAM_A.getId()).removeEntry(player.getName());
        }
        if (scoreboardB.getTeam(TeamType.TEAM_B.getId()) != null) {
            scoreboardB.getTeam(TeamType.TEAM_B.getId()).removeEntry(player.getName());
        }
    }
    
    /**
     * Выдает игроку экипировку его команды и класса
     */
    private void equipPlayer(Player player, TeamType teamType, ClassType classType) {
        
        player.getInventory().clear();
        
        
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        
        
        Color teamColor = teamType.getColor();
        setArmorColor(helmet, teamColor);
        setArmorColor(chestplate, teamColor);
        setArmorColor(leggings, teamColor);
        setArmorColor(boots, teamColor);
        
        
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        
        
        plugin.getGameManager().getEquipmentManager().equipPlayerByClass(player, classType);
    }
    
    /**
     * Устанавливает цвет кожаной брони
     */
    private void setArmorColor(ItemStack armor, Color color) {
        if (armor.getItemMeta() instanceof LeatherArmorMeta) {
            LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
            meta.setColor(color);
            armor.setItemMeta(meta);
        }
    }
    
    /**
     * Отправляет сообщение всем игрокам указанной команды
     */
    public void broadcastToTeam(TeamType teamType, String message) {
        for (UUID playerId : teamPlayers.get(teamType)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * Возвращает список игроков в указанной команде
     */
    public List<Player> getTeamPlayers(TeamType teamType) {
        return teamPlayers.get(teamType).stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .collect(Collectors.toList());
    }
    
    /**
     * Возвращает список игроков противоположной команды
     */
    public List<Player> getOppositeTeamPlayers(TeamType teamType) {
        return getTeamPlayers(teamType.getOpposite());
    }
    
    /**
     * Возвращает количество игроков в команде
     */
    public int getTeamSize(TeamType teamType) {
        return teamPlayers.get(teamType).size();
    }
    
    /**
     * Возвращает команду с наименьшим количеством игроков
     */
    public TeamType getSmallestTeam() {
        if (getTeamSize(TeamType.TEAM_A) <= getTeamSize(TeamType.TEAM_B)) {
            return TeamType.TEAM_A;
        } else {
            return TeamType.TEAM_B;
        }
    }
    
    /**
     * Возвращает объект игрока по его UUID
     */
    public BCPlayer getPlayer(UUID playerId) {
        return players.get(playerId);
    }
    
    /**
     * Проверяет, находится ли игрок в команде
     */
    public boolean isInTeam(UUID playerId) {
        return players.containsKey(playerId);
    }
    
    /**
     * Возвращает команду игрока
     */
    public TeamType getPlayerTeam(UUID playerId) {
        BCPlayer player = players.get(playerId);
        return (player != null) ? player.getTeamType() : null;
    }
    
    /**
     * Проверяет, являются ли игроки членами одной команды
     */
    public boolean areTeammates(UUID player1, UUID player2) {
        TeamType team1 = getPlayerTeam(player1);
        TeamType team2 = getPlayerTeam(player2);
        
        return team1 != null && team2 != null && team1 == team2;
    }
    
    /**
     * Добавляет очки команде
     */
    public void addTeamScore(TeamType teamType, int points) {
        if (teamType == TeamType.TEAM_A) {
            teamAScore += points;
        } else if (teamType == TeamType.TEAM_B) {
            teamBScore += points;
        }
        
        
        updateScoreDisplay();
        
        
        broadcastToTeam(teamType, ChatColor.GREEN + "Ваша команда получила " + 
                points + " очк" + getPointsEnding(points) + "! Всего: " + 
                getTeamScore(teamType));
    }
    
    /**
     * Возвращает склонение слова "очки" в зависимости от числа
     */
    private String getPointsEnding(int points) {
        int lastDigit = points % 10;
        int lastTwoDigits = points % 100;
        
        if (lastDigit == 1 && lastTwoDigits != 11) {
            return "о";
        } else if (lastDigit >= 2 && lastDigit <= 4 && 
                  (lastTwoDigits < 10 || lastTwoDigits >= 20)) {
            return "а";
        } else {
            return "ов";
        }
    }
    
    /**
     * Обновляет отображение счета для всех игроков
     */
    private void updateScoreDisplay() {
        String scoreMessage = TeamType.TEAM_A.getChatColor() + 
                             "Команда A: " + teamAScore + 
                             ChatColor.WHITE + " | " + 
                             TeamType.TEAM_B.getChatColor() + 
                             "Команда B: " + teamBScore;
        
        
        MessageUtils.broadcastToPlayers(plugin.getServer().getOnlinePlayers(), scoreMessage);
        
        
        plugin.getGameManager().updateScoreboardScore();
    }
    
    /**
     * Возвращает текущий счет указанной команды
     */
    public int getTeamScore(TeamType teamType) {
        if (teamType == TeamType.TEAM_A) {
            return teamAScore;
        } else if (teamType == TeamType.TEAM_B) {
            return teamBScore;
        }
        return 0;
    }
    
    /**
     * Сбрасывает счет команд
     */
    public void resetTeamScores() {
        teamAScore = 0;
        teamBScore = 0;
        updateScoreDisplay();
    }
    
    /**
     * Возвращает команду с наибольшим счетом
     */
    public TeamType getWinningTeam() {
        if (teamAScore > teamBScore) {
            return TeamType.TEAM_A;
        } else if (teamBScore > teamAScore) {
            return TeamType.TEAM_B;
        } else {
            return TeamType.NEUTRAL; 
        }
    }
    
    /**
     * Очищает все команды и игроков
     */
    public void clearTeams() {
        
        for (UUID playerId : new ArrayList<>(players.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                removeFromTeamScoreboard(player);
            }
        }
        
        
        players.clear();
        for (TeamType teamType : TeamType.values()) {
            teamPlayers.get(teamType).clear();
        }
        
        
        resetTeamScores();
    }
    
    /**
     * Добавляет очки игроку и его команде
     */
    public void addPoints(UUID playerId, int points, String reason) {
        BCPlayer player = getPlayer(playerId);
        if (player == null) return;
        
        player.addPoints(points);
        
        
        Player bukkitPlayer = Bukkit.getPlayer(playerId);
        if (bukkitPlayer != null) {
            bukkitPlayer.sendMessage(ChatColor.GREEN + "+" + points + " очк" + getPointsEnding(points) + 
                    " (" + reason + ")");
        }
        
        
        TeamType teamType = player.getTeamType();
        addTeamScore(teamType, points);
    }
    
    /**
     * Перегрузка метода addPoints для прямого добавления очков команде
     */
    public void addPoints(TeamType teamType, int points) {
        
        addTeamScore(teamType, points);
    }
} 