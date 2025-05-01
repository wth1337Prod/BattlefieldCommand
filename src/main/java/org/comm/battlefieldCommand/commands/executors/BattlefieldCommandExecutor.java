package org.comm.battlefieldCommand.commands.executors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.GameManager;
import org.comm.battlefieldCommand.game.player.ClassType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BattlefieldCommandExecutor implements CommandExecutor, TabCompleter {
    private final BattlefieldCommand plugin;
    private final GameManager gameManager;
    
    private final List<String> subCommands = Arrays.asList(
            "setup", "join", "leave", "start", "stop", "restart", 
            "config", "help", "info", "class", "spectate"
    );
    
    private final List<String> adminCommands = Arrays.asList(
            "setup", "start", "stop", "restart", "config"
    );
    
    private final List<String> classTypes = Arrays.asList(
            "assault", "sniper", "engineer"
    );
    
    public BattlefieldCommandExecutor(BattlefieldCommand plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        
        if (adminCommands.contains(subCommand) && !sender.hasPermission("bc.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас недостаточно прав для выполнения этой команды!");
            return true;
        }
        
        switch (subCommand) {
            case "setup":
                return handleSetupCommand(sender, args);
            case "join":
                return handleJoinCommand(sender, args);
            case "leave":
                return handleLeaveCommand(sender);
            case "start":
                return handleStartCommand(sender);
            case "stop":
                return handleStopCommand(sender);
            case "restart":
                return handleRestartCommand(sender);
            case "config":
                return handleConfigCommand(sender, args);
            case "help":
                return handleHelpCommand(sender, args);
            case "info":
                return handleInfoCommand(sender);
            case "class":
                return handleClassCommand(sender, args);
            case "spectate":
                return handleSpectateCommand(sender);
            default:
                sender.sendMessage(ChatColor.RED + "Неизвестная команда! Используйте /bc help для получения списка команд.");
                return true;
        }
    }
    
    private boolean handleSetupCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда может быть использована только игроком!");
            return true;
        }
        
        Player player = (Player) sender;
        
        
        gameManager.setupArena(player);
        return true;
    }
    
    private boolean handleJoinCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда может быть использована только игроком!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length > 1) {
            
            String classTypeStr = args[1].toLowerCase();
            if (!classTypes.contains(classTypeStr)) {
                player.sendMessage(ChatColor.RED + "Неизвестный класс! Доступные классы: assault, sniper, engineer");
                return true;
            }
            
            ClassType classType = ClassType.fromString(classTypeStr);
            gameManager.joinGame(player, classType);
        } else {
            
            gameManager.joinGame(player, ClassType.ASSAULT);
        }
        
        return true;
    }
    
    private boolean handleLeaveCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда может быть использована только игроком!");
            return true;
        }
        
        Player player = (Player) sender;
        gameManager.leaveGame(player);
        return true;
    }
    
    private boolean handleStartCommand(CommandSender sender) {
        if (!gameManager.hasArenaSetup()) {
            sender.sendMessage(ChatColor.RED + "Арена еще не настроена! Используйте /bc setup для настройки.");
            return true;
        }
        
        gameManager.startGame();
        sender.sendMessage(ChatColor.GREEN + "Игра запущена!");
        return true;
    }
    
    private boolean handleStopCommand(CommandSender sender) {
        if (!gameManager.isGameActive()) {
            sender.sendMessage(ChatColor.RED + "Нет активной игры!");
            return true;
        }
        
        gameManager.stopGame();
        sender.sendMessage(ChatColor.GREEN + "Игра остановлена!");
        return true;
    }
    
    private boolean handleRestartCommand(CommandSender sender) {
        gameManager.restartGame();
        sender.sendMessage(ChatColor.GREEN + "Игра перезапущена!");
        return true;
    }
    
    private boolean handleConfigCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Используйте: /bc config reload");
            return true;
        }
        
        if (args[1].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().reloadConfigs();
            sender.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена!");
        } else {
            sender.sendMessage(ChatColor.RED + "Неизвестная подкоманда! Используйте: /bc config reload");
        }
        
        return true;
    }
    
    private boolean handleHelpCommand(CommandSender sender, String[] args) {
        sendHelpMessage(sender);
        return true;
    }
    
    private boolean handleInfoCommand(CommandSender sender) {
        if (!gameManager.isGameActive()) {
            sender.sendMessage(ChatColor.RED + "Нет активной игры!");
            return true;
        }
        
        gameManager.sendGameInfo(sender);
        return true;
    }
    
    private boolean handleClassCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда может быть использована только игроком!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Используйте: /bc class <класс>");
            player.sendMessage(ChatColor.RED + "Доступные классы: assault, sniper, engineer");
            return true;
        }
        
        String classTypeStr = args[1].toLowerCase();
        if (!classTypes.contains(classTypeStr)) {
            player.sendMessage(ChatColor.RED + "Неизвестный класс! Доступные классы: assault, sniper, engineer");
            return true;
        }
        
        if (!gameManager.isPlayerInGame(player)) {
            player.sendMessage(ChatColor.RED + "Вы не участвуете в игре! Используйте /bc join для присоединения.");
            return true;
        }
        
        ClassType classType = ClassType.fromString(classTypeStr);
        gameManager.changePlayerClass(player, classType);
        return true;
    }
    
    private boolean handleSpectateCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда может быть использована только игроком!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!gameManager.isGameActive()) {
            player.sendMessage(ChatColor.RED + "Нет активной игры для наблюдения!");
            return true;
        }
        
        gameManager.enterSpectatorMode(player);
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Battlefield Command ===");
        sender.sendMessage(ChatColor.YELLOW + "/bc setup " + ChatColor.WHITE + "- Создать новую арену для сражений");
        sender.sendMessage(ChatColor.YELLOW + "/bc join [класс] " + ChatColor.WHITE + "- Присоединиться к игре");
        sender.sendMessage(ChatColor.YELLOW + "/bc leave " + ChatColor.WHITE + "- Покинуть игру");
        sender.sendMessage(ChatColor.YELLOW + "/bc class <класс> " + ChatColor.WHITE + "- Сменить класс");
        sender.sendMessage(ChatColor.YELLOW + "/bc spectate " + ChatColor.WHITE + "- Наблюдать за игрой");
        sender.sendMessage(ChatColor.YELLOW + "/bc info " + ChatColor.WHITE + "- Информация о текущей игре");
        
        if (sender.hasPermission("bc.admin")) {
            sender.sendMessage(ChatColor.GOLD + "=== Команды администратора ===");
            sender.sendMessage(ChatColor.YELLOW + "/bc start " + ChatColor.WHITE + "- Запустить игру");
            sender.sendMessage(ChatColor.YELLOW + "/bc stop " + ChatColor.WHITE + "- Остановить игру");
            sender.sendMessage(ChatColor.YELLOW + "/bc restart " + ChatColor.WHITE + "- Перезапустить игру");
            sender.sendMessage(ChatColor.YELLOW + "/bc config reload " + ChatColor.WHITE + "- Перезагрузить конфигурацию");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            
            String partialCommand = args[0].toLowerCase();
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partialCommand)) {
                    
                    if (adminCommands.contains(subCommand) && !sender.hasPermission("bc.admin")) {
                        continue;
                    }
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            
            String subCommand = args[0].toLowerCase();
            String partialArg = args[1].toLowerCase();
            
            if (subCommand.equals("join") || subCommand.equals("class")) {
                
                for (String classType : classTypes) {
                    if (classType.startsWith(partialArg)) {
                        completions.add(classType);
                    }
                }
            } else if (subCommand.equals("config") && sender.hasPermission("bc.admin")) {
                
                if ("reload".startsWith(partialArg)) {
                    completions.add("reload");
                }
            }
        }
        
        return completions;
    }
} 