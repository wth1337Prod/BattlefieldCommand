package org.comm.battlefieldCommand.game.map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.ConfigurationSection;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.Random;

/**
 * Генератор структур для базы команды
 */
public class BaseStructureGenerator {
    private final BattlefieldCommand plugin;
    private final Random random = new Random();
    
    
    private int spawnRoomSize = 10;
    private int spawnRoomHeight = 4;
    private int spawnRoomWallsThickness = 2;
    private boolean spawnRoomAddRoof = true;
    private int spawnRoomEntryWidth = 3;
    private boolean spawnRoomHidden = true;
    
    private int ammoStationSize = 5;
    private int ammoStationHeight = 3;
    private boolean ammoStationChestItems = true;
    
    private int healStationSize = 5;
    private int healStationHeight = 3;
    private boolean healStationBeaconEnabled = true;
    
    private int defenseWallHeight = 4;
    private boolean defenseAddBarricades = true;
    
    public BaseStructureGenerator(BattlefieldCommand plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Загружает настройки из конфигурации
     */
    private void loadConfig() {
        ConfigurationSection config = plugin.getConfigManager().getConfig("maps");
        if (config == null) return;
        
        ConfigurationSection baseConfig = config.getConfigurationSection("generator.base");
        if (baseConfig == null) return;
        
        
        ConfigurationSection spawnConfig = baseConfig.getConfigurationSection("spawn_room");
        if (spawnConfig != null) {
            spawnRoomSize = spawnConfig.getInt("size", 10);
            spawnRoomHeight = spawnConfig.getInt("height", 4);
            spawnRoomWallsThickness = spawnConfig.getInt("walls_thickness", 2);
            spawnRoomAddRoof = spawnConfig.getBoolean("add_roof", true);
            spawnRoomEntryWidth = spawnConfig.getInt("entry_width", 3);
            spawnRoomHidden = spawnConfig.getBoolean("hidden", true);
        }
        
        
        ConfigurationSection ammoConfig = baseConfig.getConfigurationSection("ammo_station");
        if (ammoConfig != null) {
            ammoStationSize = ammoConfig.getInt("size", 5);
            ammoStationHeight = ammoConfig.getInt("height", 3);
            ammoStationChestItems = ammoConfig.getBoolean("chest_items", true);
        }
        
        
        ConfigurationSection healConfig = baseConfig.getConfigurationSection("heal_station");
        if (healConfig != null) {
            healStationSize = healConfig.getInt("size", 5);
            healStationHeight = healConfig.getInt("height", 3);
            healStationBeaconEnabled = healConfig.getBoolean("beacon_enabled", true);
        }
        
        
        ConfigurationSection defenseConfig = baseConfig.getConfigurationSection("defenses");
        if (defenseConfig != null) {
            defenseWallHeight = defenseConfig.getInt("wall_height", 4);
            defenseAddBarricades = defenseConfig.getBoolean("add_barricades", true);
        }
    }
    
    /**
     * Генерирует скрытую зону респавна для команды
     * @param baseCenter Центр базы
     * @param teamType Тип команды
     * @param direction Направление базы (-1 для команды A, 1 для команды B)
     * @return Точка спавна игроков
     */
    public Location generateSpawnRoom(Location baseCenter, TeamType teamType, int direction) {
        World world = baseCenter.getWorld();
        Material teamColor = getTeamColorMaterial(teamType);
        
        
        Location spawnRoomCenter = baseCenter.clone().add(-direction * (spawnRoomSize / 2), 0, 0);
        spawnRoomCenter.setY(world.getHighestBlockYAt(spawnRoomCenter) + 1);
        
        
        int halfSize = spawnRoomSize / 2;
        
        
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                
                spawnRoomCenter.clone().add(x, -1, z).getBlock().setType(teamColor);
                
                
                if (Math.abs(x) >= halfSize - spawnRoomWallsThickness + 1 || 
                    Math.abs(z) >= halfSize - spawnRoomWallsThickness + 1) {
                    
                    
                    boolean isEntryWall = x == halfSize * direction && 
                                         Math.abs(z) <= spawnRoomEntryWidth / 2;
                    
                    if (!isEntryWall) {
                        for (int y = 0; y < spawnRoomHeight; y++) {
                            spawnRoomCenter.clone().add(x, y, z).getBlock().setType(teamColor);
                        }
                    }
                }
            }
        }
        
        
        if (spawnRoomAddRoof) {
            for (int x = -halfSize; x <= halfSize; x++) {
                for (int z = -halfSize; z <= halfSize; z++) {
                    spawnRoomCenter.clone().add(x, spawnRoomHeight, z).getBlock().setType(teamColor);
                }
            }
        }
        
        
        if (spawnRoomHidden) {
            createHiddenEntrance(spawnRoomCenter, halfSize * direction, spawnRoomHeight, spawnRoomEntryWidth, teamColor);
        }
        
        return spawnRoomCenter;
    }
    
    /**
     * Создает скрытый вход в комнату спавна
     */
    private void createHiddenEntrance(Location center, int entryX, int height, int width, Material material) {
        
        for (int z = -width / 2 - 1; z <= width / 2 + 1; z++) {
            if (Math.abs(z) > width / 2) {
                for (int y = 0; y < height + 1; y++) {
                    center.clone().add(entryX, y, z).getBlock().setType(material);
                }
            }
        }
        
        
        for (int z = -width / 2; z <= width / 2; z++) {
            center.clone().add(entryX, height, z).getBlock().setType(material);
        }
        
        
        int corridorLength = 5;
        for (int i = 1; i <= corridorLength; i++) {
            for (int z = -width / 2; z <= width / 2; z++) {
                
                center.clone().add(entryX + i, -1, z).getBlock().setType(material);
                
                
                if (Math.abs(z) == width / 2) {
                    for (int y = 0; y < height; y++) {
                        center.clone().add(entryX + i, y, z).getBlock().setType(material);
                    }
                }
                
                
                center.clone().add(entryX + i, height, z).getBlock().setType(material);
            }
        }
    }
    
    /**
     * Генерирует станцию пополнения боеприпасов
     */
    public Location generateAmmoStation(Location baseCenter, TeamType teamType) {
        World world = baseCenter.getWorld();
        Material teamColor = getTeamColorMaterial(teamType);
        
        
        Location ammoStationLocation = baseCenter.clone().add(-5, 0, 5);
        ammoStationLocation.setY(world.getHighestBlockYAt(ammoStationLocation) + 1);
        
        int halfSize = ammoStationSize / 2;
        
        
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                
                ammoStationLocation.clone().add(x, -1, z).getBlock().setType(teamColor);
                
                
                if (Math.abs(x) == halfSize || Math.abs(z) == halfSize) {
                    for (int y = 0; y < ammoStationHeight - 1; y++) {
                        ammoStationLocation.clone().add(x, y, z).getBlock().setType(Material.IRON_BARS);
                    }
                }
            }
        }
        
        
        Block chestBlock = ammoStationLocation.getBlock();
        chestBlock.setType(Material.CHEST);
        
        
        if (chestBlock.getBlockData() instanceof Directional) {
            Directional directional = (Directional) chestBlock.getBlockData();
            directional.setFacing(BlockFace.NORTH); 
            chestBlock.setBlockData(directional);
        }
        
        
        Block signBlock = ammoStationLocation.clone().add(0, 1, -1).getBlock();
        signBlock.setType(Material.OAK_WALL_SIGN);
        
        if (signBlock.getBlockData() instanceof Directional) {
            Directional directional = (Directional) signBlock.getBlockData();
            directional.setFacing(BlockFace.NORTH);
            signBlock.setBlockData(directional);
        }
        
        
        
        return ammoStationLocation;
    }
    
    /**
     * Генерирует станцию лечения
     */
    public Location generateHealStation(Location baseCenter, TeamType teamType) {
        World world = baseCenter.getWorld();
        Material teamColor = getTeamColorMaterial(teamType);
        
        
        Location healStationLocation = baseCenter.clone().add(5, 0, 5);
        healStationLocation.setY(world.getHighestBlockYAt(healStationLocation) + 1);
        
        int halfSize = healStationSize / 2;
        
        
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                
                healStationLocation.clone().add(x, -1, z).getBlock().setType(teamColor);
                
                
                if (Math.abs(x) == halfSize || Math.abs(z) == halfSize) {
                    for (int y = 0; y < healStationHeight - 1; y++) {
                        healStationLocation.clone().add(x, y, z).getBlock().setType(Material.IRON_BARS);
                    }
                }
                
                
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                    healStationLocation.clone().add(x, -2, z).getBlock().setType(Material.IRON_BLOCK);
                }
            }
        }
        
        
        if (healStationBeaconEnabled) {
            Block beaconBlock = healStationLocation.getBlock();
            beaconBlock.setType(Material.BEACON);
        }
        
        
        Block signBlock = healStationLocation.clone().add(0, 1, -1).getBlock();
        signBlock.setType(Material.OAK_WALL_SIGN);
        
        if (signBlock.getBlockData() instanceof Directional) {
            Directional directional = (Directional) signBlock.getBlockData();
            directional.setFacing(BlockFace.NORTH);
            signBlock.setBlockData(directional);
        }
        
        return healStationLocation;
    }
    
    /**
     * Генерирует оборонительные укрепления базы
     */
    public void generateBaseDefenses(Location baseCenter, int baseSize, Material wallMaterial, Material teamColor) {
        World world = baseCenter.getWorld();
        
        
        int halfSize = baseSize / 2;
        
        
        for (int i = -halfSize; i <= halfSize; i++) {
            
            buildWallSegment(baseCenter.clone().add(i, 0, -halfSize), wallMaterial, defenseWallHeight);
            buildWallSegment(baseCenter.clone().add(i, 0, halfSize), wallMaterial, defenseWallHeight);
            
            
            buildWallSegment(baseCenter.clone().add(-halfSize, 0, i), wallMaterial, defenseWallHeight);
            buildWallSegment(baseCenter.clone().add(halfSize, 0, i), wallMaterial, defenseWallHeight);
        }
        
        
        buildTower(baseCenter.clone().add(-halfSize, 0, -halfSize), wallMaterial, teamColor, defenseWallHeight + 2);
        buildTower(baseCenter.clone().add(-halfSize, 0, halfSize), wallMaterial, teamColor, defenseWallHeight + 2);
        buildTower(baseCenter.clone().add(halfSize, 0, -halfSize), wallMaterial, teamColor, defenseWallHeight + 2);
        buildTower(baseCenter.clone().add(halfSize, 0, halfSize), wallMaterial, teamColor, defenseWallHeight + 2);
        
        
        if (defenseAddBarricades) {
            addRandomBarricades(baseCenter, baseSize, wallMaterial);
        }
    }
    
    /**
     * Строит сегмент стены
     */
    private void buildWallSegment(Location start, Material material, int height) {
        for (int y = 0; y < height; y++) {
            start.clone().add(0, y, 0).getBlock().setType(material);
        }
    }
    
    /**
     * Строит башню
     */
    private void buildTower(Location corner, Material wallMaterial, Material teamColor, int height) {
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y < height; y++) {
                    Material material = (y == height - 1 || y == 0) ? teamColor : wallMaterial;
                    corner.clone().add(x, y, z).getBlock().setType(material);
                }
            }
        }
        
        
        corner.clone().add(0, height / 2, -1).getBlock().setType(Material.AIR);
        corner.clone().add(0, height / 2, 1).getBlock().setType(Material.AIR);
        corner.clone().add(-1, height / 2, 0).getBlock().setType(Material.AIR);
        corner.clone().add(1, height / 2, 0).getBlock().setType(Material.AIR);
    }
    
    /**
     * Добавляет случайные баррикады на базе
     */
    private void addRandomBarricades(Location baseCenter, int baseSize, Material material) {
        int halfSize = baseSize / 2;
        int numBarricades = 5 + random.nextInt(5); 
        
        for (int i = 0; i < numBarricades; i++) {
            
            int x = random.nextInt(baseSize - 4) - (halfSize - 2);
            int z = random.nextInt(baseSize - 4) - (halfSize - 2);
            
            Location barricadeLocation = baseCenter.clone().add(x, 0, z);
            
            
            int width = 2 + random.nextInt(3);
            int height = 1 + random.nextInt(2);
            
            
            for (int bx = 0; bx < width; bx++) {
                for (int by = 0; by < height; by++) {
                    barricadeLocation.clone().add(bx, by, 0).getBlock().setType(material);
                }
            }
        }
    }
    
    /**
     * Возвращает материал, соответствующий цвету команды
     */
    private Material getTeamColorMaterial(TeamType teamType) {
        return teamType == TeamType.TEAM_A ? Material.RED_CONCRETE : Material.BLUE_CONCRETE;
    }
} 