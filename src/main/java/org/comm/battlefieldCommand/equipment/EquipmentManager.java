package org.comm.battlefieldCommand.equipment;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.Color;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.player.ClassType;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.Arrays;

/**
 * Управляет экипировкой и снаряжением игроков
 */
public class EquipmentManager {
    private final BattlefieldCommand plugin;
    
    public EquipmentManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Экипирует игрока снаряжением в соответствии с его классом
     */
    public void equipPlayerByClass(Player player, ClassType classType) {
        if (player == null || classType == null) return;
        
        
        player.getInventory().clear();
        
        
        switch (classType) {
            case ASSAULT:
                equipAssault(player);
                break;
            case SNIPER:
                equipSniper(player);
                break;
            case ENGINEER:
                equipEngineer(player);
                break;
            default:
                equipDefault(player);
                break;
        }
        
        
        equipCommonItems(player);
        
        
        TeamType teamType = plugin.getGameManager().getPlayerTeam(player);
        if (teamType != null) {
            applyTeamColors(player, teamType);
        }
    }
    
    /**
     * Экипирует штурмовика
     */
    private void equipAssault(Player player) {
        
        ItemStack weapon = createItem(Material.IRON_SWORD, ChatColor.GOLD + "Штурмовая винтовка",
                ChatColor.GRAY + "Хорошо сбалансированное оружие ближнего боя");
        player.getInventory().setItem(0, weapon);
        
        
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
    }
    
    /**
     * Экипирует снайпера
     */
    private void equipSniper(Player player) {
        
        ItemStack weapon = createItem(Material.BOW, ChatColor.AQUA + "Снайперская винтовка",
                ChatColor.GRAY + "Мощное дальнобойное оружие");
        player.getInventory().setItem(0, weapon);
        
        
        ItemStack ammo = new ItemStack(Material.ARROW, 32);
        player.getInventory().setItem(9, ammo);
        
        
        player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
    }
    
    /**
     * Экипирует инженера
     */
    private void equipEngineer(Player player) {
        
        ItemStack weapon = createItem(Material.GOLDEN_SWORD, ChatColor.YELLOW + "Пистолет-пулемёт",
                ChatColor.GRAY + "Надежное оружие для среднего радиуса боя");
        player.getInventory().setItem(0, weapon);
        
        
        ItemStack repairKit = createItem(Material.SHEARS, ChatColor.GREEN + "Ремонтный комплект",
                ChatColor.GRAY + "Позволяет ремонтировать технику и строения");
        player.getInventory().setItem(1, repairKit);
        
        
        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
    }
    
    /**
     * Экипирует стандартным набором (если класс не определен)
     */
    private void equipDefault(Player player) {
        
        ItemStack weapon = createItem(Material.WOODEN_SWORD, ChatColor.WHITE + "Пистолет",
                ChatColor.GRAY + "Стандартное оружие");
        player.getInventory().setItem(0, weapon);
        
        
        player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
    }
    
    /**
     * Выдает общие предметы для всех классов
     */
    private void equipCommonItems(Player player) {
        
        ItemStack radio = createItem(Material.CLOCK, ChatColor.AQUA + "Тактическая рация",
                ChatColor.GRAY + "Используется для голосовой связи с командой");
        player.getInventory().setItem(8, radio);
    }
    
    /**
     * Применяет цветовую маркировку команды к броне игрока
     */
    private void applyTeamColors(Player player, TeamType teamType) {
        Color teamColor;
        
        switch (teamType) {
            case TEAM_A:
                teamColor = Color.fromRGB(0, 0, 255); 
                break;
            case TEAM_B:
                teamColor = Color.fromRGB(255, 0, 0); 
                break;
            default:
                teamColor = Color.fromRGB(128, 128, 128); 
                break;
        }
        
        
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null && armorPiece.getType().name().startsWith("LEATHER_")) {
                LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
                if (meta != null) {
                    meta.setColor(teamColor);
                    armorPiece.setItemMeta(meta);
                }
            }
        }
    }
    
    /**
     * Создает предмет с названием и описанием
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Статический метод для экипировки штурмовика
     */
    public static void equipAssault(Player player, TeamType teamType) {
        BattlefieldCommand plugin = BattlefieldCommand.getInstance();
        EquipmentManager manager = plugin.getEquipmentManager();
        manager.equipAssault(player);
        manager.applyTeamColors(player, teamType);
    }
    
    /**
     * Статический метод для экипировки снайпера
     */
    public static void equipSniper(Player player, TeamType teamType) {
        BattlefieldCommand plugin = BattlefieldCommand.getInstance();
        EquipmentManager manager = plugin.getEquipmentManager();
        manager.equipSniper(player);
        manager.applyTeamColors(player, teamType);
    }
    
    /**
     * Статический метод для экипировки инженера
     */
    public static void equipEngineer(Player player, TeamType teamType) {
        BattlefieldCommand plugin = BattlefieldCommand.getInstance();
        EquipmentManager manager = plugin.getEquipmentManager();
        manager.equipEngineer(player);
        manager.applyTeamColors(player, teamType);
    }
} 