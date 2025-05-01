package org.comm.battlefieldCommand.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.comm.battlefieldCommand.BattlefieldCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final BattlefieldCommand plugin;
    private FileConfiguration config;
    private FileConfiguration defaultConfig;
    private Map<String, FileConfiguration> configFiles;
    
    public ConfigManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
        this.configFiles = new HashMap<>();
        loadDefaultConfig();
        loadConfigs();
    }
    
    /**
     * Загружает основную конфигурацию из config.yml
     */
    private void loadDefaultConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
            defaultConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defaultConfigStream));
        }
    }
    
    /**
     * Загружает все дополнительные конфигурационные файлы
     */
    private void loadConfigs() {
        
        File configDir = new File(plugin.getDataFolder(), "configs");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        
        createConfigIfNotExists(configDir, "classes.yml");
        createConfigIfNotExists(configDir, "maps.yml");
        createConfigIfNotExists(configDir, "achievements.yml");
        createConfigIfNotExists(configDir, "voice.yml");
        
        
        loadConfig("classes", new File(configDir, "classes.yml"));
        loadConfig("maps", new File(configDir, "maps.yml"));
        loadConfig("achievements", new File(configDir, "achievements.yml"));
        loadConfig("voice", new File(configDir, "voice.yml"));
    }
    
    /**
     * Создает конфигурационный файл, если он не существует
     */
    private void createConfigIfNotExists(File directory, String fileName) {
        File configFile = new File(directory, fileName);
        if (!configFile.exists()) {
            InputStream resourceStream = plugin.getResource(fileName);
            if (resourceStream != null) {
                try {
                    Files.copy(resourceStream, configFile.toPath());
                } catch (IOException e) {
                    plugin.getLogger().severe("Не удалось создать конфигурационный файл " + fileName + ": " + e.getMessage());
                }
            } else {
                try {
                    configFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Не удалось создать конфигурационный файл " + fileName + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Загружает конфигурационный файл
     */
    private void loadConfig(String name, File file) {
        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            configFiles.put(name, config);
        } else {
            plugin.getLogger().warning("Файл конфигурации " + file.getName() + " не найден!");
        }
    }
    
    /**
     * Получает конфигурационный файл по имени
     */
    public FileConfiguration getConfig(String name) {
        return configFiles.getOrDefault(name, null);
    }
    
    /**
     * Получает основной конфигурационный файл
     */
    public FileConfiguration getMainConfig() {
        return config;
    }
    
    /**
     * Перезагружает все конфигурационные файлы
     */
    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadConfigs();
    }
    
    /**
     * Сохраняет конфигурационный файл
     */
    public void saveConfig(String name) {
        FileConfiguration config = configFiles.get(name);
        if (config != null) {
            File configDir = new File(plugin.getDataFolder(), "configs");
            File configFile = new File(configDir, name + ".yml");
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось сохранить конфигурацию " + name + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Сохраняет основной конфигурационный файл
     */
    public void saveMainConfig() {
        plugin.saveConfig();
    }
} 