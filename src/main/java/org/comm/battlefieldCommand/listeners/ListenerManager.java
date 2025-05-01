package org.comm.battlefieldCommand.listeners;

import org.bukkit.plugin.PluginManager;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.GameListener;
import org.comm.battlefieldCommand.listeners.player.PlayerListener;

public class ListenerManager {
    private final BattlefieldCommand plugin;
    
    public ListenerManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Регистрирует все слушатели событий
     */
    public void registerListeners() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        
        
        pluginManager.registerEvents(new PlayerListener(plugin), plugin);
        pluginManager.registerEvents(new GameListener(plugin), plugin);
        
        plugin.getLogger().info("Слушатели событий успешно зарегистрированы!");
    }
} 