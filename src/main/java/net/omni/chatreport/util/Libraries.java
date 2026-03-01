package net.omni.chatreport.util;

import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.omni.chatreport.OmniChatReport;

public class Libraries {

    private final OmniChatReport plugin;

    public Libraries(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    public void loadDependencies() {

        BukkitLibraryManager libraryManager = new BukkitLibraryManager(plugin);

        libraryManager.addMavenCentral();
        libraryManager.addSonatype();

        try {
            Library hikaricp = Library.builder()
                    .groupId("com.zaxxer")
                    .artifactId("HikariCP")
                    .version("6.3.0")
                    .build();

            Library mysql = Library.builder()
                    .groupId("com.mysql")
                    .artifactId("mysql-connector-j")
                    .version("9.3.0")
                    .build();

            Library commonsPool = Library.builder()
                    .groupId("org.apache.commons")
                    .artifactId("commons-pool2")
                    .version("2.12.0")
                    .build();

            Library redisAuth = Library.builder()
                    .groupId("redis.clients.authentication")
                    .artifactId("redis-authx-core")
                    .version("0.1.1-beta2")
                    .build();

            Library redis = Library.builder()
                    .groupId("redis.clients")
                    .artifactId("jedis")
                    .version("7.2.0")
                    .build();

            libraryManager.loadLibrary(hikaricp);
            libraryManager.loadLibrary(mysql);
            libraryManager.loadLibrary(commonsPool);
            libraryManager.loadLibrary(redisAuth);
            libraryManager.loadLibrary(redis);

            plugin.sendConsole("Dependencies loaded successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }
}