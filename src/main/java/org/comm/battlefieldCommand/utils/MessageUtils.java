package org.comm.battlefieldCommand.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.Collection;

/**
 * Утилиты для отправки сообщений игрокам
 */
public class MessageUtils {
    private static final String PREFIX = ChatColor.GOLD + "[BC] " + ChatColor.RESET;
    
    /**
     * Отправляет сообщение всем игрокам
     */
    public static void broadcastMessage(String message) {
        Bukkit.broadcastMessage(PREFIX + message);
    }
    
    /**
     * Отправляет сообщение всем игрокам без префикса
     */
    public static void broadcastRawMessage(String message) {
        Bukkit.broadcastMessage(message);
    }
    
    /**
     * Отправляет заголовок всем игрокам
     */
    public static void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }
    
    /**
     * Отправляет сообщение в боковой чат (actionbar) всем игрокам
     */
    public static void broadcastActionBar(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }
    
    /**
     * Отправляет сообщение игроку
     */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(PREFIX + message);
    }
    
    /**
     * Отправляет сообщение игроку без префикса
     */
    public static void sendRawMessage(Player player, String message) {
        player.sendMessage(message);
    }
    
    /**
     * Отправляет заголовок игроку
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
    
    /**
     * Отправляет сообщение в боковой чат (actionbar) игроку
     */
    public static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
    
    /**
     * Форматирует время в формате MM:SS
     */
    public static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
    
    /**
     * Отправляет сообщение об ошибке игроку
     */
    public static void sendErrorMessage(Player player, String message) {
        player.sendMessage(PREFIX + ChatColor.RED + message);
    }
    
    /**
     * Отправляет сообщение об успехе игроку
     */
    public static void sendSuccessMessage(Player player, String message) {
        player.sendMessage(PREFIX + ChatColor.GREEN + message);
    }
    
    /**
     * Отправляет информационное сообщение игроку
     */
    public static void sendInfoMessage(Player player, String message) {
        player.sendMessage(PREFIX + ChatColor.YELLOW + message);
    }
    
    /**
     * Отправляет сообщение указанному списку игроков
     */
    public static void broadcastToPlayers(Collection<? extends Player> players, String message) {
        for (Player player : players) {
            player.sendMessage(PREFIX + message);
        }
    }
} 