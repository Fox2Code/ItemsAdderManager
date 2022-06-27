package com.fox2code.itemsaddermanager.config;

import com.fox2code.itemsaddermanager.ItemsAdderManager;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;

public class ItemAdderConfiguration {
    private final String path;
    private final String key;
    private final boolean defaultValue;
    private boolean value;
    private File destination;

    public ItemAdderConfiguration(String path, String key, boolean defaultValue) {
        this.path = path;
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public void markLoaded(File root, Configuration configuration, String key) {
        if (this.destination == null) {
            this.destination = new File(root, this.path);
            if (ItemsAdderManager.isVerbose()) {
                ItemsAdderManager.getInstance().getLogger().info("Config " + key +
                        " path \"" + this.path + "\" loaded!");
            }
        }
        this.value = configuration.getBoolean(key, this.defaultValue);
        this.apply();
        if (this.destination.exists() && !configuration.isSet(key))
            configuration.set(key, this.defaultValue);
    }

    public void apply() {
        if (this.destination == null || !this.destination.exists()) return;
        try {
            YamlConfiguration yamlConfiguration = new YamlConfiguration();
            yamlConfiguration.load(this.destination);
            if (yamlConfiguration.getBoolean(this.key, !this.value) == this.value)
                return; // already set to correct value, don't do more work for nothing
            yamlConfiguration.set(this.key, this.value);
            yamlConfiguration.save(this.destination);
        } catch (Exception e) {
            ItemsAdderManager.getInstance().getLogger().log(Level.WARNING,
                    "Failed to set " + this.key + " to " + this.value + " in " + this.path);
        }
    }
}
