package com.fox2code.itemsaddermanager.utils;

import org.bukkit.Bukkit;

public class VersionUtils {
    private static final int version;

    static {
        String minecraftVersion = Bukkit.getServer().getBukkitVersion();
        int i = minecraftVersion.indexOf('.');
        int i2 = minecraftVersion.indexOf('.', i + 1);
        if (i2 == -1) i2 = minecraftVersion.indexOf("-R");
        version = Integer.parseInt(
                minecraftVersion.substring(i, i2));
    }

    public static boolean isServerVersionInRange(int min, int max) {
        return (min == -1 || version >= min) && (max == -1 || version <= max);
    }
}
