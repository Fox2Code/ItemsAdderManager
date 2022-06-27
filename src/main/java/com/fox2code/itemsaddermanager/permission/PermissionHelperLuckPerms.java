package com.fox2code.itemsaddermanager.permission;

import com.fox2code.itemsaddermanager.utils.DummyCommandSender;

final class PermissionHelperLuckPerms extends PermissionHelper {
    private final DummyCommandSender dummyCommandSender;

    private PermissionHelperLuckPerms() {
        this.dummyCommandSender = new DummyCommandSender("ItemsAdderManager");
    }

    static PermissionHelper make() {
        return new PermissionHelperLuckPerms();
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void addDefaultPerm(String permission) {
        if (permission.indexOf(' ') != -1) // Check argument
            throw new IllegalArgumentException("Invalid permission name: \"" + permission + "\"");
        // LuckPerms API is so confusing it is better to just run a command
        this.dummyCommandSender.dispatchCommand(
                "lp group default permission set " + permission + " true");
    }
}
