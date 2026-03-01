package net.omni.chatreport.db;

import net.omni.chatreport.OmniChatReport;
import net.omni.chatreport.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.*;

public class RedisHandler {

    private static final String CHANNEL = "ocr_mutes";

    private final OmniChatReport plugin;
    private final RedisClient redisClient;
    private boolean subscribed = false;
    private Jedis jedis;
    private JedisPubSub jedisPubSub;

    public RedisHandler(OmniChatReport plugin) {
        this.plugin = plugin;

        String user = plugin.getConfig().getString("redis.user");
        String password = plugin.getConfig().getString("redis.password");
        String host = plugin.getConfig().getString("redis.host");
        int port = plugin.getConfig().getInt("redis.port");

        JedisClientConfig config = DefaultJedisClientConfig.builder()
                .user(user)
                .password(password)
                .build();

        HostAndPort hostAndPort = new HostAndPort(host, port);

        this.redisClient = RedisClient.builder()
                .hostAndPort(hostAndPort)
                .clientConfig(config)
                .build();

        startSubscriber(host, port, password);

        plugin.sendConsole("&aSuccessfully connected to redis.");
    }

    public void startSubscriber(String host, int port, String password) {
        if (subscribed) return;

        subscribed = true;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            jedis = new Jedis(host, port);

            if (password != null && !password.isEmpty())
                jedis.auth(password);

            jedisPubSub = new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String[] parts = message.split(":");
                        // mute:name:expiry:reason:issuer:fromServer

                        String name = parts[1];

                        if (parts[0].equals("mute")) {
                            if (parts.length <= 4) {
                                plugin.sendConsole("&cCould not find sufficient values for redis.");
                                return;
                            }

                            String timeString = parts[2];
                            String reason = parts[3];
                            String issuer = parts[4];
                            String fromServer = parts[5];

                            if (!plugin.checkServer().equals(fromServer)) {
                                Player player = Bukkit.getServer().getPlayer(name);

                                if (player != null) {
                                    plugin.sendConsole("&aMute " + player.getName() + " request from " + fromServer + " by " + issuer);

                                    plugin.getMuteManager().mutePlayer(issuer, player.getName(), fromServer, timeString, reason, true, false)
                                            .thenAccept(success -> {
                                                // successfully muted from another server
                                                if (success) {
                                                    plugin.getMuteManager().getTimeLeft(player).thenAccept(time -> {
                                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                                            String formattedTime = TimeUtil.getTimeRemainingString(time);

                                                            plugin.sendConsole("&c" + issuer + " has muted "
                                                                    + player.getName() + " from " + fromServer + " for " + reason);

                                                            plugin.sendMessage(player, "&cYou have been muted for " + formattedTime + ".");
                                                            plugin.sendMessage(player, "&cReason: " + reason);
                                                        });
                                                    });
                                                } else
                                                    Bukkit.getScheduler().runTask(plugin, () -> plugin.sendConsole("&cCould not mute player."));
                                            });
                                }
                            }
                        } else if (parts[0].equals("unmute")) {
                            String fromServer = parts[2];

                            if (!plugin.checkServer().equals(fromServer)) {
                                Player player = Bukkit.getServer().getPlayer(name);

                                if (player != null) {
                                    plugin.sendConsole("&aUnmute " + player.getName() + " request from " + fromServer);

                                    plugin.getMuteManager().isMuted(name).thenAccept(isMuted -> {
                                        if (isMuted)
                                            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMuteManager().unmutePlayer(player.getName(), false)
                                                    .thenAccept(isUnmuted -> plugin.sendMessage(player, "&aYou are now unmuted.")));
                                        else
                                            Bukkit.getScheduler().runTask(plugin, () -> plugin.sendConsole("&c" + player.getName() + " is not muted."));
                                    });
                                }
                            }
                        } else {
                            plugin.sendConsole("&cUnknown Redis action: " + parts[0]);
                        }
                    });
                }
            };

            try {
                jedis.subscribe(jedisPubSub, CHANNEL);
            } catch (Exception e) {
                plugin.sendConsole("&cUnsubscribed from Redis.");
            }
        });

    }

    public boolean isMuted(String name) {
        return redisClient.exists("mute:" + name);
    }

    public void unmute(String name, String fromServer, boolean publish) {
        redisClient.del("mute:" + name);

        if (publish)
            publishUnmute(name, fromServer);
    }

    public void publishUnmute(String name, String fromServer) {
        redisClient.publish(CHANNEL, "unmute:" + name + ":" + fromServer);
    }

    public void mutePlayer(String issuer, String name, long millisTime, String reason, String server, boolean publish) {
        redisClient.set("mute:" + name, millisTime + ":" + reason + ":" + server);

        if (publish)
            publishMute(issuer, name, millisTime, reason, server);
    }

    public void publishMute(String issuer, String name, long millisTime, String reason, String server) {
        redisClient.publish(CHANNEL, "mute:" + name + ":" + millisTime + ":" + reason + ":" + issuer + ":" + server);
    }

    public void close() {
        if (jedisPubSub != null)
            jedisPubSub.unsubscribe();

        if (jedis != null)
            jedis.close();

        if (redisClient != null)
            redisClient.close();

        subscribed = false;
    }
}
