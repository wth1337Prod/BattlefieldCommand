package org.comm.battlefieldCommand.game.map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.comm.battlefieldCommand.BattlefieldCommand;

import java.util.HashMap;
import java.util.Map;

/**
 * Управляет темами карт
 */
public class MapThemeManager {
    private final BattlefieldCommand plugin;
    private final Map<String, MapTheme> themes = new HashMap<>();
    
    
    private static final MapTheme DEFAULT_THEME = new MapTheme(
            "default",
            Material.STONE_BRICKS,
            Material.COBBLESTONE,
            Material.STONE
    );
    
    public MapThemeManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
        loadThemes();
    }
    
    /**
     * Загружает темы из конфигурации
     */
    private void loadThemes() {
        
        themes.put(DEFAULT_THEME.getId(), DEFAULT_THEME);
        
        
        ConfigurationSection config = plugin.getConfigManager().getConfig("maps");
        if (config == null) return;
        
        ConfigurationSection themesConfig = config.getConfigurationSection("generator.themes");
        if (themesConfig == null) return;
        
        for (String themeId : themesConfig.getKeys(false)) {
            ConfigurationSection themeSection = themesConfig.getConfigurationSection(themeId);
            if (themeSection == null) continue;
            
            Material mainMaterial = getMaterial(themeSection.getString("main_material", "STONE_BRICKS"));
            Material accentMaterial = getMaterial(themeSection.getString("accent_material", "COBBLESTONE"));
            Material floorMaterial = getMaterial(themeSection.getString("floor_material", "STONE"));
            
            MapTheme theme = new MapTheme(themeId, mainMaterial, accentMaterial, floorMaterial);
            themes.put(themeId, theme);
        }
    }
    
    /**
     * Получает тему по идентификатору
     */
    public MapTheme getTheme(String themeId) {
        return themes.getOrDefault(themeId, DEFAULT_THEME);
    }
    
    /**
     * Получает материал из строки
     */
    private Material getMaterial(String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверный материал: " + materialName + ". Используется STONE_BRICKS");
            return Material.STONE_BRICKS;
        }
    }
} 