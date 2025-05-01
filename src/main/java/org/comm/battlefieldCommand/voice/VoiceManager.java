package org.comm.battlefieldCommand.voice;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.comm.battlefieldCommand.BattlefieldCommand;
import org.comm.battlefieldCommand.game.GameManager;
import org.comm.battlefieldCommand.game.team.TeamType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoiceManager {

    private VoicechatServerApi api;
    private final GameManager gameManager;
    private final BattlefieldCommand plugin;
    private final Map<UUID, VoiceChannel> playerChannels = new HashMap<>();

    private Group teamAGroup, teamBGroup, tacticalGroup, allGroup;

    public VoiceManager(BattlefieldCommand plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }
    
    /**
     * Устанавливает API голосового чата
     */
    public void setApi(VoicechatServerApi api) {
        this.api = api;
        setupVoiceGroups();
    }
    
    /**
     * Проверяет, доступен ли API голосового чата
     */
    public boolean isApiAvailable() {
        return this.api != null;
    }

    /**
     * Регистрирует события подключения и отключения
     */
    public void register(EventRegistration reg) {
        reg.registerEvent(PlayerConnectedEvent.class,    this::onPlayerConnected);
        reg.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected);
    }

    /**
     * Вызывается, когда игрок заходит в голосовой чат
     */
    private void onPlayerConnected(PlayerConnectedEvent e) {
        
        VoicechatConnection conn = e.getConnection();
        UUID uuid = conn.getPlayer().getUuid();
        Player p  = plugin.getServer().getPlayer(uuid);
        if (p != null && gameManager.isPlayerInGame(p)) {
            assignPlayerToVoiceChannel(p);
        }
    }

    /**
     * Вызывается, когда игрок выходит из голосового чата
     */
    private void onPlayerDisconnected(PlayerDisconnectedEvent e) {
        
        UUID uuid = e.getPlayerUuid();
        playerChannels.remove(uuid);
    }

    /**
     * Создаёт четыре группы (канала) без пароля
     */
    public void setupVoiceGroups() {
        if (api == null) {
            plugin.getLogger().warning("Невозможно создать голосовые группы - API не установлен");
            return;
        }
        teamAGroup    = api.createGroup("team_a",    null);
        teamBGroup    = api.createGroup("team_b",    null);
        tacticalGroup = api.createGroup("tactical",  null);
        allGroup      = api.createGroup("all",       null);
        plugin.getLogger().info("Группы голосового чата созданы");
    }
    
    /**
     * Очищает голосовые группы при выключении плагина
     */
    public void cleanupVoiceGroups() {
        if (api == null) {
            plugin.getLogger().warning("Не удалось очистить голосовые группы: API не инициализирован");
            return;
        }
        
        try {
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                try {
                    VoicechatConnection conn = api.getConnectionOf(player.getUniqueId());
                    if (conn != null) {
                        conn.setGroup(null); 
                    }
                } catch (Exception e) {
                    
                    plugin.getLogger().warning("Ошибка при удалении игрока " + player.getName() + 
                                              " из голосовой группы: " + e.getMessage());
                }
            }
            
            
            teamAGroup = null;
            teamBGroup = null;
            tacticalGroup = null;
            allGroup = null;
            
            playerChannels.clear();
            
            plugin.getLogger().info("Голосовые группы очищены");
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при очистке голосовых групп: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Принудительно переводит игрока в указанный канал
     */
    public void setPlayerVoiceChannel(Player player, VoiceChannel channel) {
        if (api == null) {
            plugin.getLogger().warning("Невозможно изменить канал - API не установлен");
            return;
        }
        
        UUID uuid = player.getUniqueId();
        VoicechatConnection conn = api.getConnectionOf(uuid);
        if (conn == null) {
            plugin.getLogger().warning("Нет VoicechatConnection для " + player.getName());
            return;
        }

        Group grp = switch (channel) {
            case TEAM_A   -> teamAGroup;
            case TEAM_B   -> teamBGroup;
            case TACTICAL -> tacticalGroup;
            case ALL      -> allGroup;
        };

        conn.setGroup(grp);
        playerChannels.put(uuid, channel);
        player.sendMessage(ChatColor.GREEN + "Переключились на канал: " + channel.getDisplayName());
    }

    /**
     * По-умолчанию отправляет игрока в канал его команды
     */
    public void assignPlayerToVoiceChannel(Player player) {
        if (!isApiAvailable()) return;
        
        TeamType team = gameManager.getPlayerTeam(player);
        if (team == null) return;
        setPlayerVoiceChannel(player,
                team == TeamType.TEAM_A ? VoiceChannel.TEAM_A : VoiceChannel.TEAM_B
        );
    }

    /**
     * Циклично переключает каналы (A → тактика → общий → B → …)
     */
    public void cyclePlayerVoiceChannel(Player player) {
        if (!isApiAvailable()) return;
        
        UUID uuid = player.getUniqueId();
        VoiceChannel current = playerChannels.getOrDefault(uuid, VoiceChannel.ALL);
        TeamType team = gameManager.getPlayerTeam(player);
        if (team == null) return;

        VoiceChannel next = switch (current) {
            case TEAM_A   -> (team == TeamType.TEAM_A ? VoiceChannel.TACTICAL : VoiceChannel.TEAM_B);
            case TEAM_B   -> (team == TeamType.TEAM_B ? VoiceChannel.TACTICAL : VoiceChannel.TEAM_A);
            case TACTICAL -> VoiceChannel.ALL;
            case ALL      -> (team == TeamType.TEAM_A ? VoiceChannel.TEAM_A : VoiceChannel.TEAM_B);
        };

        setPlayerVoiceChannel(player, next);
    }
}
