package org.comm.battlefieldCommand.commands.executors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.voice.RadioMessageSystem;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Исполнитель команды для радиосообщений
 */
public class RadioCommandExecutor implements CommandExecutor, TabCompleter {
    private final BattlefieldCommand plugin;
    private final RadioMessageSystem radioSystem;
    
    public RadioCommandExecutor(BattlefieldCommand plugin, RadioMessageSystem radioSystem) {
        this.plugin = plugin;
        this.radioSystem = radioSystem;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда может быть использована только игроком!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            
            player.sendMessage(ChatColor.GOLD + "=== Радиокоманды ===");
            player.sendMessage(ChatColor.YELLOW + "Использование: /radio <команда> [аргументы]");
            player.sendMessage(ChatColor.YELLOW + "Для просмотра доступных команд введите: /radio list");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("list")) {
            
            player.sendMessage(ChatColor.GOLD + "=== Доступные радиокоманды ===");
            
            for (String commandId : radioSystem.getRadioCommandIds()) {
                player.sendMessage(ChatColor.YELLOW + "• " + commandId.replace("_", " "));
            }
            
            return true;
        }
        
        
        String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);
        return radioSystem.handleRadioCommand(player, subCommand, cmdArgs);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = radioSystem.getRadioCommandIds();
            completions.add("list");
            
            String partialCommand = args[0].toLowerCase();
            return completions.stream()
                    .filter(cmd -> cmd.startsWith(partialCommand))
                    .collect(Collectors.toList());
        }
        
        return List.of();
    }
} 