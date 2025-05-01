package org.comm.battlefieldCommand.game;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Управляет экипировкой игроков разных классов
 */
public class EquipmentManager {
    
    /**
     * Выдает экипировку штурмовика
     */
    public static void equipAssault(Player player, TeamType teamType) {
        
        player.getInventory().clear();
        
        
        ItemStack helmet = createColoredArmor(Material.LEATHER_HELMET, "Шлем штурмовика", teamType.getColor());
        ItemStack chestplate = createColoredArmor(Material.LEATHER_CHESTPLATE, "Бронежилет штурмовика", teamType.getColor());
        ItemStack leggings = createColoredArmor(Material.LEATHER_LEGGINGS, "Поножи штурмовика", teamType.getColor());
        ItemStack boots = createColoredArmor(Material.LEATHER_BOOTS, "Ботинки штурмовика", teamType.getColor());
        
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        
        
        ItemStack rifle = createWeapon(Material.IRON_SWORD, "Штурмовая винтовка", 
                "Мощное основное оружие штурмовика",
                new EnchantmentData(Enchantment.SHARPNESS, 3));
        
        
        ItemStack pistol = createWeapon(Material.WOODEN_SWORD, "Пистолет", 
                "Запасное оружие для ближнего боя",
                new EnchantmentData(Enchantment.SHARPNESS, 1));
        
        
        ItemStack grenades = createItem(Material.EGG, ChatColor.RED + "Граната",
                ChatColor.GRAY + "Бросьте, чтобы нанести урон");
        grenades.setAmount(3);
        
        
        ItemStack medkit = createPotion(Material.POTION, ChatColor.RED + "Аптечка",
                ChatColor.GRAY + "ПКМ для лечения", Color.RED,
                new PotionEffectData(PotionEffectType.REGENERATION, 1, 2));
        
        
        ItemStack dash = createPotion(Material.SPLASH_POTION, ChatColor.YELLOW + "Рывок",
                ChatColor.GRAY + "Бросьте под ноги для рывка", Color.YELLOW,
                new PotionEffectData(PotionEffectType.SPEED, 5, 2));
        
        
        player.getInventory().setItem(0, rifle);
        player.getInventory().setItem(1, pistol);
        player.getInventory().setItem(2, grenades);
        player.getInventory().setItem(7, medkit);
        player.getInventory().setItem(8, dash);
    }
    
    /**
     * Выдает экипировку снайпера
     */
    public static void equipSniper(Player player, TeamType teamType) {
        
        player.getInventory().clear();
        
        
        ItemStack helmet = createColoredArmor(Material.LEATHER_HELMET, "Маскировочный шлем", teamType.getColor());
        ItemStack chestplate = createColoredArmor(Material.LEATHER_CHESTPLATE, "Маскировочный жилет", teamType.getColor());
        ItemStack leggings = createColoredArmor(Material.LEATHER_LEGGINGS, "Маскировочные поножи", teamType.getColor());
        ItemStack boots = createColoredArmor(Material.LEATHER_BOOTS, "Маскировочные ботинки", teamType.getColor());
        
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        
        
        ItemStack sniperRifle = createWeapon(Material.BOW, "Снайперская винтовка", 
                "Точное дальнобойное оружие снайпера",
                new EnchantmentData(Enchantment.POWER, 5),
                new EnchantmentData(Enchantment.INFINITY, 1));
        
        
        ItemStack pistol = createWeapon(Material.WOODEN_SWORD, "Пистолет", 
                "Запасное оружие для ближнего боя",
                new EnchantmentData(Enchantment.SHARPNESS, 1));
        
        
        ItemStack ammunition = new ItemStack(Material.ARROW);
        
        
        ItemStack taggers = createItem(Material.SNOWBALL, ChatColor.WHITE + "Маскировочная метка",
                ChatColor.GRAY + "Бросьте, чтобы отметить врага");
        taggers.setAmount(5);
        
        
        ItemStack invisibility = createPotion(Material.POTION, ChatColor.AQUA + "Невидимость",
                ChatColor.GRAY + "ПКМ для активации невидимости", Color.WHITE,
                new PotionEffectData(PotionEffectType.INVISIBILITY, 10, 1));
        
        
        player.getInventory().setItem(0, sniperRifle);
        player.getInventory().setItem(1, pistol);
        player.getInventory().setItem(2, taggers);
        player.getInventory().setItem(7, invisibility);
        player.getInventory().setItem(9, ammunition);
    }
    
    /**
     * Выдает экипировку инженера
     */
    public static void equipEngineer(Player player, TeamType teamType) {
        
        player.getInventory().clear();
        
        
        ItemStack helmet = createColoredArmor(Material.LEATHER_HELMET, "Шлем инженера", teamType.getColor());
        ItemStack chestplate = createColoredArmor(Material.LEATHER_CHESTPLATE, "Комбинезон инженера", teamType.getColor());
        ItemStack leggings = createColoredArmor(Material.LEATHER_LEGGINGS, "Поножи инженера", teamType.getColor());
        ItemStack boots = createColoredArmor(Material.LEATHER_BOOTS, "Ботинки инженера", teamType.getColor());
        
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        
        
        ItemStack shotgun = createWeapon(Material.GOLDEN_SWORD, "Дробовик", 
                "Мощное оружие ближнего боя",
                new EnchantmentData(Enchantment.SHARPNESS, 4),
                new EnchantmentData(Enchantment.KNOCKBACK, 1));
        
        
        ItemStack repairTools = createWeapon(Material.IRON_PICKAXE, "Ремонтный инструмент", 
                "Чините турели и укрепления",
                new EnchantmentData(Enchantment.UNBREAKING, 3));
        
        
        ItemStack mineKit = createItem(Material.STONE_PRESSURE_PLATE, ChatColor.GRAY + "Мина",
                ChatColor.GRAY + "Размещение активирует ловушку");
        mineKit.setAmount(2);
        
        
        ItemStack repairDrone = createPotion(Material.LINGERING_POTION, ChatColor.GREEN + "Ремонтный дрон",
                ChatColor.GRAY + "Бросьте для лечения техники и игроков", Color.GREEN,
                new PotionEffectData(PotionEffectType.REGENERATION, 10, 1));
        
        
        player.getInventory().setItem(0, shotgun);
        player.getInventory().setItem(1, repairTools);
        player.getInventory().setItem(2, mineKit);
        player.getInventory().setItem(8, repairDrone);
    }
    
    /**
     * Создает окрашенную броню
     */
    private static ItemStack createColoredArmor(Material material, String name, Color color) {
        ItemStack armor = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + name);
            meta.setColor(color);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            armor.setItemMeta(meta);
        }
        
        return armor;
    }
    
    /**
     * Создает зачарованное оружие
     */
    private static ItemStack createWeapon(Material material, String name, String description, EnchantmentData... enchantments) {
        ItemStack weapon = new ItemStack(material);
        ItemMeta meta = weapon.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + name);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            meta.setLore(lore);
            
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            
            for (EnchantmentData enchant : enchantments) {
                meta.addEnchant(enchant.enchantment, enchant.level, true);
            }
            
            weapon.setItemMeta(meta);
        }
        
        return weapon;
    }
    
    /**
     * Создает обычный предмет
     */
    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Создает зелье с эффектом
     */
    private static ItemStack createPotion(Material material, String name, String description, Color color, PotionEffectData... effects) {
        ItemStack potion = new ItemStack(material);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            List<String> lore = new ArrayList<>();
            lore.add(description);
            meta.setLore(lore);
            
            meta.setColor(color);
            
            for (PotionEffectData effect : effects) {
                meta.addCustomEffect(new PotionEffect(effect.type, effect.duration * 20, effect.amplifier), true);
            }
            
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            potion.setItemMeta(meta);
        }
        
        return potion;
    }
    
    /**
     * Класс для хранения данных о зачаровании
     */
    private static class EnchantmentData {
        private final Enchantment enchantment;
        private final int level;
        
        public EnchantmentData(Enchantment enchantment, int level) {
            this.enchantment = enchantment;
            this.level = level;
        }
    }
    
    /**
     * Класс для хранения данных об эффекте зелья
     */
    private static class PotionEffectData {
        private final PotionEffectType type;
        private final int duration;
        private final int amplifier;
        
        public PotionEffectData(PotionEffectType type, int duration, int amplifier) {
            this.type = type;
            this.duration = duration;
            this.amplifier = amplifier;
        }
    }
} 