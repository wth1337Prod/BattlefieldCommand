package org.comm.battlefieldCommand.game.killstreak;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.comm.battlefieldCommand.BattlefieldCommand;

import java.util.Arrays;
import java.util.List;

/**
 * Класс для создания предметов, связанных с киллстриками
 */
public class KillstreakItems {
    
    
    public static final String AIRSTRIKE_MARKER_KEY = "bc_airstrike_marker";
    
    /**
     * Создает предмет разведывательного дрона
     */
    public static ItemStack createReconDrone() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "Разведывательный дрон");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Раскрывает местоположение противников",
            ChatColor.GRAY + "в радиусе 50 блоков на 30 секунд",
            ChatColor.YELLOW + "Нажмите ПКМ для активации"
        ));
        
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Создает предмет усиленного бронежилета
     */
    public static ItemStack createEnhancedArmor() {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "Усиленный бронежилет");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Дает дополнительную защиту",
            ChatColor.GRAY + "и регенерацию на 60 секунд",
            ChatColor.YELLOW + "Нажмите ПКМ для активации"
        ));
        
        meta.addEnchant(Enchantment.PROTECTION, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Создает предмет авиаудара
     */
    public static ItemStack createAirStrikeMarker() {
        ItemStack item = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.RED + "Авиаудар");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Наносит урон всем противникам",
            ChatColor.GRAY + "в радиусе 10 блоков от указанной точки",
            ChatColor.YELLOW + "Нажмите ПКМ для выбора точки удара"
        ));
        
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Проверяет, является ли предмет маркером авиаудара
     * @param item Предмет для проверки
     * @return true, если предмет является маркером авиаудара
     */
    public static boolean isAirStrikeMarker(ItemStack item) {
        if (item == null || item.getType() != Material.FIRE_CHARGE) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        NamespacedKey key = new NamespacedKey(BattlefieldCommand.getInstance(), AIRSTRIKE_MARKER_KEY);
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
    
    /**
     * Создает предмет индикатора усиленного бронежилета
     * @return Предмет для отображения в инвентаре
     */
    public static ItemStack createArmorBoostIndicator() {
        ItemStack indicator = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta meta = indicator.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Усиленный бронежилет");
            
            List<String> lore = Arrays.asList(
                    ChatColor.GRAY + "Киллстрик за серию из 5 убийств",
                    ChatColor.YELLOW + "Дает временное повышение защиты",
                    ChatColor.YELLOW + "Время действия: 30 секунд"
            );
            meta.setLore(lore);
            
            
            meta.addEnchant(Enchantment.PROTECTION, 5, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            indicator.setItemMeta(meta);
        }
        
        return indicator;
    }
    
    /**
     * Создает предмет индикатора разведывательного дрона
     * @return Предмет для отображения в инвентаре
     */
    public static ItemStack createUAVScanIndicator() {
        ItemStack indicator = new ItemStack(Material.COMPASS);
        ItemMeta meta = indicator.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Разведывательный дрон");
            
            List<String> lore = Arrays.asList(
                    ChatColor.GRAY + "Киллстрик за серию из 3 убийств",
                    ChatColor.YELLOW + "Обнаруживает противников в большом радиусе",
                    ChatColor.YELLOW + "Производит три последовательных сканирования"
            );
            meta.setLore(lore);
            
            indicator.setItemMeta(meta);
        }
        
        return indicator;
    }
} 