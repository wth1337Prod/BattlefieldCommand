package org.comm.battlefieldCommand.commands;

import org.bukkit.command.PluginCommand;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.commands.executors.BattlefieldCommandExecutor;
import org.comm.battlefieldCommand.commands.executors.RadioCommandExecutor;
import org.comm.battlefieldCommand.commands.executors.VoiceCommandExecutor;

public class CommandManager {
    private final BattlefieldCommand plugin;
    
    public CommandManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Регистрирует все команды плагина
     */
    public void registerCommands() {
        
        PluginCommand bcCommand = plugin.getCommand("bc");
        if (bcCommand != null) {
            BattlefieldCommandExecutor executor = new BattlefieldCommandExecutor(plugin);
            bcCommand.setExecutor(executor);
            bcCommand.setTabCompleter(executor);
            plugin.getLogger().info("Команда /bc успешно зарегистрирована!");
        } else {
            plugin.getLogger().severe("Не удалось зарегистрировать команду /bc. Проверьте файл plugin.yml!");
        }
        
        
        PluginCommand voiceCommand = plugin.getCommand("voice");
        if (voiceCommand != null) {
            VoiceCommandExecutor executor = new VoiceCommandExecutor(plugin, plugin.getVoiceManager());
            voiceCommand.setExecutor(executor);
            voiceCommand.setTabCompleter(executor);
            plugin.getLogger().info("Команда /voice успешно зарегистрирована!");
        } else {
            plugin.getLogger().severe("Не удалось зарегистрировать команду /voice. Проверьте файл plugin.yml!");
        }
        
        
        PluginCommand radioCommand = plugin.getCommand("radio");
        if (radioCommand != null) {
            RadioCommandExecutor executor = new RadioCommandExecutor(plugin, plugin.getRadioMessageSystem());
            radioCommand.setExecutor(executor);
            radioCommand.setTabCompleter(executor);
            plugin.getLogger().info("Команда /radio успешно зарегистрирована!");
        } else {
            plugin.getLogger().severe("Не удалось зарегистрировать команду /radio. Проверьте файл plugin.yml!");
        }
    }
} 