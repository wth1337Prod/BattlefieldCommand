package org.comm.battlefieldCommand.game.killstreak;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.player.BCPlayer;
import org.comm.battlefieldCommand.utils.MessageUtils;

import java.util.*;

/**
 * Менеджер киллстриков (наград за серию фрагов)
 */
public class KillstreakManager {
    private final BattlefieldCommand plugin;
    private final Map<UUID, Integer> currentStreaks; 
    private final Map<UUID, Long> lastKillTime; 
    private final Map<UUID, Set<KillstreakType>> activeKillstreaks = new HashMap<>();
    
    
    private static final int[] STREAK_THRESHOLDS = {3, 5, 7}; 
    
    
    private static final long STREAK_RESET_TIME = 60000; 
    
    public KillstreakManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
        this.currentStreaks = new HashMap<>();
        this.lastKillTime = new HashMap<>();
    }
    
    /**
     * Увеличивает серию убийств игрока и проверяет награды
     */
    public void incrementKillstreak(Player player, BCPlayer bcPlayer) {
        UUID playerId = player.getUniqueId();
        
        
        lastKillTime.put(playerId, System.currentTimeMillis());
        
        
        int currentStreak = currentStreaks.getOrDefault(playerId, 0) + 1;
        currentStreaks.put(playerId, currentStreak);
        
        
        if (bcPlayer.getBestKillstreak() < currentStreak) {
            bcPlayer.setBestKillstreak(currentStreak);
        }
        
        
        checkKillstreakRewards(player, currentStreak);
    }
    
    /**
     * Сбрасывает серию убийств игрока
     */
    public void resetKillstreak(Player player) {
        UUID playerId = player.getUniqueId();
        currentStreaks.put(playerId, 0);
    }
    
    /**
     * Проверяет, положена ли награда игроку за его серию убийств
     */
    private void checkKillstreakRewards(Player player, int streak) {
        
        for (int threshold : STREAK_THRESHOLDS) {
            if (streak == threshold) {
                
                giveKillstreakReward(player, threshold);
                
                broadcastKillstreak(player, threshold);
                break;
            }
        }
    }
    
    /**
     * Выдает награду за серию убийств
     */
    private void giveKillstreakReward(Player player, int streakLevel) {
        KillstreakType reward = getRewardForStreak(streakLevel);
        
        player.sendMessage(ChatColor.GOLD + "Вы получили награду за серию из " + streakLevel + " убийств: " + 
                ChatColor.YELLOW + reward.getDisplayName());
        
        
        switch (reward) {
            case UAV_SCAN:
                activateUAVScan(player);
                break;
            case AIR_STRIKE:
                giveAirStrikeMarker(player);
                break;
            case ARMOR_BOOST:
                activateArmorBoost(player);
                break;
        }
        
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
    
    /**
     * Определяет тип награды в зависимости от достигнутой серии убийств
     */
    private KillstreakType getRewardForStreak(int streakLevel) {
        switch (streakLevel) {
            case 3:
                return KillstreakType.UAV_SCAN;
            case 5:
                return KillstreakType.ARMOR_BOOST;
            case 7:
                return KillstreakType.AIR_STRIKE;
            default:
                return KillstreakType.UAV_SCAN; 
        }
    }
    
    /**
     * Активирует разведывательный дрон (UAV) для сканирования противников
     */
    private void activateUAVScan(Player player) {
        
        int scanRadius = plugin.getConfigManager().getMainConfig().getInt("killstreaks.uav_scan_radius", 50);
        
        player.sendMessage(ChatColor.GREEN + "Разведывательный дрон активирован! Сканирование области...");
        
        
        new BukkitRunnable() {
            private int scansRemaining = 3;
            
            @Override
            public void run() {
                if (scansRemaining <= 0) {
                    cancel();
                    player.sendMessage(ChatColor.YELLOW + "Разведывательный дрон завершил сканирование.");
                    return;
                }
                
                
                player.sendMessage(ChatColor.YELLOW + "Сканирование... " + scansRemaining + " импульса(ов) осталось.");
                performScan(player, scanRadius);
                scansRemaining--;
            }
        }.runTaskTimer(plugin, 20L, 100L); 
    }
    
    /**
     * Выполняет сканирование и подсвечивает противников
     */
    private void performScan(Player scanner, int radius) {
        Location center = scanner.getLocation();
        
        
        BCPlayer bcPlayer = plugin.getGameManager().getPlayer(scanner.getUniqueId());
        if (bcPlayer == null) return;
        
        scanner.playSound(scanner.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.0f);
        
        
        for (Player target : scanner.getWorld().getPlayers()) {
            
            if (target.equals(scanner)) continue;
            
            
            if (target.getLocation().distance(center) <= radius) {
                
                BCPlayer targetBCPlayer = plugin.getGameManager().getPlayer(target.getUniqueId());
                if (targetBCPlayer != null && targetBCPlayer.getTeamType() != bcPlayer.getTeamType()) {
                    
                    highlightEnemy(scanner, target);
                }
            }
        }
    }
    
    /**
     * Подсвечивает противника для игрока и его команды
     */
    private void highlightEnemy(Player scanner, Player enemy) {
        
        BCPlayer bcScanner = plugin.getGameManager().getPlayer(scanner.getUniqueId());
        
        if (bcScanner == null) return;
        
        
        for (Player teammate : plugin.getGameManager().getTeamManager().getTeamPlayers(bcScanner.getTeamType())) {
            Location enemyLoc = enemy.getLocation();
            teammate.sendMessage(ChatColor.RED + "[UAV] " + ChatColor.YELLOW + "Обнаружен противник: " + 
                    enemy.getName() + " на X:" + enemyLoc.getBlockX() + ", Y:" + enemyLoc.getBlockY() + 
                    ", Z:" + enemyLoc.getBlockZ());
            
            
            teammate.playSound(teammate.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 2.0f);
            
            
            
            
        }
    }
    
    /**
     * Выдает игроку маркер для вызова авиаудара
     */
    private void giveAirStrikeMarker(Player player) {
        
        ItemStack airStrikeMarker = KillstreakItems.createAirStrikeMarker();
        
        
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(airStrikeMarker);
            player.sendMessage(ChatColor.GREEN + "Вы получили маркер авиаудара! Используйте его, чтобы вызвать бомбардировку.");
        } else {
            player.sendMessage(ChatColor.RED + "Нет места в инвентаре! Маркер авиаудара не был добавлен.");
        }
    }
    
    /**
     * Активирует усиленный бронежилет для игрока
     */
    private void activateArmorBoost(Player player) {
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 1)); 
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1)); 
        
        player.sendMessage(ChatColor.GREEN + "Усиленный бронежилет активирован! Вы получили временную защиту.");
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.0f);
    }
    
    /**
     * Вызывает авиаудар в указанном месте
     */
    public void callAirStrike(Player player, Location targetLocation) {
        player.sendMessage(ChatColor.GREEN + "Авиаудар вызван по координатам X:" + 
                targetLocation.getBlockX() + ", Z:" + targetLocation.getBlockZ());
        
        
        MessageUtils.broadcastMessage(ChatColor.RED + player.getName() + 
                " вызвал авиаудар! Укройтесь!");
        
        
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        }
        
        
        new BukkitRunnable() {
            @Override
            public void run() {
                executeAirStrike(targetLocation);
            }
        }.runTaskLater(plugin, 80L); 
    }
    
    /**
     * Выполняет авиаудар в указанном месте
     */
    private void executeAirStrike(Location targetLocation) {
        
        int radius = plugin.getConfigManager().getMainConfig().getInt("killstreaks.airstrike_radius", 15);
        
        
        for (int i = 0; i < 5; i++) {
            
            double offsetX = (Math.random() - 0.5) * radius;
            double offsetZ = (Math.random() - 0.5) * radius;
            
            Location explosionLoc = targetLocation.clone().add(offsetX, 0, offsetZ);
            
            explosionLoc.setY(explosionLoc.getWorld().getHighestBlockYAt(explosionLoc));
            
            
            boolean breakBlocks = plugin.getConfigManager().getMainConfig().getBoolean("killstreaks.airstrike_breaks_blocks", false);
            float power = plugin.getConfigManager().getMainConfig().getInt("killstreaks.airstrike_power", 4);
            
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    explosionLoc.getWorld().createExplosion(explosionLoc, power, false, breakBlocks);
                }
            }.runTaskLater(plugin, i * 10); 
        }
    }
    
    /**
     * Оповещает всех о серии убийств игрока
     */
    private void broadcastKillstreak(Player player, int streak) {
        String message;
        
        switch (streak) {
            case 3:
                message = ChatColor.YELLOW + player.getName() + " выполнил серию из 3 убийств! " + 
                        ChatColor.GOLD + "UAV активирован!";
                break;
            case 5:
                message = ChatColor.GOLD + player.getName() + " выполнил серию из 5 убийств! " + 
                        ChatColor.YELLOW + "Усиленная броня получена!";
                break;
            case 7:
                message = ChatColor.RED + player.getName() + " выполнил серию из 7 убийств! " + 
                        ChatColor.GOLD + "Авиаудар доступен!";
                break;
            default:
                message = ChatColor.YELLOW + player.getName() + " выполнил серию из " + streak + " убийств!";
                break;
        }
        
        MessageUtils.broadcastMessage(message);
    }
    
    /**
     * Периодически обновляет серии убийств (проверяет тайм-аут)
     * Вызывается по таймеру
     */
    public void checkStreakTimeouts() {
        long currentTime = System.currentTimeMillis();
        
        
        for (Map.Entry<UUID, Long> entry : lastKillTime.entrySet()) {
            UUID playerId = entry.getKey();
            long lastKill = entry.getValue();
            
            
            if (currentTime - lastKill > STREAK_RESET_TIME && currentStreaks.getOrDefault(playerId, 0) > 0) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.YELLOW + "Ваша серия убийств завершилась из-за тайм-аута.");
                }
                
                
                currentStreaks.put(playerId, 0);
            }
        }
    }
    
    /**
     * Получает текущую серию убийств игрока
     */
    public int getCurrentStreak(UUID playerId) {
        return currentStreaks.getOrDefault(playerId, 0);
    }
    
    /**
     * Проверяет, есть ли у игрока активная серия убийств
     */
    public boolean hasActiveStreak(UUID playerId) {
        return getCurrentStreak(playerId) > 0;
    }
    
    /**
     * Выдает игроку разведывательный дрон (3 убийства)
     */
    public void giveReconDrone(Player player) {
        UUID playerId = player.getUniqueId();
        
        
        ItemStack drone = new ItemStack(Material.COMPASS);
        ItemMeta meta = drone.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Разведывательный дрон");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Раскрывает местоположение противников",
            ChatColor.GRAY + "в радиусе 50 блоков на 30 секунд",
            ChatColor.YELLOW + "Нажмите ПКМ для активации"
        ));
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        drone.setItemMeta(meta);
        
        
        player.getInventory().addItem(drone);
        
        
        addActiveKillstreak(playerId, KillstreakType.RECON_DRONE);
        
        
        player.sendMessage(ChatColor.GOLD + "Вы получили разведывательный дрон! ПКМ для активации.");
    }
    
    /**
     * Выдает игроку усиленный бронежилет (5 убийств)
     */
    public void giveEnhancedArmor(Player player) {
        UUID playerId = player.getUniqueId();
        
        
        ItemStack armor = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = armor.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Усиленный бронежилет");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Дает дополнительную защиту",
            ChatColor.GRAY + "и регенерацию на 60 секунд",
            ChatColor.YELLOW + "Нажмите ПКМ для активации"
        ));
        meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        armor.setItemMeta(meta);
        
        
        player.getInventory().addItem(armor);
        
        
        addActiveKillstreak(playerId, KillstreakType.ENHANCED_ARMOR);
        
        
        player.sendMessage(ChatColor.AQUA + "Вы получили усиленный бронежилет! ПКМ для активации.");
    }
    
    /**
     * Выдает игроку авиаудар (7 убийств)
     */
    public void giveAirStrike(Player player) {
        UUID playerId = player.getUniqueId();
        
        
        ItemStack airstrike = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta meta = airstrike.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Авиаудар");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Наносит урон всем противникам",
            ChatColor.GRAY + "в радиусе 10 блоков от указанной точки",
            ChatColor.YELLOW + "Нажмите ПКМ для выбора точки удара"
        ));
        meta.addEnchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        airstrike.setItemMeta(meta);
        
        
        player.getInventory().addItem(airstrike);
        
        
        addActiveKillstreak(playerId, KillstreakType.AIR_STRIKE);
        
        
        player.sendMessage(ChatColor.RED + "Вы получили авиаудар! ПКМ для выбора точки удара.");
    }
    
    /**
     * Активирует разведывательный дрон
     */
    public void activateReconDrone(Player player) {
        UUID playerId = player.getUniqueId();
        
        
        if (!hasActiveKillstreak(playerId, KillstreakType.RECON_DRONE)) {
            return;
        }
        
        
        removeActiveKillstreak(playerId, KillstreakType.RECON_DRONE);
        
        
        removeKillstreakItem(player, "Разведывательный дрон");
        
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0, false, false));
        
        
        highlightEnemies(player);
        
        
        player.sendMessage(ChatColor.GOLD + "Разведывательный дрон активирован! Вражеские позиции раскрыты на 30 секунд.");
    }
    
    /**
     * Активирует усиленный бронежилет
     */
    public void activateEnhancedArmor(Player player) {
        UUID playerId = player.getUniqueId();
        
        
        if (!hasActiveKillstreak(playerId, KillstreakType.ENHANCED_ARMOR)) {
            return;
        }
        
        
        removeActiveKillstreak(playerId, KillstreakType.ENHANCED_ARMOR);
        
        
        removeKillstreakItem(player, "Усиленный бронежилет");
        
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1200, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 0, false, false));
        
        
        player.sendMessage(ChatColor.AQUA + "Усиленный бронежилет активирован! Вы получили защиту и регенерацию на 60 секунд.");
    }
    
    /**
     * Инициирует выбор точки для авиаудара
     */
    public void initiateAirStrike(Player player) {
        UUID playerId = player.getUniqueId();
        
        
        if (!hasActiveKillstreak(playerId, KillstreakType.AIR_STRIKE)) {
            return;
        }
        
        
        removeActiveKillstreak(playerId, KillstreakType.AIR_STRIKE);
        
        
        removeKillstreakItem(player, "Авиаудар");
        
        
        BCPlayer bcPlayer = plugin.getGameManager().getPlayer(playerId);
        if (bcPlayer != null) {
            bcPlayer.setSelectingAirstrikeTarget(true);
            player.sendMessage(ChatColor.RED + "Укажите точку удара, кликнув ЛКМ по блоку.");
        }
    }
    
    /**
     * Подсвечивает всех противников в радиусе
     */
    private void highlightEnemies(Player player) {
        UUID playerId = player.getUniqueId();
        BCPlayer bcPlayer = plugin.getGameManager().getPlayer(playerId);
        if (bcPlayer == null) return;
        
        
        org.comm.battlefieldCommand.game.team.TeamType team = bcPlayer.getTeamType();
        
        
        for (Player otherPlayer : player.getWorld().getPlayers()) {
            if (otherPlayer.equals(player)) continue;
            
            
            if (otherPlayer.getLocation().distance(player.getLocation()) <= 50) {
                
                BCPlayer otherBCPlayer = plugin.getGameManager().getPlayer(otherPlayer.getUniqueId());
                if (otherBCPlayer != null && otherBCPlayer.getTeamType() != team) {
                    
                    otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 600, 0, false, false));
                }
            }
        }
    }
    
    /**
     * Удаляет предмет киллстрика из инвентаря
     */
    private void removeKillstreakItem(Player player, String displayName) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String itemName = item.getItemMeta().getDisplayName();
                if (itemName.contains(displayName)) {
                    player.getInventory().remove(item);
                    return;
                }
            }
        }
    }
    
    /**
     * Добавляет киллстрик в список активных для игрока
     */
    public void addActiveKillstreak(UUID playerId, KillstreakType type) {
        activeKillstreaks.computeIfAbsent(playerId, k -> new HashSet<>()).add(type);
    }
    
    /**
     * Удаляет киллстрик из списка активных для игрока
     */
    public void removeActiveKillstreak(UUID playerId, KillstreakType type) {
        Set<KillstreakType> playerKillstreaks = activeKillstreaks.get(playerId);
        if (playerKillstreaks != null) {
            playerKillstreaks.remove(type);
        }
    }
    
    /**
     * Проверяет, есть ли у игрока указанный киллстрик
     */
    public boolean hasActiveKillstreak(UUID playerId, KillstreakType type) {
        Set<KillstreakType> playerKillstreaks = activeKillstreaks.get(playerId);
        return playerKillstreaks != null && playerKillstreaks.contains(type);
    }
} 