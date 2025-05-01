package org.comm.battlefieldCommand;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;

import org.comm.battlefieldCommand.commands.CommandManager;
import org.comm.battlefieldCommand.config.ConfigManager;
import org.comm.battlefieldCommand.game.GameManager;
import org.comm.battlefieldCommand.game.killstreak.KillstreakManager;
import org.comm.battlefieldCommand.listeners.ListenerManager;
import org.comm.battlefieldCommand.voice.VoiceManager;
import org.comm.battlefieldCommand.voice.RadioMessageSystem;
import org.comm.battlefieldCommand.equipment.EquipmentManager;
import org.comm.battlefieldCommand.game.map.MapGenerator;
import org.comm.battlefieldCommand.game.arena.ArenaSetupManager;
import org.comm.battlefieldCommand.game.arena.ArenaSetupManager.SetupSession;

public final class BattlefieldCommand extends JavaPlugin implements VoicechatPlugin {
    private static BattlefieldCommand instance;

    private ConfigManager    configManager;
    private GameManager      gameManager;
    private CommandManager   commandManager;
    private ListenerManager  listenerManager;
    private VoiceManager     voiceManager;
    private RadioMessageSystem radioMessageSystem;
    private KillstreakManager killstreakManager;
    private EquipmentManager equipmentManager;

    public static BattlefieldCommand getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager()       { return configManager; }
    public GameManager   getGameManager()         { return gameManager; }
    public CommandManager getCommandManager()     { return commandManager; }
    public VoiceManager  getVoiceManager()        { return voiceManager; }
    public RadioMessageSystem getRadioMessageSystem() { return radioMessageSystem; }
    public KillstreakManager getKillstreakManager() { return killstreakManager; }
    public EquipmentManager getEquipmentManager() { return equipmentManager; }

    @Override
    public void onEnable() {
        instance = this;

        
        saveDefaultConfig();
        configManager   = new ConfigManager(this);
        gameManager     = new GameManager(this);
        commandManager  = new CommandManager(this);
        listenerManager = new ListenerManager(this);
        
        
        try {
            voiceManager = new VoiceManager(this);
            radioMessageSystem = new RadioMessageSystem(this, voiceManager);
        } catch (Exception e) {
            getLogger().severe("Ошибка при инициализации голосового чата: " + e.getMessage());
            getLogger().warning("Голосовой чат будет отключен.");
            voiceManager = null;
            radioMessageSystem = null;
        }
        
        killstreakManager = new KillstreakManager(this);
        equipmentManager = new EquipmentManager(this);

        commandManager.registerCommands();
        listenerManager.registerListeners();

        
        if (!gameManager.hasArenaSetup()) {
            getLogger().info("Арена не настроена. Запуск автоматической настройки...");
            getServer().getScheduler().runTaskLater(this, () -> {
                MapGenerator mapGenerator = gameManager.getMapGenerator();
                Player player;
                
                if (getServer().getOnlinePlayers().size() > 0) {
                    player = getServer().getOnlinePlayers().iterator().next();
                } else {
                    player = null;
                }

                if (player != null) {
                    
                    ArenaSetupManager setupManager = gameManager.getArenaSetupManager();
                    setupManager.startSetup(player);
                    
                    
                    Location centerLocation = player.getLocation();
                    SetupSession session = setupManager.getSetupSession(player);
                    
                    if (session != null) {
                        
                        player.sendMessage(ChatColor.GREEN + "Автоматическая генерация карты...");
                        setupManager.generateMap(player, "Автоматическая Арена", "default", centerLocation);
                        
                        
                        getServer().getScheduler().runTaskLater(this, () -> {
                            if (setupManager.isPlayerInSetupMode(player)) {
                                setupManager.saveArena(player);
                                getLogger().info("Арена автоматически сохранена");
                                
                                
                                getServer().getScheduler().runTaskLater(this, () -> {
                                    
                                    getLogger().info("Загрузка арены после сохранения...");
                                    gameManager.loadArenaFromConfig();
                                    
                                    
                                    getServer().getScheduler().runTaskLater(this, () -> {
                                        
                                        if (gameManager.hasArenaSetup()) {
                                            getLogger().info("Запуск игры с загруженной ареной...");
                                            gameManager.startGame();
                                            getServer().broadcastMessage(ChatColor.GOLD + "[BattlefieldCommand] " + 
                                                ChatColor.GREEN + "Игра автоматически запущена! Используйте /bc join для присоединения.");
                                        } else {
                                            getLogger().warning("Не удалось запустить игру: арена не настроена должным образом");
                                            getServer().broadcastMessage(ChatColor.GOLD + "[BattlefieldCommand] " + 
                                                ChatColor.RED + "Не удалось запустить игру автоматически. Используйте /bc setup для ручной настройки.");
                                        }
                                    }, 40L); 
                                }, 20L); 
                            }
                        }, 100L); 
                        
                        player.sendMessage(ChatColor.GREEN + "Карта сгенерирована. Используйте /bc join для присоединения к игре.");
                        
                        
                    }
                } else {
                    getLogger().info("Не удалось найти игроков для автоматической настройки арены.");
                    getLogger().info("Используйте команду /bc setup для ручной настройки.");
                }
            }, 100L); 
        }

        
        try {
            org.bukkit.plugin.RegisteredServiceProvider<BukkitVoicechatService> provider = 
                Bukkit.getServicesManager().getRegistration(BukkitVoicechatService.class);
            
            if (provider != null) {
                BukkitVoicechatService service = provider.getProvider();
                if (service != null) {
                    service.registerPlugin(this);
                } else {
                    getLogger().warning("Simple Voice Chat service не найден - голосовой чат отключен");
                }
            } else {
                getLogger().warning("Simple Voice Chat service не зарегистрирован - голосовой чат отключен");
            }
        } catch (Exception e) {
            getLogger().warning("Ошибка при регистрации в Simple Voice Chat: " + e.getMessage());
        }

        getLogger().info("Battlefield Command успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
        getLogger().info("Battlefield Command выключен!");
    }

    
    
    @Override
    public String getPluginId() {
        return "battlefieldcommand";
    }

    
    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi serverApi) {
            voiceManager.setApi(serverApi);
            getLogger().info("VoiceManager инициализирован с API");
        }
    }

    
    @Override
    public void registerEvents(EventRegistration registration) {
        if (voiceManager != null) {
            try {
                voiceManager.register(registration);
            } catch (Exception e) {
                getLogger().warning("Ошибка при регистрации обработчиков голосового чата: " + e.getMessage());
            }
        }
        
        if (radioMessageSystem != null) {
            try {
                radioMessageSystem.register(registration);
            } catch (Exception e) {
                getLogger().warning("Ошибка при регистрации системы радио: " + e.getMessage());
            }
        }
    }
}
