package com.fox2code.itemsaddermanager.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DummyCommandSender extends PermissibleBase implements RemoteConsoleCommandSender {
    private final String name;

    public DummyCommandSender(String name) {
        super(new ServerOperator() {
            @Override
            public boolean isOp() {
                return true;
            }

            @Override
            public void setOp(boolean value) {}
        });
        this.name = name;
    }

    Spigot spigot = new Spigot() {
        @Override
        public void sendMessage(@NotNull BaseComponent... components) {
            for (BaseComponent baseComponent : components) {
                this.sendMessage(baseComponent);
            }
        }

        @Override
        public void sendMessage(UUID sender, @NotNull BaseComponent... components) {
            for (BaseComponent baseComponent : components) {
                this.sendMessage(sender, baseComponent);
            }
        }

        @Override
        public void sendMessage(@NotNull BaseComponent component) {
            DummyCommandSender.this.sendMessage(component.toLegacyText());
        }

        @Override
        public void sendMessage(UUID sender, @NotNull BaseComponent component) {
            this.sendMessage(component);
        }
    };

    @Override
    public void sendMessage(@NotNull String message) {
        // System.out.println(message);
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        for (String message : messages) {
            this.sendMessage(message);
        }
    }

    @Override
    public void sendMessage(UUID sender, @NotNull String message) {
        this.sendMessage(message);
    }

    @Override
    public void sendMessage(UUID sender, @NotNull String... messages) {
        for (String message : messages) {
            this.sendMessage(sender, message);
        }
    }

    @NotNull
    @Override
    public Server getServer() {
        return Bukkit.getServer();
    }

    @NotNull
    @Override
    public String getName() {
        return this.name;
    }

    @NotNull
    @Override
    public Spigot spigot() {
        return this.spigot;
    }

    public final void dispatchCommand(String commandLine) {
        this.getServer().dispatchCommand(this, commandLine);
    }
}
