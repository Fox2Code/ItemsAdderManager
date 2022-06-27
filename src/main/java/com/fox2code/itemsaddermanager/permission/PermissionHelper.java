package com.fox2code.itemsaddermanager.permission;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public class PermissionHelper {
    private static final PermissionHelper permissionHelper = makeHelper();

    PermissionHelper() {}

    private static PermissionHelper makeHelper() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        if (pluginManager.getPlugin("LuckPerms") != null) {
            return PermissionHelperLuckPerms.make();
        } else if (pluginManager.getPlugin("Vault") != null) {
            return PermissionHelperVault.make();
        } else {
            return new PermissionHelper();
        }
    }

    public boolean isSupported() {
        return false;
    }

    public void addDefaultPerm(String permission) {
        throw new UnsupportedOperationException("Can't addDefault(\"" + permission + "\")");
    }

    public static PermissionHelper getPermissionHelper() {
        return permissionHelper;
    }
}
