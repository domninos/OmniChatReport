package net.omni.chatreport.db;

import net.omni.chatreport.OmniChatReport;
import org.bukkit.Bukkit;
import redis.clients.jedis.*;

public class RedisHandler {

    private static final String CHANNEL = "ocr_mutes";

    private final RedisClient redisClient;
    private Jedis jedis;
    private JedisPubSub jedisPubSub;

    public RedisHandler(OmniChatReport plugin) {
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
                                plugin.getMuteManager().mutePlayer(issuer, name, fromServer, timeString, reason, true)
                                        .thenAccept(success -> {
                                            // successfully muted from another server
                                            if (success)
                                                plugin.sendConsole("&c" + issuer + " has muted " + name + " on " + fromServer + " for " + reason);
                                        });
                            }
                        } else if (parts[0].equals("unmute")) {
                            String fromServer = parts[2];

                            if (!plugin.checkServer().equals(fromServer))
                                plugin.getMuteManager().unmutePlayer(name);
                        } else {
                            plugin.sendConsole("&cUnknown Redis action: " + parts[0]);
                        }
                    });
                }
            };

            try {
                jedis.subscribe(jedisPubSub, CHANNEL);
            } catch (Exception e) {
                plugin.sendConsole("&cCouldn't subscribe to Jedis.");
            }
        });

        plugin.sendConsole("&aSuccessfully connected to redis.");
    }

    public boolean isMuted(String name) {
        return redisClient.exists("mute:" + name);
    }

    public void unmute(String name, String fromServer) {
        redisClient.del("mute:" + name);

        publishUnmute(name, fromServer);
    }

    public void publishUnmute(String name, String fromServer) {
        redisClient.publish(CHANNEL, "unmute:" + name + ":" + fromServer);
    }

    public void mutePlayer(String issuer, String name, long millisTime, String reason, String server) {
        redisClient.set("mute:" + name, millisTime + ":" + reason + ":" + server);

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
    }
}
