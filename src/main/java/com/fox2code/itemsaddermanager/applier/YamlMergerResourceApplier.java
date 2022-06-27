package com.fox2code.itemsaddermanager.applier;

import com.fox2code.itemsaddermanager.iapack.ItemsAdderPack;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

public class YamlMergerResourceApplier extends ResourceApplier {
    public static final YamlMergerResourceApplier
            YAML = new YamlMergerResourceApplier();
    public static final YamlMergerResourceApplier
            IA_GUI = new YamlMergerResourceApplier("categories.all.hide_items");

    private final String[] entries;

    protected YamlMergerResourceApplier(String... entries) {
        super(false);
        this.entries = entries;
    }

    @Override
    public void applyResource(ItemsAdderPack itemsAdderPack, String path, File destination) throws IOException {
        if (!destination.exists()) {
            itemsAdderPack.tryExtractEntryToNotMissing(path, destination);
            return;
        }
        InputStream inputStream = itemsAdderPack.getResourceNotMissing(path);
        YamlConfiguration pack = new YamlConfiguration();
        loadYaml(pack, inputStream);
        YamlConfiguration dest = new YamlConfiguration();
        dest.options().copyDefaults(false);
        loadYaml(dest, destination);
        for (String entry : entries) {
            if (pack.contains(entry) && dest.contains(entry)) {
                ArrayList<String> listDest = new ArrayList<>(
                        Objects.requireNonNull(dest.getStringList(entry)));
                for (String element : pack.getStringList(entry)) {
                    if (!listDest.contains(element)) listDest.add(element);
                }
                dest.set(entry, listDest);
            }
        }
        dest.options().copyDefaults(true);
        dest.setDefaults(pack);
        dest.save(destination);
    }

    private static void loadYaml(YamlConfiguration configuration, File file) throws IOException {
        try {
            configuration.load(file);
        } catch (InvalidConfigurationException e) {
            throw new IOException(e);
        }
    }

    private static void loadYaml(YamlConfiguration configuration, InputStream inputStream) throws IOException {
        try (inputStream) {
            configuration.load(new InputStreamReader(
                    inputStream, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException e) {
            throw new IOException(e);
        }
    }
}
