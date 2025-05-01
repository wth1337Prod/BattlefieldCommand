package org.comm.battlefieldCommand.game.environment;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.arena.Arena;
import org.comm.battlefieldCommand.utils.MessageUtils;

import java.util.Random;

/**
 * Менеджер погоды и времени суток
 * Отвечает за динамическую смену погоды и времени в ходе матча
 */
public class WeatherManager {
    private final BattlefieldCommand plugin;
    private final Random random = new Random();
    
    
    private WeatherType currentWeather = WeatherType.CLEAR;
    private TimeOfDay currentTime = TimeOfDay.DAY;
    
    
    private int weatherTimerTaskId = -1;
    
    
    private int weatherChangeChance = 30; 
    private int timeChangeChance = 20;    
    
    
    private static final long WEATHER_CHECK_INTERVAL = 6000L; 
    
    public WeatherManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Загружает настройки из конфигурации
     */
    private void loadConfig() {
        weatherChangeChance = plugin.getConfigManager().getMainConfig().getInt("weather.change_chance", 30);
        timeChangeChance = plugin.getConfigManager().getMainConfig().getInt("weather.time_change_chance", 20);
    }
    
    /**
     * Запускает систему динамической погоды для арены
     */
    public void startDynamicWeather(Arena arena) {
        if (arena == null || arena.getWorld() == null) {
            return;
        }
        
        
        resetWeather(arena.getWorld());
        
        
        startWeatherTimer(arena);
    }
    
    /**
     * Останавливает систему динамической погоды
     */
    public void stopDynamicWeather() {
        if (weatherTimerTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(weatherTimerTaskId);
            weatherTimerTaskId = -1;
        }
    }
    
    /**
     * Сбрасывает погоду и время суток на стандартные
     */
    public void resetWeather(World world) {
        if (world == null) return;
        
        
        world.setStorm(false);
        world.setThundering(false);
        
        
        world.setTime(1000); 
        
        currentWeather = WeatherType.CLEAR;
        currentTime = TimeOfDay.DAY;
    }
    
    /**
     * Запускает таймер для случайной смены погоды
     */
    private void startWeatherTimer(Arena arena) {
        weatherTimerTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (arena == null || arena.getWorld() == null) {
                    cancel();
                    return;
                }
                
                
                if (random.nextInt(100) < weatherChangeChance) {
                    changeWeather(arena.getWorld());
                }
                
                
                if (random.nextInt(100) < timeChangeChance) {
                    changeTimeOfDay(arena.getWorld());
                }
            }
        }.runTaskTimer(plugin, WEATHER_CHECK_INTERVAL, WEATHER_CHECK_INTERVAL).getTaskId();
    }
    
    /**
     * Меняет текущую погоду случайным образом
     */
    private void changeWeather(World world) {
        
        WeatherType newWeather;
        do {
            newWeather = WeatherType.values()[random.nextInt(WeatherType.values().length)];
        } while (newWeather == currentWeather);
        
        
        applyWeather(world, newWeather);
        
        
        broadcastWeatherChange(newWeather);
    }
    
    /**
     * Применяет указанную погоду к миру
     */
    private void applyWeather(World world, WeatherType weather) {
        if (world == null) return;
        
        currentWeather = weather;
        
        switch (weather) {
            case CLEAR:
                world.setStorm(false);
                world.setThundering(false);
                break;
            case RAIN:
                world.setStorm(true);
                world.setThundering(false);
                break;
            case THUNDER:
                world.setStorm(true);
                world.setThundering(true);
                break;
            case FOG:
                
                world.setStorm(true);
                world.setThundering(false);
                
                
                
                
                break;
        }
    }
    
    /**
     * Меняет время суток случайным образом
     */
    private void changeTimeOfDay(World world) {
        
        TimeOfDay newTime;
        do {
            newTime = TimeOfDay.values()[random.nextInt(TimeOfDay.values().length)];
        } while (newTime == currentTime);
        
        
        applyTimeOfDay(world, newTime);
        
        
        broadcastTimeChange(newTime);
    }
    
    /**
     * Применяет указанное время суток к миру
     */
    private void applyTimeOfDay(World world, TimeOfDay time) {
        if (world == null) return;
        
        currentTime = time;
        
        switch (time) {
            case DAY:
                world.setTime(1000); 
                break;
            case SUNSET:
                world.setTime(12000); 
                break;
            case NIGHT:
                world.setTime(18000); 
                break;
            case DAWN:
                world.setTime(23000); 
                break;
        }
    }
    
    /**
     * Оповещает всех игроков о смене погоды
     */
    private void broadcastWeatherChange(WeatherType weather) {
        String message;
        ChatColor color;
        
        switch (weather) {
            case CLEAR:
                message = "Погода проясняется.";
                color = ChatColor.YELLOW;
                break;
            case RAIN:
                message = "Начинается дождь. Снижена видимость и точность стрельбы.";
                color = ChatColor.BLUE;
                break;
            case THUNDER:
                message = "Начинается гроза! Повышенная опасность на открытой местности.";
                color = ChatColor.RED;
                break;
            case FOG:
                message = "Область покрывается туманом. Значительно снижена видимость.";
                color = ChatColor.GRAY;
                break;
            default:
                message = "Погода меняется.";
                color = ChatColor.WHITE;
                break;
        }
        
        MessageUtils.broadcastMessage(color + message);
    }
    
    /**
     * Оповещает всех игроков о смене времени суток
     */
    private void broadcastTimeChange(TimeOfDay time) {
        String message;
        ChatColor color;
        
        switch (time) {
            case DAY:
                message = "Наступает день. Хорошая видимость на всей карте.";
                color = ChatColor.YELLOW;
                break;
            case SUNSET:
                message = "Солнце садится. Видимость постепенно снижается.";
                color = ChatColor.GOLD;
                break;
            case NIGHT:
                message = "Наступает ночь. Снайперы получают преимущество в маскировке.";
                color = ChatColor.DARK_BLUE;
                break;
            case DAWN:
                message = "Наступает рассвет. Видимость постепенно улучшается.";
                color = ChatColor.AQUA;
                break;
            default:
                message = "Время суток меняется.";
                color = ChatColor.WHITE;
                break;
        }
        
        MessageUtils.broadcastMessage(color + message);
    }
    
    /**
     * Применяет указанные эффекты погоды к игрокам (отдача от оружия, скорость и т.д.)
     * Вызывается из GameManager для применения эффектов к активным игрокам
     */
    public void applyWeatherEffects() {
        
        
        
        
        
        
        
        
    }
    
    /**
     * Получает текущий тип погоды
     */
    public WeatherType getCurrentWeather() {
        return currentWeather;
    }
    
    /**
     * Получает текущее время суток
     */
    public TimeOfDay getCurrentTimeOfDay() {
        return currentTime;
    }
} 