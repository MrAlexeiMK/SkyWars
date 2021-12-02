package argento.skywars.configs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import argento.skywars.Main;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public abstract class AbstractConfig {
    File file = null;
    FileConfiguration config = null;
    private String path;

    public AbstractConfig(String path) {
        init(path);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void init(String path) {
        this.path = path;
        file = new File(Main.instance.getDataFolder(), path);
        if(!file.exists()) {
            getConfig().options().copyDefaults(true);
            saveDefaultConfig();
        }
        reloadConfig();
    }

    public void saveDefaultConfig() {
        if(file == null) {
            file = new File(Main.instance.getDataFolder(), getPath());
        }
        if(!file.exists()) {
        	Main.instance.saveResource(getPath(), false);
        }
    }

    public void saveConfig() {
        if(config == null || file == null) return;
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void accept() {
        saveConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        saveDefaultConfig();
        config = YamlConfiguration.loadConfiguration(file);
        Reader defConfigStream = new InputStreamReader(Main.instance.getResource(getPath()));
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            config.setDefaults(defConfig);
        }
    }

    public FileConfiguration getConfig() {
        if(config == null) {
            reloadConfig();
        }
        return config;
    }
}
