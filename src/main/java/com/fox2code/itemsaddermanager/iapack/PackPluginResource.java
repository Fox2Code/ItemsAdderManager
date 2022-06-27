package com.fox2code.itemsaddermanager.iapack;

import com.fox2code.itemsaddermanager.utils.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

final class PackPluginResource extends PackZipResource {
    private static final String IA_NEW_CONFIG_PATH = "default_configs/new/";
    private final JavaPlugin javaPlugin;
    private final boolean itemsAdder;

    public PackPluginResource(JavaPlugin javaPlugin) throws IOException {
        super(PluginUtils.getFile(javaPlugin), StorageType.PLUGIN);
        this.javaPlugin = javaPlugin;
        this.itemsAdder = this.javaPlugin
                .getName().equals("ItemsAdder");
        if (this.itemsAdder) {
            this.addPrefixAsRoot(IA_NEW_CONFIG_PATH);
        }
        super.close();
    }

    @NotNull
    @Override
    public String getProviderName() {
        return this.javaPlugin.getName() + " " + this.javaPlugin.getDescription().getVersion();
    }

    @NotNull
    @Override
    public String getDefaultName() {
        return this.javaPlugin.getName();
    }

    @NotNull
    @Override
    public String getDefaultVersion() {
        return this.javaPlugin.getDescription().getVersion();
    }

    @Override
    public int getDefaultPriority() {
        return this.itemsAdder ? 0 : 1000;
    }

    @Nullable
    @Override
    public String getDefaultWebsite() {
        String website = this.javaPlugin.getDescription().getWebsite();
        if (this.itemsAdder && (website == null || !website.startsWith("https://"))) {
            website = "https://itemsadder.devs.beer/";
        }
        return website;
    }

    @Override
    InputStream getResourceImpl(@NotNull String path) {
        if (this.itemsAdder) {
            InputStream inputStream = this.javaPlugin
                    .getResource(IA_NEW_CONFIG_PATH + path);
            if (inputStream != null) return inputStream;
        }
        return this.javaPlugin.getResource(path);
    }

    @Override
    public void close() {}

    @Override
    public boolean isClosable() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return this.javaPlugin.getResource("plugin.yml") == null;
    }

    @Override
    public boolean shouldReopen() {
        Plugin newPlugin = Bukkit.getPluginManager().getPlugin(this.javaPlugin.getName());
        return newPlugin != this.javaPlugin && newPlugin instanceof JavaPlugin;
    }

    @Override
    public PackZipResource reopen() throws IOException {
        String pluginName = this.javaPlugin.getName();
        Plugin newPlugin = Bukkit.getPluginManager().getPlugin(this.javaPlugin.getName());
        if (newPlugin == null) throw new FileNotFoundException(
                "Plugin \"" + pluginName + "\" is not loaded");
        if (!(newPlugin instanceof JavaPlugin)) throw new IOException(
                "Plugin \"" + pluginName + "\" is not a java plugin");
        return new PackPluginResource((JavaPlugin) newPlugin);
    }
}
