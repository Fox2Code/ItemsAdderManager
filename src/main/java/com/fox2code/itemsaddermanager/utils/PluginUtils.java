package com.fox2code.itemsaddermanager.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;

public class PluginUtils {
    private static Field javaPluginFile;

    static {
        try {
            javaPluginFile = JavaPlugin.class.getDeclaredField("file");
            javaPluginFile.setAccessible(true);
        } catch (Exception e) {
            javaPluginFile = null;
        }
    }

    public static File getFile(JavaPlugin javaPlugin) {
        if (javaPluginFile != null) {
            try {
                return (File) javaPluginFile.get(javaPlugin);
            } catch (Exception ignored) {}
        }
        URL location = javaPlugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        try {
            return new File(location.toURI().getPath());
        } catch (URISyntaxException ignored) {
            return new File(location.getPath().replace("%20", " "));
        }
    }
}
