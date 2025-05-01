package org.comm.battlefieldCommand.game.map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.arena.Arena;
import org.comm.battlefieldCommand.game.arena.capture.CaptureFlag;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Генератор карт для PvP-сражений
 */
public class MapGenerator {
    private final BattlefieldCommand plugin;
    private final Random random = new Random();
    private final BaseStructureGenerator baseStructureGenerator;
    private final MapThemeManager themeManager;
    
    
    private int baseSize = 30;
    private int distanceBetweenBases = 150;
    private int numCapturePoints = 5;
    private int capturePointRadius = 5;
    private int baseHeight = 3;
    private int spawnProtectionRadius = 10;
    private boolean ammoStationEnabled = true;
    private boolean healStationEnabled = true;
    
    
    private Material baseFloorMaterial = Material.STONE_BRICKS;
    private Material baseWallMaterial = Material.COBBLESTONE_WALL;
    private Material teamAColorMaterial = Material.RED_CONCRETE;
    private Material teamBColorMaterial = Material.BLUE_CONCRETE;
    private Material flagPostMaterial = Material.QUARTZ_PILLAR;
    
    
    private MapTheme currentTheme;
    
    public MapGenerator(BattlefieldCommand plugin) {
        this.plugin = plugin;
        this.baseStructureGenerator = new BaseStructureGenerator(plugin);
        this.themeManager = new MapThemeManager(plugin);
        this.currentTheme = themeManager.getTheme("default");
        loadConfig();
    }
    
    /**
     * Загружает настройки генерации из конфигурации
     */
    private void loadConfig() {
        ConfigurationSection config = plugin.getConfigManager().getMainConfig().getConfigurationSection("map_generator");
        if (config == null) return;
        
        baseSize = config.getInt("base_size", 30);
        distanceBetweenBases = config.getInt("distance_between_bases", 150);
        numCapturePoints = config.getInt("num_capture_points", 5);
        capturePointRadius = config.getInt("capture_point_radius", 5);
        baseHeight = config.getInt("base_height", 3);
        spawnProtectionRadius = config.getInt("spawn_protection_radius", 10);
        ammoStationEnabled = config.getBoolean("ammo_station_enabled", true);
        healStationEnabled = config.getBoolean("heal_station_enabled", true);
        
        
        String teamAColorStr = config.getString("team_a_color", "RED_CONCRETE");
        String teamBColorStr = config.getString("team_b_color", "BLUE_CONCRETE");
        String baseFloorStr = config.getString("base_floor_material", "STONE_BRICKS");
        String baseWallStr = config.getString("base_wall_material", "COBBLESTONE_WALL");
        String flagPostStr = config.getString("flag_post_material", "QUARTZ_PILLAR");
        
        try {
            teamAColorMaterial = Material.valueOf(teamAColorStr);
            teamBColorMaterial = Material.valueOf(teamBColorStr);
            baseFloorMaterial = Material.valueOf(baseFloorStr);
            baseWallMaterial = Material.valueOf(baseWallStr);
            flagPostMaterial = Material.valueOf(flagPostStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверный материал в конфигурации: " + e.getMessage());
        }
    }
    
    /**
     * Устанавливает тему карты
     */
    public void setTheme(String themeId) {
        currentTheme = themeManager.getTheme(themeId);
    }
    
    /**
     * Генерирует полную карту с базами и контрольными точками
     */
    public Arena generateMap(World world, String arenaName) {
        if (world == null) return null;
        
        
        Arena arena = new Arena(arenaName);
        
        
        Location center = new Location(world, 0, 200, 0);
        
        
        generateTerrain(world, center);
        
        
        Location teamABase = generateTeamBase(world, center, -1, TeamType.TEAM_A);
        Location teamBBase = generateTeamBase(world, center, 1, TeamType.TEAM_B);
        
        
        arena.setTeamASpawn(teamABase);
        arena.setTeamBSpawn(teamBBase);
        arena.setCenter(center);
        
        
        generateCapturePoints(arena, world, center);
        
        return arena;
    }
    
    /**
     * Генерирует реалистичный ландшафт для карты
     */
    private void generateTerrain(World world, Location center) {
        int radius = distanceBetweenBases + 100; 
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int baseY = center.getBlockY() - 5; 
        
        plugin.getLogger().info("Генерация ландшафта с центром в: " + centerX + ", " + baseY + ", " + centerZ);
        
        
        Material surfaceMaterial = currentTheme.getFloorMaterial();
        Material baseMaterial = currentTheme.getMainMaterial();
        
        
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                
                if (Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2)) <= radius) {
                    
                    double noise = getNoise(x, z);
                    int height = (int)(noise * 20); 
                    
                    
                    for (int y = baseY - 10; y <= baseY + height; y++) {
                        if (y == baseY + height) {
                            
                            world.getBlockAt(x, y, z).setType(surfaceMaterial);
                        } else if (y > baseY + height - 3) {
                            
                            world.getBlockAt(x, y, z).setType(baseMaterial);
                        } else {
                            
                            world.getBlockAt(x, y, z).setType(Material.STONE);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Генерирует значение шума для создания реалистичного ландшафта
     */
    private double getNoise(int x, int z) {
        double scale = 0.02; 
        
        double noise = 0.5 * (1 + Math.sin(x * scale) * Math.cos(z * scale) + 
                              0.5 * Math.sin(x * scale * 2) * Math.cos(z * scale * 2) +
                              0.25 * Math.sin(x * scale * 4) * Math.cos(z * scale * 4));
        
        
        for (int i = 0; i < 5; i++) {
            int hillX = random.nextInt(200) - 100;
            int hillZ = random.nextInt(200) - 100;
            double distance = Math.sqrt(Math.pow(x - hillX, 2) + Math.pow(z - hillZ, 2));
            double hillSize = random.nextInt(50) + 30;
            
            if (distance < hillSize) {
                double hillHeight = (1 - distance / hillSize) * 0.5;
                noise += hillHeight;
            }
        }
        
        return Math.max(0, Math.min(1, noise)); 
    }
    
    /**
     * Генерирует базу команды
     * @param direction -1 для команды A, 1 для команды B
     */
    private Location generateTeamBase(World world, Location center, int direction, TeamType teamType) {
        
        int offsetX = direction * (distanceBetweenBases / 2);
        Location baseCenter = center.clone().add(offsetX, 0, 0);
        
        
        flattenTerrain(world, baseCenter, baseSize);
        
        
        Material colorMaterial = (teamType == TeamType.TEAM_A) ? teamAColorMaterial : teamBColorMaterial;
        
        
        for (int x = -baseSize / 2; x <= baseSize / 2; x++) {
            for (int z = -baseSize / 2; z <= baseSize / 2; z++) {
                Location blockLoc = baseCenter.clone().add(x, -1, z);
                
                
                if (Math.abs(x) < baseSize / 2 - 1 && Math.abs(z) < baseSize / 2 - 1) {
                    blockLoc.getBlock().setType(currentTheme.getFloorMaterial());
                } else {
                    
                    blockLoc.getBlock().setType(colorMaterial);
                }
                
                
                if (x == -baseSize / 2 || x == baseSize / 2 || z == -baseSize / 2 || z == baseSize / 2) {
                    for (int y = 0; y < baseHeight; y++) {
                        Location wallLoc = baseCenter.clone().add(x, y, z);
                        wallLoc.getBlock().setType(baseWallMaterial);
                    }
                }
                
                
                if ((Math.abs(x) + Math.abs(z)) % 5 == 0) {
                    Location accentLoc = baseCenter.clone().add(x, -1, z);
                    accentLoc.getBlock().setType(colorMaterial);
                }
            }
        }
        
        
        Location spawnPoint = baseStructureGenerator.generateSpawnRoom(baseCenter, teamType, direction);
        
        
        if (ammoStationEnabled) {
            baseStructureGenerator.generateAmmoStation(baseCenter, teamType);
        }
        
        
        if (healStationEnabled) {
            baseStructureGenerator.generateHealStation(baseCenter, teamType);
        }
        
        
        baseStructureGenerator.generateBaseDefenses(baseCenter, baseSize, currentTheme.getMainMaterial(), colorMaterial);
        
        return spawnPoint;
    }
    
    /**
     * Выравнивает участок местности для базы команды
     */
    private void flattenTerrain(World world, Location center, int size) {
        int halfSize = size / 2 + 5; 
        
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                
                Location loc = center.clone().add(x, -1, z);
                loc.getBlock().setType(Material.STONE);
                
                
                for (int y = 0; y < 10; y++) {
                    center.clone().add(x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }
    }
    
    /**
     * Генерирует контрольные точки между базами
     */
    private void generateCapturePoints(Arena arena, World world, Location center) {
        List<Location> capturePointLocations = new ArrayList<>();
        
        
        capturePointLocations.add(center.clone());
        
        
        int step = distanceBetweenBases / (numCapturePoints + 1);
        
        
        for (int i = 1; i <= numCapturePoints / 2; i++) {
            
            Location pointA = center.clone().add(-step * i, 0, 0);
            
            adjustHeightToTerrain(world, pointA);
            capturePointLocations.add(pointA);
            
            
            Location pointB = center.clone().add(step * i, 0, 0);
            
            adjustHeightToTerrain(world, pointB);
            capturePointLocations.add(pointB);
        }
        
        
        if (numCapturePoints % 2 != 0) {
            Location extraPoint = center.clone().add(0, 0, step);
            adjustHeightToTerrain(world, extraPoint);
            capturePointLocations.add(extraPoint);
        }
        
        
        for (int i = 0; i < capturePointLocations.size(); i++) {
            Location flagLocation = capturePointLocations.get(i);
            
            
            TeamType defaultTeam = TeamType.NEUTRAL;
            boolean isMainFlag = false;
            
            
            double distanceToTeamA = flagLocation.distance(arena.getTeamASpawn());
            double distanceToTeamB = flagLocation.distance(arena.getTeamBSpawn());
            
            
            if (distanceToTeamA < baseSize) {
                defaultTeam = TeamType.TEAM_A;
                isMainFlag = true;
            } else if (distanceToTeamB < baseSize) {
                defaultTeam = TeamType.TEAM_B;
                isMainFlag = true;
            }
            
            
            flattenCapturePoint(world, flagLocation, capturePointRadius);
            
            
            createCapturePointStructure(flagLocation, defaultTeam);
            
            
            String flagName = "flag_" + i;
            CaptureFlag captureFlag = new CaptureFlag(flagName, flagLocation, capturePointRadius, defaultTeam, isMainFlag);
            arena.addCapturePoint(captureFlag);
        }
    }
    
    /**
     * Корректирует высоту точки в соответствии с сгенерированным ландшафтом
     */
    private void adjustHeightToTerrain(World world, Location location) {
        
        for (int y = 250; y > 0; y--) {
            Location checkLoc = location.clone();
            checkLoc.setY(y);
            if (checkLoc.getBlock().getType().isSolid()) {
                location.setY(y + 1); 
                return;
            }
        }
        
        
        location.setY(200);
    }
    
    /**
     * Выравнивает площадку для контрольной точки
     */
    private void flattenCapturePoint(World world, Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x*x + z*z <= radius*radius) {
                    Location blockLoc = center.clone().add(x, -1, z);
                    blockLoc.getBlock().setType(currentTheme.getFloorMaterial());
                    
                    
                    for (int y = 0; y < 3; y++) {
                        center.clone().add(x, y, z).getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }
    
    /**
     * Создает структуру контрольной точки
     */
    private void createCapturePointStructure(Location location, TeamType teamType) {
        World world = location.getWorld();
        Material teamColor = TeamType.NEUTRAL == teamType ? Material.WHITE_CONCRETE : 
                              (teamType == TeamType.TEAM_A ? teamAColorMaterial : teamBColorMaterial);
        
        
        int platformRadius = 3;
        for (int x = -platformRadius; x <= platformRadius; x++) {
            for (int z = -platformRadius; z <= platformRadius; z++) {
                if (x*x + z*z <= platformRadius*platformRadius) {
                    Location platformLoc = location.clone().add(x, -1, z);
                    platformLoc.getBlock().setType(teamColor);
                }
            }
        }
        
        
        for (int y = 0; y < 3; y++) {
            location.clone().add(0, y, 0).getBlock().setType(flagPostMaterial);
        }
        
        
        Block flagBlock = location.clone().add(0, 3, 0).getBlock();
        flagBlock.setType(teamColor);
        
        
        if (teamType != TeamType.NEUTRAL) {
            for (int i = 0; i < 4; i++) {
                Location defenseLoc = location.clone().add(
                    Math.sin(i * Math.PI / 2) * 2,
                    0,
                    Math.cos(i * Math.PI / 2) * 2
                );
                for (int y = 0; y < 2; y++) {
                    defenseLoc.clone().add(0, y, 0).getBlock().setType(currentTheme.getMainMaterial());
                }
            }
        }
    }
    
    /**
     * Генерирует лобби для ожидания игры
     */
    public Location generateLobby(World world) {
        
        Location lobbyLocation = new Location(world, 0, 200, distanceBetweenBases / 2 + 20);
        
        
        int lobbySize = 15;
        for (int x = -lobbySize / 2; x <= lobbySize / 2; x++) {
            for (int z = -lobbySize / 2; z <= lobbySize / 2; z++) {
                
                lobbyLocation.clone().add(x, -1, z).getBlock().setType(Material.POLISHED_ANDESITE);
                
                
                if (x == -lobbySize / 2 || x == lobbySize / 2 || z == -lobbySize / 2 || z == lobbySize / 2) {
                    for (int y = 0; y < 1; y++) {
                        lobbyLocation.clone().add(x, y, z).getBlock().setType(Material.SMOOTH_STONE_SLAB);
                    }
                }
                
                
                for (int y = 0; y < 5; y++) {
                    lobbyLocation.clone().add(x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }
        
        
        for (int i = 0; i < 4; i++) {
            Location lampLoc = lobbyLocation.clone().add(
                Math.sin(i * Math.PI / 2) * (lobbySize / 4),
                0,
                Math.cos(i * Math.PI / 2) * (lobbySize / 4)
            );
            lampLoc.getBlock().setType(Material.LANTERN);
        }
        
        return lobbyLocation;
    }
    
    /**
     * Возвращает размер базы
     */
    public int getBaseSize() {
        return baseSize;
    }
    
    /**
     * Устанавливает размер базы
     */
    public void setBaseSize(int baseSize) {
        this.baseSize = baseSize;
    }
    
    /**
     * Возвращает расстояние между базами
     */
    public int getDistanceBetweenBases() {
        return distanceBetweenBases;
    }
    
    /**
     * Устанавливает расстояние между базами
     */
    public void setDistanceBetweenBases(int distanceBetweenBases) {
        this.distanceBetweenBases = distanceBetweenBases;
    }
    
    /**
     * Возвращает количество контрольных точек
     */
    public int getNumCapturePoints() {
        return numCapturePoints;
    }
    
    /**
     * Устанавливает количество контрольных точек
     */
    public void setNumCapturePoints(int numCapturePoints) {
        this.numCapturePoints = numCapturePoints;
    }
} 