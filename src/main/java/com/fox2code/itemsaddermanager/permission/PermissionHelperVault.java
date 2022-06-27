package com.fox2code.itemsaddermanager.permission;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Arrays;
import java.util.Collection;

final class PermissionHelperVault extends PermissionHelper {
    private final Permission permission;

    private PermissionHelperVault() {
        Collection<RegisteredServiceProvider<Permission>> permissions =
                Bukkit.getServer().getServicesManager().getRegistrations(Permission.class);
        Permission usePermission = null;
        for (RegisteredServiceProvider<Permission> registeredServiceProvider : permissions) {
            Permission permission = registeredServiceProvider.getProvider();
            if (permission.hasGroupSupport() &&
                    permission.hasSuperPermsCompat()) {
                usePermission = permission;
            }
        }
        this.permission = usePermission;
    }

    static PermissionHelper make() {
        return new PermissionHelperVault();
    }

    @Override
    public boolean isSupported() {
        return this.permission != null && Arrays.asList(
                this.permission.getGroups()).contains("default");
    }

    @Override
    public void addDefaultPerm(String permission) {
        this.permission.groupAdd((String) null, "default", permission);
    }
}