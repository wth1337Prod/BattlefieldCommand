package org.comm.battlefieldCommand.game.arena.capture;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Представляет контрольную точку (флаг)
 */
public class CaptureFlag {
    private final String name;
    private final Location location;
    private final double captureRadius;
    private final TeamType defaultTeam;
    private final boolean isMainFlag;
    
    private TeamType teamOwner;
    private TeamType capturingTeam;
    private int captureProgress;
    private final int maxCaptureProgress;
    
    private final Map<UUID, TeamType> playersInRadius = new HashMap<>();
    
    public CaptureFlag(String name, Location location, double captureRadius, TeamType defaultTeam, boolean isMainFlag) {
        this.name = name;
        this.location = location;
        this.captureRadius = captureRadius;
        this.defaultTeam = defaultTeam;
        this.isMainFlag = isMainFlag;
        this.teamOwner = defaultTeam;
        this.capturingTeam = null;
        this.captureProgress = 0;
        this.maxCaptureProgress = 100;
    }
    
    /**
     * Сбрасывает контрольную точку в исходное состояние
     */
    public void reset() {
        teamOwner = defaultTeam;
        capturingTeam = null;
        captureProgress = 0;
        playersInRadius.clear();
    }
    
    /**
     * Добавляет игрока в радиус контрольной точки
     */
    public void addPlayerInRadius(Player player, TeamType teamType) {
        playersInRadius.put(player.getUniqueId(), teamType);
    }
    
    /**
     * Удаляет игрока из радиуса контрольной точки
     */
    public void removePlayerFromRadius(Player player) {
        playersInRadius.remove(player.getUniqueId());
    }
    
    /**
     * Обрабатывает процесс захвата контрольной точки
     */
    public void processCaptureProgress() {
        
        int teamACount = 0;
        int teamBCount = 0;
        
        for (TeamType teamType : playersInRadius.values()) {
            if (teamType == TeamType.TEAM_A) {
                teamACount++;
            } else if (teamType == TeamType.TEAM_B) {
                teamBCount++;
            }
        }
        
        
        TeamType dominantTeam = null;
        int dominance = 0;
        
        if (teamACount > teamBCount) {
            dominantTeam = TeamType.TEAM_A;
            dominance = teamACount - teamBCount;
        } else if (teamBCount > teamACount) {
            dominantTeam = TeamType.TEAM_B;
            dominance = teamBCount - teamACount;
        }
        
        
        if (dominantTeam != null && dominance > 0) {
            
            if (dominantTeam == teamOwner) {
                capturingTeam = null;
                captureProgress = 0;
                return;
            }
            
            
            if (capturingTeam == null || capturingTeam != dominantTeam) {
                capturingTeam = dominantTeam;
                captureProgress = 0;
            }
            
            
            captureProgress += dominance;
            
            
            if (captureProgress >= maxCaptureProgress) {
                teamOwner = dominantTeam;
                capturingTeam = null;
                captureProgress = 0;
                
                
                
            }
        } else {
            
            if (captureProgress > 0) {
                captureProgress = Math.max(0, captureProgress - 1);
                
                if (captureProgress == 0) {
                    capturingTeam = null;
                }
            }
        }
    }
    
    /**
     * Проверяет, находится ли игрок в радиусе захвата
     */
    public boolean isPlayerInRadius(Player player) {
        return player.getLocation().distance(location) <= captureRadius;
    }
    
    /**
     * Проверяет, захватывается ли точка в данный момент
     */
    public boolean isBeingCaptured() {
        return capturingTeam != null && captureProgress > 0;
    }
    
    /**
     * Получает процент захвата точки (0-100%)
     */
    public int getCapturePercentage() {
        return (captureProgress * 100) / maxCaptureProgress;
    }
    
    
    
    public String getName() {
        return name;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public double getCaptureRadius() {
        return captureRadius;
    }
    
    public TeamType getDefaultTeam() {
        return defaultTeam;
    }
    
    public boolean isMainFlag() {
        return isMainFlag;
    }
    
    public TeamType getTeamOwner() {
        return teamOwner;
    }
    
    public void setTeamOwner(TeamType teamOwner) {
        this.teamOwner = teamOwner;
    }
    
    public TeamType getCapturingTeam() {
        return capturingTeam;
    }
    
    public int getCaptureProgress() {
        return captureProgress;
    }
    
    public Map<UUID, TeamType> getPlayersInRadius() {
        return playersInRadius;
    }
    
    /**
     * Проверяет, находится ли указанная локация в радиусе захвата контрольной точки
     */
    public boolean isLocationInCaptureRadius(Location location) {
        return location.distance(this.location) <= captureRadius;
    }
    
    /**
     * Регистрирует игрока в зоне захвата
     */
    public void registerPlayerInZone(Player player, TeamType teamType) {
        addPlayerInRadius(player, teamType);
    }
} 