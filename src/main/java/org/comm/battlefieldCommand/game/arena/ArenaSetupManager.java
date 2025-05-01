package org.comm.battlefieldCommand.game.arena;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.arena.capture.CaptureFlag;
import org.comm.battlefieldCommand.game.map.MapGenerator;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Управляет процессом настройки арены
 */
public class ArenaSetupManager {
    private final BattlefieldCommand plugin;
    private final Map<UUID, SetupSession> setupSessions = new HashMap<>();
    
    
    private final ItemStack teamASpawnTool;
    private final ItemStack teamBSpawnTool;
    private final ItemStack centerTool;
    private final ItemStack flagTool;
    private final ItemStack lobbyTool;
    private final ItemStack saveTool;
    private final ItemStack generateTool;
    
    public ArenaSetupManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
        
        
        teamASpawnTool = createTool(Material.RED_WOOL, ChatColor.RED + "Точка спавна команды A", "Клик: установить спавн команды A");
        teamBSpawnTool = createTool(Material.BLUE_WOOL, ChatColor.BLUE + "Точка спавна команды B", "Клик: установить спавн команды B");
        centerTool = createTool(Material.BEACON, ChatColor.GOLD + "Центр арены", "Клик: установить центр арены");
        flagTool = createTool(Material.YELLOW_BANNER, ChatColor.YELLOW + "Контрольная точка", "Клик: добавить контрольную точку");
        lobbyTool = createTool(Material.EMERALD, ChatColor.GREEN + "Точка лобби", "Клик: установить лобби");
        saveTool = createTool(Material.NETHER_STAR, ChatColor.AQUA + "Сохранить арену", "Клик: сохранить настройки арены");
        generateTool = createTool(Material.COMPASS, ChatColor.LIGHT_PURPLE + "Сгенерировать арену", "Клик: открыть меню генерации");
    }
    
    /**
     * Создает инструмент для настройки арены
     */
    private ItemStack createTool(Material material, String name, String... lore) {
        ItemStack tool = new ItemStack(material);
        ItemMeta meta = tool.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(ChatColor.GRAY + line);
                }
                meta.setLore(loreList);
            }
            
            tool.setItemMeta(meta);
        }
        return tool;
    }
    
    /**
     * Начинает процесс настройки арены для игрока
     */
    public void startSetup(Player player) {
        if (setupSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Вы уже настраиваете арену!");
            return;
        }
        
        
        SetupSession session = new SetupSession();
        setupSessions.put(player.getUniqueId(), session);
        
        
        session.saveInventory(player);
        
        
        player.getInventory().clear();
        player.getInventory().setItem(0, teamASpawnTool);
        player.getInventory().setItem(1, teamBSpawnTool);
        player.getInventory().setItem(2, centerTool);
        player.getInventory().setItem(3, flagTool);
        player.getInventory().setItem(4, lobbyTool);
        player.getInventory().setItem(7, generateTool);
        player.getInventory().setItem(8, saveTool);
        
        player.sendMessage(ChatColor.GREEN + "Режим настройки арены активирован!");
        player.sendMessage(ChatColor.YELLOW + "Используйте предметы в инвентаре для настройки различных точек арены.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Или используйте автоматическую генерацию арены с помощью соответствующего предмета.");
    }
    
    /**
     * Завершает процесс настройки арены для игрока
     */
    public void stopSetup(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!setupSessions.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "Вы не настраиваете арену!");
            return;
        }
        
        
        SetupSession session = setupSessions.get(playerId);
        session.restoreInventory(player);
        
        
        setupSessions.remove(playerId);
        
        player.sendMessage(ChatColor.GREEN + "Режим настройки арены деактивирован!");
    }
    
    /**
     * Обрабатывает клик по блоку при настройке арены
     */
    public boolean handleBlockClick(Player player, Location location, ItemStack tool) {
        UUID playerId = player.getUniqueId();
        
        if (!setupSessions.containsKey(playerId)) {
            return false;
        }
        
        SetupSession session = setupSessions.get(playerId);
        
        if (tool == null) {
            return false;
        }
        
        if (!tool.hasItemMeta() || !tool.getItemMeta().hasDisplayName()) {
            return false;
        }
        
        String toolName = tool.getItemMeta().getDisplayName();
        
        
        if (toolName.equals(teamASpawnTool.getItemMeta().getDisplayName())) {
            session.setTeamASpawn(location);
            player.sendMessage(ChatColor.GREEN + "Точка спавна команды A установлена!");
            return true;
        } else if (toolName.equals(teamBSpawnTool.getItemMeta().getDisplayName())) {
            session.setTeamBSpawn(location);
            player.sendMessage(ChatColor.GREEN + "Точка спавна команды B установлена!");
            return true;
        } else if (toolName.equals(centerTool.getItemMeta().getDisplayName())) {
            session.setCenter(location);
            player.sendMessage(ChatColor.GREEN + "Центр арены установлен!");
            return true;
        } else if (toolName.equals(flagTool.getItemMeta().getDisplayName())) {
            
            session.setLastClickedLocation(location);
            promptForFlagInfo(player);
            return true;
        } else if (toolName.equals(lobbyTool.getItemMeta().getDisplayName())) {
            session.setLobbySpawn(location);
            player.sendMessage(ChatColor.GREEN + "Точка лобби установлена!");
            return true;
        } else if (toolName.equals(saveTool.getItemMeta().getDisplayName())) {
            saveArena(player);
            return true;
        } else if (toolName.equals(generateTool.getItemMeta().getDisplayName())) {
            promptForMapGeneration(player, location);
            return true;
        }
        
        return false;
    }
    
    /**
     * Запрашивает информацию о контрольной точке
     */
    private void promptForFlagInfo(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Выберите команду для контрольной точки:");
        player.sendMessage(ChatColor.RED + "1. Команда A");
        player.sendMessage(ChatColor.BLUE + "2. Команда B");
        player.sendMessage(ChatColor.GRAY + "3. Нейтральная");
        player.sendMessage(ChatColor.YELLOW + "Введите в чат номер команды и имя точки (например: 1 База A)");
    }
    
    /**
     * Обрабатывает сообщение игрока при настройке флага
     */
    public boolean handleFlagInfoMessage(Player player, String message) {
        UUID playerId = player.getUniqueId();
        
        if (!setupSessions.containsKey(playerId)) {
            return false;
        }
        
        SetupSession session = setupSessions.get(playerId);
        
        if (session.getLastClickedLocation() == null) {
            return false;
        }
        
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            player.sendMessage(ChatColor.RED + "Неверный формат. Используйте: <номер команды> <имя точки>");
            return false;
        }
        
        int teamNumber;
        try {
            teamNumber = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Неверный номер команды. Используйте 1, 2 или 3.");
            return false;
        }
        
        if (teamNumber < 1 || teamNumber > 3) {
            player.sendMessage(ChatColor.RED + "Неверный номер команды. Используйте 1, 2 или 3.");
            return false;
        }
        
        String flagName = parts[1];
        TeamType team;
        boolean isMainFlag = flagName.toLowerCase().contains("баз");
        
        switch (teamNumber) {
            case 1:
                team = TeamType.TEAM_A;
                break;
            case 2:
                team = TeamType.TEAM_B;
                break;
            default:
                team = TeamType.NEUTRAL;
        }
        
        double radius = plugin.getConfigManager().getMainConfig().getDouble("arena.capture_radius", 5.0);
        CaptureFlag flag = new CaptureFlag(flagName, session.getLastClickedLocation(), radius, team, isMainFlag);
        session.addCaptureFlag(flag);
        
        player.sendMessage(ChatColor.GREEN + "Контрольная точка '" + flagName + "' добавлена!");
        return true;
    }
    
    /**
     * Сохраняет настроенную арену
     */
    public void saveArena(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!setupSessions.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "Вы не настраиваете арену!");
            plugin.getLogger().warning("Попытка сохранить арену игроком, который не находится в режиме настройки");
            return;
        }
        
        SetupSession session = setupSessions.get(playerId);
        
        plugin.getLogger().info("Проверка сессии настройки: " +
            "teamASpawn=" + (session.getTeamASpawn() != null) + 
            ", teamBSpawn=" + (session.getTeamBSpawn() != null) + 
            ", center=" + (session.getCenter() != null) + 
            ", captureFlags=" + session.getCaptureFlags().size());
        
        if (!session.isValid()) {
            player.sendMessage(ChatColor.RED + "Невозможно сохранить арену: не все обязательные точки установлены!");
            player.sendMessage(ChatColor.RED + "Требуются: точки спавна команд A и B, центр арены и минимум 1 контрольная точка.");
            plugin.getLogger().warning("Сессия настройки неверна: не все точки установлены");
            
            
            if (session.getTeamASpawn() != null && session.getTeamBSpawn() != null && session.getCenter() != null && 
                session.getCaptureFlags().isEmpty()) {
                
                plugin.getLogger().info("Автоматическое добавление контрольной точки в центре арены");
                Location centerLoc = session.getCenter().clone();
                CaptureFlag centerFlag = new CaptureFlag("Центральная точка", centerLoc, 5.0, TeamType.NEUTRAL, false);
                session.addCaptureFlag(centerFlag);
                player.sendMessage(ChatColor.YELLOW + "Автоматически добавлена центральная контрольная точка!");
            } else {
                return;
            }
        }
        
        
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        
        config.set("arena.team_a_spawn", session.getTeamASpawn());
        config.set("arena.team_b_spawn", session.getTeamBSpawn());
        config.set("arena.center", session.getCenter());
        
        if (session.getLobbySpawn() != null) {
            config.set("arena.lobby", session.getLobbySpawn());
        }
        
        
        config.set("arena.flags", null); 
        
        List<CaptureFlag> flags = session.getCaptureFlags();
        for (int i = 0; i < flags.size(); i++) {
            CaptureFlag flag = flags.get(i);
            String path = "arena.flags." + i;
            
            config.set(path + ".name", flag.getName());
            config.set(path + ".location", flag.getLocation());
            config.set(path + ".radius", flag.getCaptureRadius());
            config.set(path + ".team", flag.getDefaultTeam().getId());
            config.set(path + ".main_flag", flag.isMainFlag());
        }
        
        
        plugin.saveConfig();
        plugin.getLogger().info("Конфигурация арены успешно сохранена");
        
        player.sendMessage(ChatColor.GREEN + "Арена успешно сохранена!");
        
        
        stopSetup(player);
    }
    
    /**
     * Проверяет, завершена ли настройка арены
     */
    public boolean hasSetupComplete() {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        return config.contains("arena.team_a_spawn") &&
                config.contains("arena.team_b_spawn") &&
                config.contains("arena.center") &&
                config.contains("arena.flags");
    }
    
    /**
     * Создает арену из сохраненных настроек
     */
    public Arena createArena() {
        if (!hasSetupComplete()) {
            return null;
        }
        
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        
        Arena arena = new Arena("Battlefield");
        
        
        arena.setTeamASpawn(config.getLocation("arena.team_a_spawn"));
        arena.setTeamBSpawn(config.getLocation("arena.team_b_spawn"));
        arena.setCenter(config.getLocation("arena.center"));
        
        if (config.contains("arena.lobby")) {
            arena.setLobbySpawn(config.getLocation("arena.lobby"));
        }
        
        
        if (config.contains("arena.flags")) {
            for (String key : config.getConfigurationSection("arena.flags").getKeys(false)) {
                String path = "arena.flags." + key;
                
                String name = config.getString(path + ".name");
                Location location = config.getLocation(path + ".location");
                double radius = config.getDouble(path + ".radius", 5.0);
                TeamType team = TeamType.fromString(config.getString(path + ".team", "N"));
                boolean mainFlag = config.getBoolean(path + ".main_flag", false);
                
                CaptureFlag flag = new CaptureFlag(name, location, radius, team, mainFlag);
                arena.addCapturePoint(flag);
            }
        }
        
        return arena;
    }
    
    /**
     * Проверяет, настраивает ли игрок арену
     */
    public boolean isPlayerInSetupMode(Player player) {
        return setupSessions.containsKey(player.getUniqueId());
    }
    
    /**
     * Получает сессию настройки игрока
     */
    public SetupSession getSetupSession(Player player) {
        return setupSessions.get(player.getUniqueId());
    }
    
    /**
     * Запрашивает информацию для генерации карты
     */
    private void promptForMapGeneration(Player player, Location clickedLocation) {
        UUID playerId = player.getUniqueId();
        SetupSession session = setupSessions.get(playerId);
        session.setLastClickedLocation(clickedLocation);

        player.sendMessage(ChatColor.LIGHT_PURPLE + "=== ГЕНЕРАЦИЯ КАРТЫ ===");
        player.sendMessage(ChatColor.YELLOW + "Выберите тему для карты:");
        player.sendMessage(ChatColor.GRAY + "1. Стандартная");
        player.sendMessage(ChatColor.GREEN + "2. Джунгли");
        player.sendMessage(ChatColor.AQUA + "3. Зимняя");
        player.sendMessage(ChatColor.GOLD + "4. Пустыня");
        player.sendMessage(ChatColor.RED + "5. Руины");
        player.sendMessage(ChatColor.YELLOW + "Введите в чат номер темы и имя арены (например: 1 Главная Арена)");
    }

    /**
     * Обрабатывает информацию о генерации карты
     */
    public boolean handleMapGenerationMessage(Player player, String message) {
        UUID playerId = player.getUniqueId();
        
        if (!setupSessions.containsKey(playerId)) {
            return false;
        }
        
        SetupSession session = setupSessions.get(playerId);
        
        if (session.getLastClickedLocation() == null) {
            return false;
        }
        
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            player.sendMessage(ChatColor.RED + "Неверный формат. Используйте: <номер темы> <имя арены>");
            return false;
        }
        
        int themeId;
        try {
            themeId = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Неверный номер темы. Введите число от 1 до 5.");
            return false;
        }
        
        if (themeId < 1 || themeId > 5) {
            player.sendMessage(ChatColor.RED + "Неверный номер темы. Введите число от 1 до 5.");
            return false;
        }
        
        String arenaName = parts[1];
        String themeName;
        
        switch (themeId) {
            case 1:
                themeName = "default";
                break;
            case 2:
                themeName = "jungle";
                break;
            case 3:
                themeName = "winter";
                break;
            case 4:
                themeName = "desert";
                break;
            case 5:
                themeName = "ruins";
                break;
            default:
                themeName = "default";
        }
        
        generateMap(player, arenaName, themeName, session.getLastClickedLocation());
        return true;
    }
    
    /**
     * Генерирует карту с использованием MapGenerator
     */
    public void generateMap(Player player, String arenaName, String themeName, Location centerLocation) {
        World world = centerLocation.getWorld();
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: мир не найден!");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + "Начинаем генерацию карты " + arenaName + " с темой " + themeName + "...");
        plugin.getLogger().info("Генерация карты: " + arenaName + ", тема: " + themeName);
        
        
        MapGenerator mapGenerator = plugin.getGameManager().getMapGenerator();
        mapGenerator.setTheme(themeName);
        
        
        Location viewLocation = centerLocation.clone().add(0, 30, 0);
        player.teleport(viewLocation);
        
        
        Arena arena = mapGenerator.generateMap(world, arenaName);
        
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Ошибка при генерации карты!");
            plugin.getLogger().severe("Не удалось сгенерировать карту: карта null");
            return;
        }
        
        plugin.getLogger().info("Карта успешно сгенерирована");
        
        
        UUID playerId = player.getUniqueId();
        SetupSession session = setupSessions.get(playerId);
        
        session.setTeamASpawn(arena.getTeamASpawn());
        session.setTeamBSpawn(arena.getTeamBSpawn());
        session.setCenter(arena.getCenter());
        
        
        for (CaptureFlag flag : arena.getCapturePoints()) {
            session.addCaptureFlag(flag);
        }
        
        
        if (session.getCaptureFlags().isEmpty() && session.getCenter() != null) {
            plugin.getLogger().info("Добавление центральной контрольной точки");
            Location centerLoc = session.getCenter().clone();
            CaptureFlag centerFlag = new CaptureFlag("Центральная точка", centerLoc, 5.0, TeamType.NEUTRAL, false);
            session.addCaptureFlag(centerFlag);
            
            
            if (session.getTeamASpawn() != null) {
                Location teamAMiddle = centerLoc.clone().add(
                    (session.getTeamASpawn().getX() - centerLoc.getX()) / 2,
                    0,
                    (session.getTeamASpawn().getZ() - centerLoc.getZ()) / 2
                );
                CaptureFlag teamAFlag = new CaptureFlag("Точка A", teamAMiddle, 5.0, TeamType.TEAM_A, false);
                session.addCaptureFlag(teamAFlag);
            }
            
            if (session.getTeamBSpawn() != null) {
                Location teamBMiddle = centerLoc.clone().add(
                    (session.getTeamBSpawn().getX() - centerLoc.getX()) / 2,
                    0,
                    (session.getTeamBSpawn().getZ() - centerLoc.getZ()) / 2
                );
                CaptureFlag teamBFlag = new CaptureFlag("Точка B", teamBMiddle, 5.0, TeamType.TEAM_B, false);
                session.addCaptureFlag(teamBFlag);
            }
        }
        
        
        Location lobbyLocation = mapGenerator.generateLobby(world);
        session.setLobbySpawn(lobbyLocation);
        
        
        plugin.getLogger().info("Результаты генерации: " +
            "teamASpawn=" + (session.getTeamASpawn() != null ? session.getTeamASpawn().toString() : "null") + 
            ", teamBSpawn=" + (session.getTeamBSpawn() != null ? session.getTeamBSpawn().toString() : "null") + 
            ", center=" + (session.getCenter() != null ? session.getCenter().toString() : "null") + 
            ", captureFlags=" + session.getCaptureFlags().size());
        
        player.sendMessage(ChatColor.GREEN + "Карта успешно сгенерирована!");
        player.sendMessage(ChatColor.YELLOW + "Теперь используйте предмет 'Сохранить арену', чтобы сохранить настройки.");
    }
    
    /**
     * Класс для хранения информации о сессии настройки
     */
    public class SetupSession {
        private Location teamASpawn;
        private Location teamBSpawn;
        private Location center;
        private Location lobbySpawn;
        private Location lastClickedLocation;
        private final List<CaptureFlag> captureFlags = new ArrayList<>();
        private ItemStack[] savedInventory;
        
        /**
         * Проверяет, настроены ли все обязательные точки
         */
        public boolean isValid() {
            return teamASpawn != null && teamBSpawn != null && center != null && !captureFlags.isEmpty();
        }
        
        /**
         * Сохраняет инвентарь игрока
         */
        public void saveInventory(Player player) {
            savedInventory = player.getInventory().getContents();
        }
        
        /**
         * Восстанавливает инвентарь игрока
         */
        public void restoreInventory(Player player) {
            if (savedInventory != null) {
                player.getInventory().setContents(savedInventory);
            }
        }
        
        
        
        public Location getTeamASpawn() {
            return teamASpawn;
        }
        
        public void setTeamASpawn(Location teamASpawn) {
            this.teamASpawn = teamASpawn;
        }
        
        public Location getTeamBSpawn() {
            return teamBSpawn;
        }
        
        public void setTeamBSpawn(Location teamBSpawn) {
            this.teamBSpawn = teamBSpawn;
        }
        
        public Location getCenter() {
            return center;
        }
        
        public void setCenter(Location center) {
            this.center = center;
        }
        
        public Location getLobbySpawn() {
            return lobbySpawn;
        }
        
        public void setLobbySpawn(Location lobbySpawn) {
            this.lobbySpawn = lobbySpawn;
        }
        
        public Location getLastClickedLocation() {
            return lastClickedLocation;
        }
        
        public void setLastClickedLocation(Location lastClickedLocation) {
            this.lastClickedLocation = lastClickedLocation;
        }
        
        public List<CaptureFlag> getCaptureFlags() {
            return captureFlags;
        }
        
        public void addCaptureFlag(CaptureFlag flag) {
            captureFlags.add(flag);
        }
    }
} 