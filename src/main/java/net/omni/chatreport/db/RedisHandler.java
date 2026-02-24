package net.omni.chatreport.db;

import net.omni.chatreport.OmniChatReport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.*;

public class RedisHandler {

    private static final String CHANNEL = "ocr_mutes";

    private final RedisClient redisClient;

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
            try (Jedis jedis = new Jedis(host, port)) {
                if (password != null && !password.isEmpty()) {
                    jedis.auth(password);
                }

                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            String[] parts = message.split(":");
                            // mute:name:expiry:reason:issuer:fromServer

                            String name = parts[1];
                            Player player = Bukkit.getPlayer(name);

                            if (parts[0].equals("mute")) {
                                if (parts.length <= 4) {
                                    plugin.sendConsole("&cCould not find sufficient values for redis.");
                                    return;
                                }

                                String timeString = parts[2];
                                String reason = parts[3];
                                String issuer = parts[4];
                                String fromServer = parts[5];

                                if (!plugin.checkServer().equals(fromServer))
                                    plugin.getMuteManager().mutePlayer(issuer, player, timeString, reason);
                            } else if (parts[0].equals("unmute")) {
                                String fromServer = parts[2];

                                if (!plugin.checkServer().equals(fromServer))
                                    plugin.getMuteManager().unmutePlayer(player);
                            } else {
                                plugin.sendConsole("&cUnknown Redis action: " + parts[0]);
                            }
                        });
                    }
                }, CHANNEL);
            }
        });

        plugin.sendConsole("&aSuccessfully connected to redis.");
    }

    public boolean isMuted(String name) {
        return redisClient.exists("mute:" + name);
    }

    public void unmute(String name, String fromServer) {
        redisClient.publish(CHANNEL, "unmute:" + name + ":" + fromServer);
        redisClient.del("mute:" + name);
    }

    public void mutePlayer(String issuer, String name, long millisTime, String reason, String server) {
        redisClient.set("mute:" + name, millisTime + ":" + reason + ":" + server);
        redisClient.publish(CHANNEL, "mute:" + name + ":" + millisTime + ":" + reason + ":" + issuer + ":" + server);
    }

    public void close() {
        redisClient.close();
    }
}
