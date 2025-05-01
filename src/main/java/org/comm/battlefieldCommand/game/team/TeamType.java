package org.comm.battlefieldCommand.game.team;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;

/**
 * Типы команд
 */
public enum TeamType {
    /**
     * Команда A (красная)
     */
    TEAM_A("A", "Команда A", ChatColor.RED, Color.RED, DyeColor.RED),
    
    /**
     * Команда B (синяя)
     */
    TEAM_B("B", "Команда B", ChatColor.BLUE, Color.BLUE, DyeColor.BLUE),
    
    /**
     * Нейтральная команда (серая)
     */
    NEUTRAL("N", "Нейтральная", ChatColor.GRAY, Color.GRAY, DyeColor.GRAY);
    
    private final String id;
    private final String displayName;
    private final ChatColor chatColor;
    private final Color color;
    private final DyeColor dyeColor;
    
    TeamType(String id, String displayName, ChatColor chatColor, Color color, DyeColor dyeColor) {
        this.id = id;
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.color = color;
        this.dyeColor = dyeColor;
    }
    
    /**
     * Получает идентификатор команды
     */
    public String getId() {
        return id;
    }
    
    /**
     * Получает отображаемое имя команды
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Получает цвет чата команды
     */
    public ChatColor getChatColor() {
        return chatColor;
    }
    
    /**
     * Получает цвет команды
     */
    public Color getColor() {
        return color;
    }
    
    /**
     * Получает цвет красителя команды
     */
    public DyeColor getDyeColor() {
        return dyeColor;
    }
    
    /**
     * Получает тип команды по строковому идентификатору
     */
    public static TeamType fromString(String id) {
        for (TeamType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return NEUTRAL; 
    }
    
    /**
     * Получает противоположную команду
     */
    public TeamType getOpposite() {
        if (this == TEAM_A) {
            return TEAM_B;
        } else if (this == TEAM_B) {
            return TEAM_A;
        }
        return NEUTRAL;
    }
} 