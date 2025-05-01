package org.comm.battlefieldCommand.commands.executors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.voice.VoiceChannel;
import org.comm.battlefieldCommand.voice.VoiceManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Исполнитель команды для управления голосовым чатом
 */
public class VoiceCommandExecutor implements CommandExecutor, TabCompleter {
    private final BattlefieldCommand plugin;
    private final VoiceManager voiceManager;
    
    private final List<String> subCommands = Arrays.asList(
            "channel", "toggle", "help", "status"
    );
    
    private final List<String> channelNames = Arrays.asList(
            "team", "tactical", "all"
    );
    
    public VoiceCommandExecutor(BattlefieldCommand plugin, VoiceManager voiceManager) {
        this.plugin = plugin;
        this.voiceManager = voiceManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда может быть использована только игроком!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!voiceManager.isApiAvailable()) {
            player.sendMessage(ChatColor.RED + "Голосовой чат не доступен на этом сервере!");
            return true;
        }
        
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "channel":
                return handleChannelCommand(player, args);
            case "toggle":
                return handleToggleCommand(player);
            case "status":
                return handleStatusCommand(player);
            default:
                player.sendMessage(ChatColor.RED + "Неизвестная команда! Используйте /voice help для получения списка команд.");
                return true;
        }
    }
    
    /**
     * Обрабатывает команду переключения канала
     */
    private boolean handleChannelCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Используйте: /voice channel <channel>");
            player.sendMessage(ChatColor.RED + "Доступные каналы: team, tactical, all");
            return true;
        }
        
        String channelName = args[1].toLowerCase();
        VoiceChannel channel;
        
        switch (channelName) {
            case "team":
                
                channel = null; 
                voiceManager.assignPlayerToVoiceChannel(player);
                break;
            case "tactical":
                channel = VoiceChannel.TACTICAL;
                voiceManager.setPlayerVoiceChannel(player, channel);
                break;
            case "all":
                channel = VoiceChannel.ALL;
                voiceManager.setPlayerVoiceChannel(player, channel);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Неизвестный канал! Доступные каналы: team, tactical, all");
                return true;
        }
        
        return true;
    }
    
    /**
     * Обрабатывает команду переключения канала (циклически)
     */
    private boolean handleToggleCommand(Player player) {
        voiceManager.cyclePlayerVoiceChannel(player);
        return true;
    }
    
    /**
     * Обрабатывает команду запроса статуса голосового чата
     */
    private boolean handleStatusCommand(Player player) {
        if (!voiceManager.isApiAvailable()) {
            player.sendMessage(ChatColor.RED + "Голосовой чат не доступен на этом сервере!");
            return true;
        }
        
        player.sendMessage(ChatColor.GREEN + "Голосовой чат активен. Используйте /voice channel для переключения канала.");
        return true;
    }
    
    /**
     * Отправляет сообщение помощи
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Команды голосового чата ===");
        player.sendMessage(ChatColor.YELLOW + "/voice channel <channel> - Переключиться на указанный канал");
        player.sendMessage(ChatColor.YELLOW + "/voice toggle - Циклически переключать между каналами");
        player.sendMessage(ChatColor.YELLOW + "/voice status - Проверить статус голосового чата");
        player.sendMessage(ChatColor.YELLOW + "/voice help - Показать это сообщение");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partialCommand = args[0].toLowerCase();
            return subCommands.stream()
                    .filter(cmd -> cmd.startsWith(partialCommand))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("channel")) {
            String partialChannel = args[1].toLowerCase();
            return channelNames.stream()
                    .filter(channel -> channel.startsWith(partialChannel))
                    .collect(Collectors.toList());
        }
        
        return List.of();
    }
} 