package org.comm.battlefieldCommand.game.player;

import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.UUID;

/**
 * Представляет игрока в игре
 */
public class BCPlayer {
    private final UUID playerId;
    private final String playerName;
    private TeamType teamType;
    private ClassType classType;
    
    
    private int kills;
    private int deaths;
    private int assists;
    private int captures;
    private int killStreak;
    private int maxKillStreak;
    private int score;
    private int bestKillstreak; 
    
    
    private boolean waitingForRespawn;
    private boolean isInvisible;
    private int killstreakAbilityLevel; 
    
    
    private long lastCaptureNotificationTime = 0;
    
    
    private boolean selectingAirstrikeTarget = false;
    
    public BCPlayer(UUID playerId, String playerName, TeamType teamType, ClassType classType) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.teamType = teamType;
        this.classType = classType;
        this.kills = 0;
        this.deaths = 0;
        this.assists = 0;
        this.captures = 0;
        this.killStreak = 0;
        this.maxKillStreak = 0;
        this.score = 0;
        this.waitingForRespawn = false;
        this.isInvisible = false;
        this.killstreakAbilityLevel = 0;
    }
    
    /**
     * Добавляет убийство и обновляет серию убийств
     */
    public void addKill() {
        kills++;
        killStreak++;
        
        if (killStreak > maxKillStreak) {
            maxKillStreak = killStreak;
        }
        
        
        checkKillstreakRewards();
        
        
        score += 10; 
    }
    
    /**
     * Добавляет помощь в убийстве
     */
    public void addAssist() {
        assists++;
        score += 5; 
    }
    
    /**
     * Добавляет захват контрольной точки
     */
    public void addCapture() {
        captures++;
        score += 15; 
    }
    
    /**
     * Добавляет смерть и сбрасывает серию убийств
     */
    public void addDeath() {
        deaths++;
        killStreak = 0;
        killstreakAbilityLevel = 0;
    }
    
    /**
     * Проверяет и устанавливает награды за серию убийств
     */
    private void checkKillstreakRewards() {
        if (killStreak == 3 && killstreakAbilityLevel < 1) {
            killstreakAbilityLevel = 1;
        } else if (killStreak == 5 && killstreakAbilityLevel < 2) {
            killstreakAbilityLevel = 2;
        } else if (killStreak == 7 && killstreakAbilityLevel < 3) {
            killstreakAbilityLevel = 3;
        }
    }
    
    /**
     * Использует способность киллстрика и сбрасывает её уровень
     */
    public boolean useKillstreakAbility() {
        if (killstreakAbilityLevel > 0) {
            int level = killstreakAbilityLevel;
            killstreakAbilityLevel = 0;
            return true;
        }
        return false;
    }
    
    /**
     * Добавляет очки игроку
     */
    public void addPoints(int points) {
        this.score += points;
    }
    
    /**
     * Увеличивает счетчик смертей
     */
    public void incrementDeaths() {
        addDeath();
    }
    
    /**
     * Увеличивает счетчик убийств
     */
    public void incrementKills() {
        addKill();
    }
    
    /**
     * Получает максимальную серию убийств
     */
    public int getBestKillstreak() {
        return bestKillstreak;
    }
    
    /**
     * Устанавливает максимальную серию убийств
     */
    public void setBestKillstreak(int bestKillstreak) {
        this.bestKillstreak = bestKillstreak;
    }
    
    
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public TeamType getTeamType() {
        return teamType;
    }
    
    public void setTeamType(TeamType teamType) {
        this.teamType = teamType;
    }
    
    public ClassType getClassType() {
        return classType;
    }
    
    public void setClassType(ClassType classType) {
        this.classType = classType;
    }
    
    public int getKills() {
        return kills;
    }
    
    public int getDeaths() {
        return deaths;
    }
    
    public int getAssists() {
        return assists;
    }
    
    public int getCaptures() {
        return captures;
    }
    
    public int getKillStreak() {
        return killStreak;
    }
    
    public int getMaxKillStreak() {
        return maxKillStreak;
    }
    
    public int getScore() {
        return score;
    }
    
    public boolean isWaitingForRespawn() {
        return waitingForRespawn;
    }
    
    public void setWaitingForRespawn(boolean waitingForRespawn) {
        this.waitingForRespawn = waitingForRespawn;
    }
    
    public boolean isInvisible() {
        return isInvisible;
    }
    
    public void setInvisible(boolean invisible) {
        isInvisible = invisible;
    }
    
    public int getKillstreakAbilityLevel() {
        return killstreakAbilityLevel;
    }
    
    /**
     * Получает время последнего уведомления о захвате
     */
    public long getLastCaptureNotificationTime() {
        return lastCaptureNotificationTime;
    }
    
    /**
     * Устанавливает время последнего уведомления о захвате
     */
    public void setLastCaptureNotificationTime(long time) {
        this.lastCaptureNotificationTime = time;
    }
    
    /**
     * Проверяет, выбирает ли игрок цель для авиаудара
     */
    public boolean isSelectingAirstrikeTarget() {
        return selectingAirstrikeTarget;
    }
    
    /**
     * Устанавливает флаг выбора цели для авиаудара
     */
    public void setSelectingAirstrikeTarget(boolean selecting) {
        this.selectingAirstrikeTarget = selecting;
    }
} 