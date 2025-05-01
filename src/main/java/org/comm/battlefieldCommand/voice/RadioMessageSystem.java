package org.comm.battlefieldCommand.voice;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.GameManager;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система радиосообщений для быстрой коммуникации в команде
 * Позволяет отправлять заранее определенные сообщения одним нажатием клавиши
 */
public class RadioMessageSystem {
    private final BattlefieldCommand plugin;
    private final GameManager gameManager;
    private final VoiceManager voiceManager;
    
    
    private final Map<UUID, Long> lastCommandTime;
    
    
    private static final long COMMAND_COOLDOWN = 3000;
    
    
    private final Map<String, RadioCommand> radioCommands;
    
    public RadioMessageSystem(BattlefieldCommand plugin, VoiceManager voiceManager) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.voiceManager = voiceManager;
        this.lastCommandTime = new ConcurrentHashMap<>();
        this.radioCommands = new HashMap<>();
        initRadioCommands();
    }
    
    /**
     * Инициализирует стандартный набор радиокоманд
     */
    private void initRadioCommands() {
        
        radioCommands.put("attack_a", new RadioCommand(
            "attacka", 
            "Атакуем точку А!",
            ChatColor.RED + "[РАДИО] %s: Атакуем точку А!",
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            (player, args) -> markObjectiveLocation(player, "A")));
            
        radioCommands.put("attack_b", new RadioCommand(
            "attackb", 
            "Атакуем точку Б!",
            ChatColor.RED + "[РАДИО] %s: Атакуем точку Б!",
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            (player, args) -> markObjectiveLocation(player, "B")));
            
        radioCommands.put("attack_c", new RadioCommand(
            "attackc", 
            "Атакуем точку С!",
            ChatColor.RED + "[РАДИО] %s: Атакуем точку С!",
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            (player, args) -> markObjectiveLocation(player, "C")));
            
        radioCommands.put("defend_a", new RadioCommand(
            "defenda", 
            "Защищаем точку А!",
            ChatColor.BLUE + "[РАДИО] %s: Защищаем точку А!",
            Sound.BLOCK_NOTE_BLOCK_PLING,
            (player, args) -> markObjectiveLocation(player, "A")));
            
        radioCommands.put("defend_b", new RadioCommand(
            "defendb", 
            "Защищаем точку Б!",
            ChatColor.BLUE + "[РАДИО] %s: Защищаем точку Б!",
            Sound.BLOCK_NOTE_BLOCK_PLING,
            (player, args) -> markObjectiveLocation(player, "B")));
            
        radioCommands.put("defend_c", new RadioCommand(
            "defendc", 
            "Защищаем точку С!",
            ChatColor.BLUE + "[РАДИО] %s: Защищаем точку С!",
            Sound.BLOCK_NOTE_BLOCK_PLING,
            (player, args) -> markObjectiveLocation(player, "C")));
            
        
        radioCommands.put("need_support", new RadioCommand(
            "support", 
            "Нужна поддержка!",
            ChatColor.GOLD + "[РАДИО] %s: Нужна поддержка!",
            Sound.ENTITY_PLAYER_LEVELUP,
            (player, args) -> markPlayerLocation(player)));
            
        radioCommands.put("need_ammo", new RadioCommand(
            "ammo", 
            "Нужны боеприпасы!",
            ChatColor.YELLOW + "[РАДИО] %s: Нужны боеприпасы!",
            Sound.BLOCK_CHEST_OPEN,
            (player, args) -> markPlayerLocation(player)));
            
        radioCommands.put("need_medic", new RadioCommand(
            "medic", 
            "Нужен медик!",
            ChatColor.GREEN + "[РАДИО] %s: Нужен медик!",
            Sound.BLOCK_BREWING_STAND_BREW,
            (player, args) -> markPlayerLocation(player)));
            
        radioCommands.put("need_repair", new RadioCommand(
            "repair", 
            "Нужен ремонт!",
            ChatColor.AQUA + "[РАДИО] %s: Нужен ремонт!",
            Sound.BLOCK_ANVIL_USE,
            (player, args) -> markPlayerLocation(player)));
            
        
        radioCommands.put("affirmative", new RadioCommand(
            "yes", 
            "Понял, выполняю!",
            ChatColor.GREEN + "[РАДИО] %s: Понял, выполняю!",
            Sound.ENTITY_VILLAGER_YES,
            null));
            
        radioCommands.put("negative", new RadioCommand(
            "no", 
            "Отрицательно, не могу выполнить!",
            ChatColor.RED + "[РАДИО] %s: Отрицательно, не могу выполнить!",
            Sound.ENTITY_VILLAGER_NO,
            null));
            
        radioCommands.put("enemy_spotted", new RadioCommand(
            "enemy", 
            "Обнаружен противник!",
            ChatColor.RED + "[РАДИО] %s: Обнаружен противник!",
            Sound.ENTITY_ARROW_HIT_PLAYER,
            (player, args) -> markPlayerLocation(player)));
            
        radioCommands.put("area_clear", new RadioCommand(
            "clear", 
            "Зона чиста!",
            ChatColor.GREEN + "[РАДИО] %s: Зона чиста!",
            Sound.BLOCK_NOTE_BLOCK_CHIME,
            (player, args) -> markPlayerLocation(player)));
    }
    
    /**
     * Обрабатывает команду радиосвязи от игрока
     */
    public boolean handleRadioCommand(Player player, String commandId, String[] args) {
        if (!gameManager.isPlayerInGame(player)) {
            player.sendMessage(ChatColor.RED + "Вы должны быть в игре, чтобы использовать радиокоманды!");
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        
        long currentTime = System.currentTimeMillis();
        if (lastCommandTime.containsKey(playerUuid)) {
            long lastTime = lastCommandTime.get(playerUuid);
            if (currentTime - lastTime < COMMAND_COOLDOWN) {
                player.sendMessage(ChatColor.RED + "Подождите перед отправкой следующего сообщения!");
                return true;
            }
        }
        
        
        RadioCommand command = radioCommands.get(commandId.toLowerCase());
        if (command == null) {
            player.sendMessage(ChatColor.RED + "Неизвестная радиокоманда!");
            return false;
        }
        
        
        lastCommandTime.put(playerUuid, currentTime);
        
        
        TeamType team = gameManager.getPlayerTeam(player);
        if (team != null) {
            String formattedMessage = String.format(command.getMessageFormat(), player.getName());
            broadcastToTeam(team, formattedMessage, command.getSound());
            
            
            if (command.getAction() != null) {
                command.getAction().execute(player, args);
            }
        }
        
        return true;
    }
    
    /**
     * Отправляет сообщение всей команде
     */
    private void broadcastToTeam(TeamType team, String message, Sound sound) {
        for (Player teamPlayer : gameManager.getTeamManager().getTeamPlayers(team)) {
            teamPlayer.sendMessage(message);
            if (sound != null) {
                teamPlayer.playSound(teamPlayer.getLocation(), sound, 0.5f, 1.0f);
            }
        }
    }
    
    /**
     * Отмечает текущую позицию игрока для команды
     */
    private void markPlayerLocation(Player player) {
        TeamType team = gameManager.getPlayerTeam(player);
        Location location = player.getLocation();
        
        
        String message = ChatColor.YELLOW + "[МЕТКА] " + player.getName() + 
                " отметил позицию: X:" + location.getBlockX() + 
                ", Y:" + location.getBlockY() + 
                ", Z:" + location.getBlockZ();

        for (Player teamPlayer : gameManager.getTeamManager().getTeamPlayers(team)) {
            teamPlayer.sendMessage(message);
        }
    }
    
    /**
     * Отмечает местоположение указанной контрольной точки
     */
    private void markObjectiveLocation(Player player, String objectiveId) {
        
        
        TeamType team = gameManager.getPlayerTeam(player);
        
        String message = ChatColor.YELLOW + "[МЕТКА] " + player.getName() + 
                " отметил контрольную точку " + objectiveId;

        for (Player teamPlayer : gameManager.getTeamManager().getTeamPlayers(team)) {
            teamPlayer.sendMessage(message);
        }
    }
    
    /**
     * Возвращает список доступных радиокоманд
     */
    public List<String> getRadioCommandIds() {
        return new ArrayList<>(radioCommands.keySet());
    }
    
    /**
     * Внутренний класс, представляющий радиокоманду
     */
    private static class RadioCommand {
        private final String id;
        private final String description;
        private final String messageFormat;
        private final Sound sound;
        private final RadioAction action;
        
        public RadioCommand(String id, String description, String messageFormat, Sound sound, RadioAction action) {
            this.id = id;
            this.description = description;
            this.messageFormat = messageFormat;
            this.sound = sound;
            this.action = action;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getMessageFormat() {
            return messageFormat;
        }
        
        public Sound getSound() {
            return sound;
        }
        
        public RadioAction getAction() {
            return action;
        }
    }
    
    /**
     * Интерфейс для действий, связанных с радиокомандами
     */
    @FunctionalInterface
    private interface RadioAction {
        void execute(Player player, String[] args);
    }
    
    /**
     * Регистрирует слушателей событий для радиосистемы
     */
    public void register(de.maxhenkel.voicechat.api.events.EventRegistration registration) {
        
        plugin.getLogger().info("Регистрация обработчиков событий радиосвязи");
    }
} 