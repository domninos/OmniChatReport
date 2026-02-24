package net.omni.chatreport;

import net.omni.chatreport.commands.MuteCommand;
import net.omni.chatreport.commands.ReportCommand;
import net.omni.chatreport.commands.UnmuteCommand;
import net.omni.chatreport.db.DatabaseHandler;
import net.omni.chatreport.db.RedisHandler;
import net.omni.chatreport.listeners.GUIListener;
import net.omni.chatreport.listeners.PlayerListener;
import net.omni.chatreport.managers.GUIManager;
import net.omni.chatreport.managers.MuteManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class OmniChatReport extends JavaPlugin {

    private String prefix = "OCR";
    private String server = "";

    private GUIManager guiManager;

    private DatabaseHandler databaseHandler;

    private MuteManager muteManager;

    private RedisHandler redisHandler;

    /*
    TODO:
        store recent chat messages of the reported player database (should be at least 1 day) - use redisClient.setex (may expiration to)
        use AI-based / advanced filtering systems
            detect: Swearing, Advertising (IP Addresses, domains, links etc.) store matching and things on config.yml
        if inappropriate:
            apply punishment (e.g mute)
        sync multi-server - if player reported on a diff server, punishment should work
        use mySQL or Redis for chat storing
         - add fallback punishments.yml
        add command autocomplete (especially in /report <player> <reason> <-)
        setup bungee for multi-server
        check for all async threads. make threads just for redis and database.

     */

    @Override
    public void onDisable() {
        guiManager.flush();
        muteManager.flush();

        if (useRedis())
            redisHandler.close();

        // TODO databaseSave
        databaseHandler.closeDB();

        sendConsole("&cSuccessfully disabled " + getName() + "v-" + getDescription().getVersion());
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        updateConfig();

        guiManager = new GUIManager(this);

        // listeners
        new GUIListener(this).register();
        new PlayerListener(this).register();

        // commands
        new ReportCommand(this).register();
        new MuteCommand(this).register();
        new UnmuteCommand(this).register();

        databaseHandler = new DatabaseHandler(this);
        databaseHandler.loadDatabase();

        if (useRedis())
            this.redisHandler = new RedisHandler(this);

        muteManager = new MuteManager(this);

        sendConsole("&aSuccessfully enabled " + getName() + " v-" + getDescription().getVersion());
    }

    public void updateConfig() {
        prefix = getConfig().getString("prefix");
        server = getConfig().getString("server");
    }

    public boolean useRedis() {
        return getConfig().getBoolean("redis.enable");
    }

    public void sendConsole(String message) {
        sendMessage(Bukkit.getConsoleSender(), message);
    }

    public void sendMessage(CommandSender sender, String message) {
        sendMessage(sender, message, true);
    }

    public void sendMessage(CommandSender sender, String message, boolean addPrefix) {
        String toSend = addPrefix ? "%prefix% &r".replace("%prefix%", prefix) + message : message;

        sender.sendMessage(translate(toSend));
    }

    public String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public RedisHandler getRedisHandler() {
        return redisHandler;
    }

    public MuteManager getMuteManager() {
        return muteManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }

}
