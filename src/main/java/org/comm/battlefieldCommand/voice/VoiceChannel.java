package org.comm.battlefieldCommand.voice;

/**
 * Перечисление каналов голосового чата
 */
public enum VoiceChannel {
    /**
     * Канал команды A
     */
    TEAM_A("team_a", "Команда А"),
    
    /**
     * Канал команды B
     */
    TEAM_B("team_b", "Команда Б"),
    
    /**
     * Тактический канал для координации
     */
    TACTICAL("tactical", "Тактический канал"),
    
    /**
     * Общий канал для всех игроков
     */
    ALL("all", "Общий канал");
    
    private final String id;
    private final String displayName;
    
    VoiceChannel(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    /**
     * Получает идентификатор канала
     */
    public String getId() {
        return id;
    }
    
    /**
     * Получает отображаемое имя канала
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Получает канал по идентификатору
     */
    public static VoiceChannel fromString(String id) {
        for (VoiceChannel channel : values()) {
            if (channel.getId().equalsIgnoreCase(id)) {
                return channel;
            }
        }
        return ALL; 
    }
} 