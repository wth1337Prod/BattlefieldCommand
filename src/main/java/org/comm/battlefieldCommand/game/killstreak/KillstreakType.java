package org.comm.battlefieldCommand.game.killstreak;

/**
 * Типы наград за серию убийств
 */
public enum KillstreakType {
    /**
     * Разведывательный дрон (UAV) - доступен при 3 убийствах
     */
    RECON_DRONE("Разведывательный дрон"),
    
    /**
     * Усиленный бронежилет - доступен при 5 убийствах
     */
    ENHANCED_ARMOR("Усиленный бронежилет"),
    
    /**
     * Авиаудар - доступен при 7 убийствах
     */
    AIR_STRIKE("Авиаудар"),
    
    /**
     * Скан вражеских позиций - доступен при 3 убийствах
     */
    UAV_SCAN("UAV Сканирование"),
    
    /**
     * Увеличение брони - доступен при 5 убийствах
     */
    ARMOR_BOOST("Усиление брони");
    
    private final String displayName;
    
    KillstreakType(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Получает отображаемое название киллстрика
     */
    public String getDisplayName() {
        return displayName;
    }
} 