package com.fox2code.itemsaddermanager.iapack;

import com.fox2code.itemsaddermanager.ItemsAdderManager;
import com.fox2code.itemsaddermanager.applier.MissingDeclaredResourceException;
import com.fox2code.itemsaddermanager.utils.IOUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

public final class ItemsAdderPack implements Comparable<ItemsAdderPack> {
    private PackZipResource resources;
    private String name;
    private String version;
    private String website;
    private int priority = 1000;

    public ItemsAdderPack(File zipFile) throws IOException {
        this(new PackZipResource(zipFile));
    }

    public ItemsAdderPack(JavaPlugin javaPlugin) throws IOException {
        this(new PackPluginResource(javaPlugin));
    }

    public ItemsAdderPack(PackZipResource packZipResource) {
        this.resources = Objects.requireNonNull(packZipResource);
        this.reloadMetaData();
    }

    private void reloadMetaData() {
        // Set default
        this.name = this.resources.getDefaultName();
        this.version = this.resources.getDefaultVersion();
        this.priority = this.resources.getDefaultPriority();
        this.website = this.resources.getDefaultWebsite();
        if (this.resources.entries.contains(
                "ItemsAdder/data/items_packs/iasurvival/categories.yml")) {
            this.priority = 50; // Different default priority for DefaultPack
        }
        // Load iapack.yml if available
        InputStream inputStream = this.getResource("iapack.yml");
        if (inputStream != null) {
            try {
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                yamlConfiguration.load(new InputStreamReader(inputStream));
                this.name = yamlConfiguration.getString("name", this.name);
                this.version = yamlConfiguration.getString("version", this.version);
                this.priority = yamlConfiguration.getInt("priority", this.priority);
                this.website = yamlConfiguration.getString("website", this.website);
            } catch (Exception e) {
                ItemsAdderManager.getInstance().getLogger().log(Level.WARNING,
                        "Failed to load iapack.yml for " + this.resources.getFile().getName(), e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
        }
    }


    @NotNull
    public Set<String> getEntries() {
        return this.resources.getEntries();
    }

    @Nullable
    public InputStream getResource(String path) {
        try {
            return this.resources.getResource(path, false);
        } catch (MissingDeclaredResourceException e) {
            return null;
        }
    }

    @NotNull
    public InputStream getResourceNotMissing(String path)
            throws MissingDeclaredResourceException {
        InputStream inputStream = this.resources.getResource(path, true);
        if (inputStream == null) throw new MissingDeclaredResourceException(path);
        return inputStream;
    }

    public void extractEntryTo(String path, File destination) throws IOException {
        InputStream inputStream = this.getResource(path);
        if (inputStream == null) return;
        destination.getParentFile().mkdirs(); // Just in case.
        try (inputStream; FileOutputStream fileOutputStream =
                new FileOutputStream(destination)) {
            IOUtils.copyStream(inputStream, fileOutputStream);
        }
    }

    public void tryExtractEntryToNotMissing(String path, File destination) throws IOException {
        InputStream inputStream = this.getResourceNotMissing(path);
        destination.getParentFile().mkdirs(); // Just in case.
        try (inputStream; FileOutputStream fileOutputStream =
                new FileOutputStream(destination)) {
            IOUtils.copyStream(inputStream, fileOutputStream);
        }
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getVersion() {
        return version;
    }

    @NotNull
    public String getProviderName() {
        return this.resources.getProviderName();
    }

    @Nullable
    public String getWebsite() {
        return this.website;
    }

    public boolean reload() {
        if (this.resources.shouldReopen()) {
            try {
                this.resources = this.resources.reopen();
                this.reloadMetaData();
                return true;
            } catch (IOException e) {
                return false;
            }
        } else return !this.resources.isClosed();
    }

    @Override
    public int compareTo(@NotNull ItemsAdderPack o) {
        int i = Integer.compare(o.priority, this.priority);
        return i == 0 ? this.name.compareTo(o.name) : i;
    }
}
