package net.omni.chatreport;

import net.omni.chatreport.commands.MuteCommand;
import net.omni.chatreport.commands.ReportCommand;
import net.omni.chatreport.db.DatabaseHandler;
import net.omni.chatreport.listeners.GUIListener;
import net.omni.chatreport.managers.GUIManager;
import net.omni.chatreport.managers.MuteManager;
import net.omni.chatreport.util.OMCConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class OmniChatReport extends JavaPlugin {

    private String prefix = "OCR";

//    private OMCConfig reportConfig;

    private GUIManager guiManager;

    private DatabaseHandler databaseHandler;

    private MuteManager muteManager;

    /*
    TODO:
        /report <player>
            make GUI customizable
        store recent chat messages of the reported player database (should be at least 1 day)
        use AI-based / advanced filtering systems
            detect: Swearing, Advertising (IP Addresses, domains, links etc.) store matching and things on config.yml
        if inappropriate:
            apply punishment (e.g mute)
        sync multi-server - if player reported on a diff server, punishment should work
        use mySQL or Redis for chat storing
         - add fallback punishments.yml
        add command autocomplete (especially in /report <player> <reason> <-)
     */


    @Override
    public void onEnable() {
        saveDefaultConfig();

        prefix = getConfig().getString("prefix");

        guiManager = new GUIManager(this);

        // listeners
        new GUIListener(this).register();

        // commands
        new ReportCommand(this).register();
        new MuteCommand(this).register();

        databaseHandler = new DatabaseHandler(this);
        databaseHandler.loadDatabase();

        muteManager = new MuteManager(this);
        muteManager.load();

        sendConsole("&aSuccessfully enabled " + getName() + "v-" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        guiManager.flush();

        // TODO databaseSave

        sendConsole("&cSuccessfully disabled " + getName() + "v-" + getDescription().getVersion());
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

    public void sendConsole(String message) {
        sendMessage(Bukkit.getConsoleSender(), message);
    }

    public void sendMessage(CommandSender sender, String message, boolean addPrefix) {
        String toSend = addPrefix ? "%prefix% &r".replace("%prefix%", prefix) + message : message;

        sender.sendMessage(translate(toSend));
    }

    public void sendMessage(CommandSender sender, String message) {
        sendMessage(sender, message, true);
    }

    public String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

}
