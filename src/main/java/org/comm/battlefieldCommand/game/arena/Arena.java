package org.comm.battlefieldCommand.game.arena;

import org.bukkit.Location;
import org.comm.battlefieldCommand.game.arena.capture.CaptureFlag;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.ArrayList;
import java.util.List;

/**
 * Представляет игровую арену
 */
public class Arena {
    private final String name;
    private Location teamASpawn;
    private Location teamBSpawn;
    private Location center;
    private Location lobbySpawn;
    
    private final List<CaptureFlag> capturePoints = new ArrayList<>();
    
    public Arena(String name) {
        this.name = name;
    }
    
    /**
     * Проверяет, валидна ли арена (настроены ли все необходимые точки)
     */
    public boolean isValid() {
        return teamASpawn != null && teamBSpawn != null && center != null && !capturePoints.isEmpty();
    }
    
    /**
     * Добавляет контрольную точку
     */
    public void addCapturePoint(CaptureFlag captureFlag) {
        capturePoints.add(captureFlag);
    }
    
    /**
     * Удаляет контрольную точку
     */
    public void removeCapturePoint(CaptureFlag captureFlag) {
        capturePoints.remove(captureFlag);
    }
    
    /**
     * Сбрасывает все контрольные точки в нейтральное состояние
     */
    public void resetCapturePoints() {
        for (CaptureFlag flag : capturePoints) {
            flag.reset();
        }
    }
    
    /**
     * Получает контрольную точку по индексу
     */
    public CaptureFlag getCapturePoint(int index) {
        if (index >= 0 && index < capturePoints.size()) {
            return capturePoints.get(index);
        }
        return null;
    }
    
    /**
     * Находит главный флаг команды (обычно это первый флаг на базе команды)
     */
    public CaptureFlag getTeamMainFlag(TeamType teamType) {
        for (CaptureFlag flag : capturePoints) {
            if (flag.isMainFlag() && flag.getDefaultTeam() == teamType) {
                return flag;
            }
        }
        
        
        for (CaptureFlag flag : capturePoints) {
            if (flag.getDefaultTeam() == teamType) {
                return flag;
            }
        }
        
        return null;
    }
    
    /**
     * Возвращает самую близкую к указанной локации контрольную точку
     */
    public CaptureFlag getNearestCapturePoint(Location location) {
        if (capturePoints.isEmpty()) {
            return null;
        }
        
        CaptureFlag nearest = capturePoints.get(0);
        double minDistance = nearest.getLocation().distance(location);
        
        for (int i = 1; i < capturePoints.size(); i++) {
            CaptureFlag flag = capturePoints.get(i);
            double distance = flag.getLocation().distance(location);
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = flag;
            }
        }
        
        return nearest;
    }
    
    
    
    public String getName() {
        return name;
    }
    
    public Location getTeamASpawn() {
        return teamASpawn;
    }
    
    public void setTeamASpawn(Location teamASpawn) {
        this.teamASpawn = teamASpawn;
    }
    
    public Location getTeamBSpawn() {
        return teamBSpawn;
    }
    
    public void setTeamBSpawn(Location teamBSpawn) {
        this.teamBSpawn = teamBSpawn;
    }
    
    public Location getCenter() {
        return center;
    }
    
    public void setCenter(Location center) {
        this.center = center;
    }
    
    public Location getLobbySpawn() {
        return lobbySpawn;
    }
    
    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }
    
    public List<CaptureFlag> getCapturePoints() {
        return capturePoints;
    }
    
    /**
     * Возвращает мир, в котором расположена арена
     */
    public org.bukkit.World getWorld() {
        if (center != null) {
            return center.getWorld();
        } else if (teamASpawn != null) {
            return teamASpawn.getWorld();
        } else if (teamBSpawn != null) {
            return teamBSpawn.getWorld();
        }
        return null;
    }
} 