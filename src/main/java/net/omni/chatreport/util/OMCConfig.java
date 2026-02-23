package net.omni.chatreport.util;

import net.omni.chatreport.OmniChatReport;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class OMCConfig {
    private final OmniChatReport plugin;
    private final File zippedFile;
    private final FileConfiguration config;

    private final String fileName;

    public OMCConfig(OmniChatReport plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;

        if (plugin.getDataFolder().mkdir())
            plugin.sendConsole("&aSuccessfully created .../OmniZiptie");

        if (!fileName.endsWith(".yml"))
            fileName = fileName + ".yml";

        this.zippedFile = new File(plugin.getDataFolder(), fileName);

        if (!(zippedFile.exists())) {
            try {
                plugin.saveResource(fileName, false);
                plugin.sendConsole("&aSuccessfully created " + fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.config = YamlConfiguration.loadConfiguration(zippedFile);
    }

    public void set(String path, Object object) {
        setNoSave(path, object);
        save();
    }

    public void setNoSave(String path, Object object) {
        getConfig().set(path, object);
    }


    public void save() {
        try {
            config.save(zippedFile);
        } catch (IOException e) {
            plugin.sendConsole("&cCouldn't save " + fileName);
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public int getInt(String path) {
        return getConfig().getInt(path);
    }

    public String getString(String path) {
        return getConfig().getString(path);
    }

    public List<String> getStringList(String path) {
        return getConfig().getStringList(path);
    }
}