package org.comm.battlefieldCommand.game.map;

import org.bukkit.Material;

/**
 * Представляет тему карты
 */
public class MapTheme {
    private final String id;
    private final Material mainMaterial;
    private final Material accentMaterial;
    private final Material floorMaterial;
    
    public MapTheme(String id, Material mainMaterial, Material accentMaterial, Material floorMaterial) {
        this.id = id;
        this.mainMaterial = mainMaterial;
        this.accentMaterial = accentMaterial;
        this.floorMaterial = floorMaterial;
    }
    
    /**
     * Получает идентификатор темы
     */
    public String getId() {
        return id;
    }
    
    /**
     * Получает основной материал темы
     */
    public Material getMainMaterial() {
        return mainMaterial;
    }
    
    /**
     * Получает акцентный материал темы
     */
    public Material getAccentMaterial() {
        return accentMaterial;
    }
    
    /**
     * Получает материал пола темы
     */
    public Material getFloorMaterial() {
        return floorMaterial;
    }
} 