package org.comm.battlefieldCommand.game.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import org.bukkit.scoreboard.Team;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.GameManager;
import org.comm.battlefieldCommand.game.player.BCPlayer;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Управляет табло игры
 */
public class ScoreboardManager {
    private final BattlefieldCommand plugin;
    private final GameManager gameManager;
    private final org.bukkit.scoreboard.ScoreboardManager bukkitScoreboardManager;
    private final Scoreboard mainScoreboard;
    private Objective sidebarObjective;
    private Objective tablistObjective;
    
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    
    public ScoreboardManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.bukkitScoreboardManager = Bukkit.getScoreboardManager();
        this.mainScoreboard = bukkitScoreboardManager.getNewScoreboard();
        
        
        setupScoreboard();
    }
    
    /**
     * Настраивает основное табло и команды
     */
    private void setupScoreboard() {
        
        if (mainScoreboard.getObjective("bcSidebar") != null) {
            mainScoreboard.getObjective("bcSidebar").unregister();
        }
        
        sidebarObjective = mainScoreboard.registerNewObjective("bcSidebar", "dummy", ChatColor.GOLD + "Battlefield Command");
        sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        
        if (mainScoreboard.getObjective("bcTablist") != null) {
            mainScoreboard.getObjective("bcTablist").unregister();
        }
        
        tablistObjective = mainScoreboard.registerNewObjective("bcTablist", "dummy", ChatColor.YELLOW + "Очки");
        tablistObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        
        
        setupTeam(TeamType.TEAM_A, "teamA");
        setupTeam(TeamType.TEAM_B, "teamB");
        setupTeam(TeamType.NEUTRAL, "neutral");
    }
    
    /**
     * Настраивает команду на табло
     */
    private void setupTeam(TeamType teamType, String teamName) {
        Team team = mainScoreboard.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
        
        team = mainScoreboard.registerNewTeam(teamName);
        team.setColor(teamType.getChatColor());
        team.setPrefix(teamType.getChatColor() + "[" + teamType.getId() + "] ");
        team.setAllowFriendlyFire(false);
        team.setCanSeeFriendlyInvisibles(true);
    }
    
    /**
     * Обновляет табло для всех игроков
     */
    public void updateScoreboard() {
        
        if (!gameManager.isGameActive()) {
            hideScoreboard();
            return;
        }
        
        
        int teamAScore = gameManager.getTeamManager().getTeamScore(TeamType.TEAM_A);
        int teamBScore = gameManager.getTeamManager().getTeamScore(TeamType.TEAM_B);
        
        sidebarObjective.setDisplayName(ChatColor.GOLD + "Battlefield Command");
        
        
        for (String entry : mainScoreboard.getEntries()) {
            mainScoreboard.resetScores(entry);
        }
        
        
        int maxGameTime = plugin.getConfigManager().getMainConfig().getInt("game.time_limit", 1200);
        int gameTime = gameManager.getGameTime();
        int minutesRemaining = (maxGameTime - gameTime) / 1200;
        int secondsRemaining = ((maxGameTime - gameTime) % 1200) / 20;
        
        Score scoreLine0 = sidebarObjective.getScore(ChatColor.WHITE + "" + ChatColor.STRIKETHROUGH + "--------------------");
        scoreLine0.setScore(12);
        
        Score scoreLine1 = sidebarObjective.getScore(ChatColor.GOLD + "Время: " + ChatColor.WHITE + 
                String.format("%02d:%02d", minutesRemaining, secondsRemaining));
        scoreLine1.setScore(11);
        
        Score scoreLine2 = sidebarObjective.getScore(" ");
        scoreLine2.setScore(10);
        
        Score scoreLine3 = sidebarObjective.getScore(TeamType.TEAM_A.getChatColor() + "Команда A: " + ChatColor.WHITE + teamAScore);
        scoreLine3.setScore(9);
        
        Score scoreLine4 = sidebarObjective.getScore(TeamType.TEAM_B.getChatColor() + "Команда B: " + ChatColor.WHITE + teamBScore);
        scoreLine4.setScore(8);
        
        Score scoreLine5 = sidebarObjective.getScore("  ");
        scoreLine5.setScore(7);
        
        Score scoreLine6 = sidebarObjective.getScore(ChatColor.YELLOW + "Контрольные точки:");
        scoreLine6.setScore(6);
        
        
        if (gameManager.getArena() != null) {
            int flagIndex = 5;
            for (int i = 0; i < Math.min(3, gameManager.getArena().getCapturePoints().size()); i++) {
                org.comm.battlefieldCommand.game.arena.capture.CaptureFlag flag = gameManager.getArena().getCapturePoint(i);
                if (flag != null) {
                    Score flagLine = sidebarObjective.getScore(ChatColor.WHITE + "• " + 
                            flag.getTeamOwner().getChatColor() + flag.getName());
                    flagLine.setScore(flagIndex--);
                }
            }
        }
        
        Score scoreLine10 = sidebarObjective.getScore("   ");
        scoreLine10.setScore(2);
        
        Score scoreLine11 = sidebarObjective.getScore(ChatColor.YELLOW + "Игроков: " + 
                ChatColor.WHITE + gameManager.getPlayers().size());
        scoreLine11.setScore(1);
        
        Score scoreLine12 = sidebarObjective.getScore(ChatColor.WHITE + "" + ChatColor.STRIKETHROUGH + "--------------------" + ChatColor.RESET);
        scoreLine12.setScore(0);
        
        
        updatePlayerTeams(gameManager);
        
        
        showScoreboardToPlayers();
    }
    
    /**
     * Обновляет команды игроков на табло
     */
    private void updatePlayerTeams(GameManager gameManager) {
        
        for (Team team : mainScoreboard.getTeams()) {
            for (String entry : team.getEntries()) {
                team.removeEntry(entry);
            }
        }
        
        
        for (BCPlayer bcPlayer : gameManager.getPlayers().values()) {
            Player player = Bukkit.getPlayer(bcPlayer.getPlayerId());
            if (player != null && player.isOnline()) {
                TeamType teamType = bcPlayer.getTeamType();
                String teamName;
                
                switch (teamType) {
                    case TEAM_A:
                        teamName = "teamA";
                        break;
                    case TEAM_B:
                        teamName = "teamB";
                        break;
                    default:
                        teamName = "neutral";
                        break;
                }
                
                Team team = mainScoreboard.getTeam(teamName);
                if (team != null) {
                    team.addEntry(player.getName());
                }
                
                
                tablistObjective.getScore(player.getName()).setScore(bcPlayer.getScore());
            }
        }
    }
    
    /**
     * Показывает табло всем игрокам
     */
    private void showScoreboardToPlayers() {
        
        for (UUID playerId : gameManager.getPlayers().keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.setScoreboard(mainScoreboard);
            }
        }
        
        
        for (UUID spectatorId : gameManager.getSpectators()) {
            Player spectator = Bukkit.getPlayer(spectatorId);
            if (spectator != null && spectator.isOnline()) {
                spectator.setScoreboard(mainScoreboard);
            }
        }
    }
    
    /**
     * Скрывает табло у всех игроков
     */
    public void hideScoreboard() {
        Scoreboard emptyScoreboard = bukkitScoreboardManager.getNewScoreboard();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(emptyScoreboard);
        }
    }
    
    /**
     * Создает персональное табло для игрока
     */
    public Scoreboard createPlayerScoreboard(Player player) {
        Scoreboard playerScoreboard = bukkitScoreboardManager.getNewScoreboard();
        playerScoreboards.put(player.getUniqueId(), playerScoreboard);
        
        
        Objective sidebarObj = playerScoreboard.registerNewObjective("bcSidebar", "dummy", ChatColor.GOLD + "Battlefield Command");
        sidebarObj.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        Objective tablistObj = playerScoreboard.registerNewObjective("bcTablist", "dummy", ChatColor.YELLOW + "Очки");
        tablistObj.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        
        
        setupTeam(TeamType.TEAM_A, "teamA", playerScoreboard);
        setupTeam(TeamType.TEAM_B, "teamB", playerScoreboard);
        setupTeam(TeamType.NEUTRAL, "neutral", playerScoreboard);
        
        return playerScoreboard;
    }
    
    /**
     * Настраивает команду на персональном табло
     */
    private void setupTeam(TeamType teamType, String teamName, Scoreboard scoreboard) {
        Team team = scoreboard.registerNewTeam(teamName);
        team.setColor(teamType.getChatColor());
        team.setPrefix(teamType.getChatColor() + "[" + teamType.getId() + "] ");
        team.setAllowFriendlyFire(false);
        team.setCanSeeFriendlyInvisibles(true);
    }
} 