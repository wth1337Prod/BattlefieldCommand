package org.comm.battlefieldCommand.game.player;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.comm.battlefieldCommand.BattlefieldCommand;

/**
 * Управление способностями игроков в зависимости от их класса
 */
public class PlayerAbility {
    private final BattlefieldCommand plugin;
    
    
    private static final int ASSAULT_DASH_COOLDOWN = 15;
    private static final int SNIPER_STEALTH_COOLDOWN = 30;
    private static final int ENGINEER_REPAIR_COOLDOWN = 25;
    
    public PlayerAbility(BattlefieldCommand plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Активирует основную способность игрока в зависимости от его класса
     */
    public boolean activateAbility(Player player, BCPlayer bcPlayer) {
        if (player == null || bcPlayer == null) {
            return false;
        }
        
        ClassType classType = bcPlayer.getClassType();
        
        switch (classType) {
            case ASSAULT:
                return activateAssaultDash(player);
            case SNIPER:
                return activateSniperStealth(player);
            case ENGINEER:
                return activateEngineerRepair(player);
            default:
                return false;
        }
    }
    
    /**
     * Активирует способность "Рывок" для штурмовика
     */
    private boolean activateAssaultDash(Player player) {
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 3)); 
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 1)); 
        
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1)); 
        
        
        player.sendMessage(ChatColor.GREEN + "Рывок активирован! Вы временно ускорены.");
        
        
        setCooldown(player, "ability.assault.dash", ASSAULT_DASH_COOLDOWN);
        
        return true;
    }
    
    /**
     * Активирует способность "Маскировка" для снайпера
     */
    private boolean activateSniperStealth(Player player) {
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0)); 
        
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0)); 
        
        
        player.sendMessage(ChatColor.GREEN + "Режим маскировки активирован! Вы временно невидимы.");
        
        
        setCooldown(player, "ability.sniper.stealth", SNIPER_STEALTH_COOLDOWN);
        
        return true;
    }
    
    /**
     * Активирует способность "Ремонтный дрон" для инженера
     */
    private boolean activateEngineerRepair(Player player) {
        
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0)); 
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0)); 
        
        
        player.sendMessage(ChatColor.GREEN + "Ремонтный дрон запущен! Вы временно усилены.");
        
        
        setCooldown(player, "ability.engineer.repair", ENGINEER_REPAIR_COOLDOWN);
        
        return true;
    }
    
    /**
     * Проверяет, доступна ли способность игрока (не на перезарядке)
     */
    public boolean isAbilityAvailable(Player player, ClassType classType) {
        switch (classType) {
            case ASSAULT:
                return !hasCooldown(player, "ability.assault.dash");
            case SNIPER:
                return !hasCooldown(player, "ability.sniper.stealth");
            case ENGINEER:
                return !hasCooldown(player, "ability.engineer.repair");
            default:
                return false;
        }
    }
    
    /**
     * Устанавливает перезарядку способности
     */
    private void setCooldown(Player player, String ability, int cooldownSeconds) {
        String metadataKey = "bc_cooldown_" + ability;
        long cooldownUntil = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        player.setMetadata(metadataKey, new org.bukkit.metadata.FixedMetadataValue(plugin, cooldownUntil));
    }
    
    /**
     * Проверяет, находится ли способность на перезарядке
     */
    private boolean hasCooldown(Player player, String ability) {
        String metadataKey = "bc_cooldown_" + ability;
        
        if (!player.hasMetadata(metadataKey)) {
            return false;
        }
        
        long cooldownUntil = player.getMetadata(metadataKey).get(0).asLong();
        return System.currentTimeMillis() < cooldownUntil;
    }
    
    /**
     * Получает оставшееся время перезарядки способности (в секундах)
     */
    public int getRemainingCooldown(Player player, ClassType classType) {
        String ability;
        
        switch (classType) {
            case ASSAULT:
                ability = "ability.assault.dash";
                break;
            case SNIPER:
                ability = "ability.sniper.stealth";
                break;
            case ENGINEER:
                ability = "ability.engineer.repair";
                break;
            default:
                return 0;
        }
        
        String metadataKey = "bc_cooldown_" + ability;
        
        if (!player.hasMetadata(metadataKey)) {
            return 0;
        }
        
        long cooldownUntil = player.getMetadata(metadataKey).get(0).asLong();
        long remainingMillis = cooldownUntil - System.currentTimeMillis();
        
        if (remainingMillis <= 0) {
            return 0;
        }
        
        return (int) (remainingMillis / 1000);
    }
} 