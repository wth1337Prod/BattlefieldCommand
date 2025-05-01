package org.comm.battlefieldCommand.game.environment;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.arena.Arena;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Система разрушаемого окружения
 * Управляет разрушением и восстановлением блоков на карте
 */
public class DestructibleEnvironment {
    private final BattlefieldCommand plugin;
    
    
    private final Map<Location, BlockState> savedBlocks = new ConcurrentHashMap<>();
    
    
    private boolean destructionEnabled = true;
    private boolean autoRestore = true;
    private int autoRestoreTime = 60; 
    private double destructionChance = 0.8; 
    
    
    private final Set<Material> destructibleMaterials = new HashSet<>();
    
    
    private final List<ProtectedRegion> protectedRegions = new ArrayList<>();
    
    public DestructibleEnvironment(BattlefieldCommand plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Загружает настройки разрушаемости из конфигурации
     */
    private void loadConfig() {
        ConfigurationSection config = plugin.getConfigManager().getMainConfig().getConfigurationSection("destruction");
        if (config == null) return;
        
        destructionEnabled = config.getBoolean("enabled", true);
        autoRestore = config.getBoolean("auto_restore", true);
        autoRestoreTime = config.getInt("auto_restore_time", 60);
        destructionChance = config.getDouble("destruction_chance", 0.8);
        
        
        List<String> materialNames = config.getStringList("destructible_materials");
        for (String materialName : materialNames) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                destructibleMaterials.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный материал в конфигурации: " + materialName);
            }
        }
        
        
        if (destructibleMaterials.isEmpty()) {
            addDefaultDestructibleMaterials();
        }
    }
    
    /**
     * Добавляет стандартные разрушаемые материалы
     */
    private void addDefaultDestructibleMaterials() {
        destructibleMaterials.add(Material.STONE);
        destructibleMaterials.add(Material.COBBLESTONE);
        destructibleMaterials.add(Material.DIRT);
        destructibleMaterials.add(Material.GRASS_BLOCK);
        destructibleMaterials.add(Material.SAND);
        destructibleMaterials.add(Material.GRAVEL);
        destructibleMaterials.add(Material.BRICKS);
        destructibleMaterials.add(Material.GLASS);
        destructibleMaterials.add(Material.OAK_SLAB);
        destructibleMaterials.add(Material.OAK_PLANKS);
        destructibleMaterials.add(Material.SPRUCE_PLANKS);
        destructibleMaterials.add(Material.BIRCH_PLANKS);
        destructibleMaterials.add(Material.JUNGLE_PLANKS);
        destructibleMaterials.add(Material.ACACIA_PLANKS);
        destructibleMaterials.add(Material.DARK_OAK_PLANKS);
        destructibleMaterials.add(Material.OAK_FENCE);
        destructibleMaterials.add(Material.IRON_BARS);
    }
    
    /**
     * Инициализирует защищенные регионы для арены
     */
    public void initializeProtectedRegions(Arena arena) {
        protectedRegions.clear();
        
        
        if (arena.getTeamASpawn() != null) {
            addProtectedRegion(arena.getTeamASpawn(), 10);
        }
        
        if (arena.getTeamBSpawn() != null) {
            addProtectedRegion(arena.getTeamBSpawn(), 10);
        }
        
        
        ConfigurationSection regionsConfig = plugin.getConfigManager().getMainConfig().getConfigurationSection("destruction.protected_regions");
        if (regionsConfig != null) {
            for (String key : regionsConfig.getKeys(false)) {
                ConfigurationSection regionConfig = regionsConfig.getConfigurationSection(key);
                if (regionConfig == null) continue;
                
                World world = arena.getWorld();
                if (world == null) continue;
                
                int x1 = regionConfig.getInt("min_x");
                int y1 = regionConfig.getInt("min_y");
                int z1 = regionConfig.getInt("min_z");
                int x2 = regionConfig.getInt("max_x");
                int y2 = regionConfig.getInt("max_y");
                int z2 = regionConfig.getInt("max_z");
                
                addProtectedRegion(world, x1, y1, z1, x2, y2, z2);
            }
        }
    }
    
    /**
     * Добавляет защищенный регион вокруг указанного центра
     */
    private void addProtectedRegion(Location center, int radius) {
        if (center == null || center.getWorld() == null) return;
        
        World world = center.getWorld();
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        
        addProtectedRegion(world, x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
    }
    
    /**
     * Добавляет защищенный регион в указанных координатах
     */
    private void addProtectedRegion(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        if (world == null) return;
        
        ProtectedRegion region = new ProtectedRegion(world.getName(), 
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), 
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
        
        protectedRegions.add(region);
    }
    
    /**
     * Обрабатывает взрыв для разрушения блоков
     * @param event Событие взрыва
     * @return true, если событие должно быть отменено
     */
    public boolean handleExplosion(EntityExplodeEvent event) {
        if (!destructionEnabled) {
            return true; 
        }
        
        Location center = event.getLocation();
        World world = center.getWorld();
        
        if (world == null) return true;
        
        
        List<Block> blocksToDestroy = new ArrayList<>(event.blockList());
        
        
        event.blockList().clear();
        
        
        for (Block block : blocksToDestroy) {
            
            if (!canBeDestroyed(block)) continue;
            
            
            if (Math.random() > destructionChance) continue;
            
            
            saveBlockState(block);
            
            
            createDebris(block);
            
            
            block.setType(Material.AIR);
        }
        
        
        if (autoRestore) {
            scheduleRestore(autoRestoreTime);
        }
        
        return false; 
    }
    
    /**
     * Проверяет, может ли блок быть разрушен
     */
    private boolean canBeDestroyed(Block block) {
        if (block == null) return false;
        
        
        if (!destructibleMaterials.contains(block.getType())) {
            return false;
        }
        
        
        Location location = block.getLocation();
        for (ProtectedRegion region : protectedRegions) {
            if (region.contains(location)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Сохраняет состояние блока для последующего восстановления
     */
    private void saveBlockState(Block block) {
        BlockState state = new BlockState(
                block.getType(),
                block.getBlockData().getAsString()
        );
        
        savedBlocks.put(block.getLocation(), state);
    }
    
    /**
     * Создает эффект разлетающихся обломков
     */
    private void createDebris(Block block) {
        World world = block.getWorld();
        Location location = block.getLocation().add(0.5, 0.5, 0.5);
        
        
        FallingBlock fallingBlock = world.spawnFallingBlock(location, block.getBlockData());
        fallingBlock.setDropItem(false);
        fallingBlock.setHurtEntities(true);
        
        
        double speedX = (Math.random() - 0.5) * 0.3;
        double speedY = Math.random() * 0.5;
        double speedZ = (Math.random() - 0.5) * 0.3;
        
        fallingBlock.setVelocity(new Vector(speedX, speedY, speedZ));
    }
    
    /**
     * Планирует восстановление разрушенных блоков
     */
    private void scheduleRestore(int delaySeconds) {
        plugin.getServer().getScheduler().runTaskLater(plugin, this::restoreBlocks, delaySeconds * 20L);
    }
    
    /**
     * Восстанавливает все разрушенные блоки
     */
    public void restoreBlocks() {
        
        List<Map.Entry<Location, BlockState>> blockList = new ArrayList<>(savedBlocks.entrySet());
        blockList.sort(Comparator.comparingInt(entry -> entry.getKey().getBlockY()));
        
        for (Map.Entry<Location, BlockState> entry : blockList) {
            Location location = entry.getKey();
            BlockState state = entry.getValue();
            
            Block block = location.getBlock();
            block.setType(state.getMaterial());
            
            try {
                block.setBlockData(plugin.getServer().createBlockData(state.getBlockData()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ошибка при восстановлении блока: " + e.getMessage());
                block.setType(state.getMaterial()); 
            }
        }
        
        
        savedBlocks.clear();
    }
    
    /**
     * Мгновенно разрушает блок
     */
    public void destroyBlock(Block block, Player source) {
        if (!destructionEnabled || !canBeDestroyed(block)) {
            return;
        }
        
        
        saveBlockState(block);
        
        
        createDebris(block);
        
        
        block.setType(Material.AIR);
        
        
        if (autoRestore) {
            scheduleRestore(autoRestoreTime);
        }
    }
    
    /**
     * Проверяет, включена ли система разрушения
     */
    public boolean isDestructionEnabled() {
        return destructionEnabled;
    }
    
    /**
     * Временно включает или отключает разрушаемость
     */
    public void setDestructionEnabled(boolean enabled) {
        this.destructionEnabled = enabled;
    }
    
    /**
     * Класс для хранения состояния блока
     */
    private static class BlockState {
        private final Material material;
        private final String blockData;
        
        public BlockState(Material material, String blockData) {
            this.material = material;
            this.blockData = blockData;
        }
        
        public Material getMaterial() {
            return material;
        }
        
        public String getBlockData() {
            return blockData;
        }
    }
    
    /**
     * Класс для определения защищенной области
     */
    private static class ProtectedRegion {
        private final String worldName;
        private final int minX, minY, minZ;
        private final int maxX, maxY, maxZ;
        
        public ProtectedRegion(String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.worldName = worldName;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
        
        /**
         * Проверяет, содержит ли регион указанную точку
         */
        public boolean contains(Location location) {
            if (location == null || location.getWorld() == null) {
                return false;
            }
            
            if (!location.getWorld().getName().equals(worldName)) {
                return false;
            }
            
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            
            return x >= minX && x <= maxX && 
                   y >= minY && y <= maxY && 
                   z >= minZ && z <= maxZ;
        }
    }
} 