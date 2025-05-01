package org.comm.battlefieldCommand.game.player;

/**
 * Типы классов игроков
 */
public enum ClassType {
    /**
     * Штурмовик с штурмовой винтовкой, бронежилетом, гранатами и способностью к рывку
     */
    ASSAULT("assault", "Штурмовик"),
    
    /**
     * Снайпер с оптической винтовкой, камуфляжем, маскировочными метками и временной невидимостью
     */
    SNIPER("sniper", "Снайпер"),
    
    /**
     * Инженер с дробовиком, инструментами для ремонта турелей, минным комплектом и вызовом ремонтного дрона
     */
    ENGINEER("engineer", "Инженер");
    
    private final String id;
    private final String displayName;
    
    ClassType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    /**
     * Получает идентификатор класса
     */
    public String getId() {
        return id;
    }
    
    /**
     * Получает отображаемое имя класса
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Получает тип класса по строковому идентификатору
     */
    public static ClassType fromString(String id) {
        for (ClassType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return ASSAULT; 
    }
} 