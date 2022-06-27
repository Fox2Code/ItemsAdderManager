package com.fox2code.itemsaddermanager;

import com.fox2code.itemsaddermanager.applier.AnimationMergerResourceApplier;
import com.fox2code.itemsaddermanager.applier.ResourceApplier;
import com.fox2code.itemsaddermanager.applier.YamlMergerResourceApplier;
import com.fox2code.itemsaddermanager.iapack.ItemsAdderPack;
import com.fox2code.itemsaddermanager.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

public class ItemsAdderPackApplier {
    private static final String[] licensesFiles =
            new String[]{"LICENSE.md", "LICENSE", "COPYING"};
    private final List<ItemsAdderPack> orderedPacks;
    private final File pluginsRoot;
    private final File iaPackDataSource;
    private final File iaPackDataTarget;
    private final HashSet<String> extraAllowedPaths;
    private final HashMap<String, ResourceApplier> extraResourceAppliers;

    public ItemsAdderPackApplier(List<ItemsAdderPack> orderedPacks, File pluginsRoot,
                                 File iaPackDataSource, File iaPackDataTarget) {
        this.orderedPacks = orderedPacks;
        this.pluginsRoot = pluginsRoot;
        this.iaPackDataSource = iaPackDataSource;
        this.iaPackDataTarget = iaPackDataTarget;
        this.extraAllowedPaths = new HashSet<>();
        this.extraResourceAppliers = new HashMap<>();
        this.addResourceApplier("ItemsAdder/ia_gui.yml",
                YamlMergerResourceApplier.IA_GUI);
    }

    public void addAllowedFile(String path) {
        this.extraAllowedPaths.add(path);
    }

    public void addResourceApplier(String path, ResourceApplier resourceApplier) {
        this.extraResourceAppliers.put(path, resourceApplier);
    }

    public void applyPacks() throws IOException {
        IOUtils.deleteDataFolderContent(this.iaPackDataTarget);
        IOUtils.copyFolderContent(this.iaPackDataSource, this.iaPackDataTarget);
        HashSet<String> extraAllowedPaths = new HashSet<>(this.extraAllowedPaths);
        File licensesRoot = new File(this.iaPackDataTarget, "resource_pack" + File.separator + "licenses");
        for (ItemsAdderPack itemsAdderPack : this.orderedPacks) {
            boolean failedLicenseCopy = false;
            File licenseRootPack = new File(licensesRoot, itemsAdderPack.getName());
            for (String license : licensesFiles) {
                InputStream inputStream = itemsAdderPack.getResource(license);
                if (inputStream == null) continue;
                if (!licenseRootPack.isDirectory() && !licenseRootPack.mkdirs()) {
                    failedLicenseCopy = true;
                    break;
                }
                try (inputStream; FileOutputStream fileOutputStream =
                        new FileOutputStream(new File(licenseRootPack, license))) {
                    IOUtils.copyStream(inputStream, fileOutputStream);
                } catch (Exception e) {
                    ItemsAdderManager.getInstance().getLogger().log(Level.SEVERE,
                            "Failed to extract license " + license + " from " + itemsAdderPack.getName(), e);
                    failedLicenseCopy = true;
                    break;
                }
            }
            if (failedLicenseCopy) {
                ItemsAdderManager.getInstance().getLogger().log(Level.SEVERE,
                        "Skipping applying " + itemsAdderPack.getName() + " for legal reasons.");
                continue;
            }
            boolean hasSentTrace = false; // Only allow one trace per pack to avoid log spam
            for (String path : itemsAdderPack.getEntries()) {
                ResourceApplier resourceApplier = fromPath(extraAllowedPaths, path);
                if (resourceApplier != null) {
                    try {
                        File dest = new File(this.pluginsRoot, path);
                        resourceApplier.applyResource(itemsAdderPack, path, dest);
                        if (dest.exists()) {
                            if (ItemsAdderManager.verbose) {
                                ItemsAdderManager.getInstance().getLogger().info(
                                        "Applied: " + path + " from " + itemsAdderPack.getName() + " to server.");
                            }
                        } else {
                            ItemsAdderManager.getInstance().getLogger().severe(
                                    "Failed to apply " + path + " from " + itemsAdderPack.getName());
                        }
                    } catch (Exception e) {
                        if (hasSentTrace) {
                            ItemsAdderManager.getInstance().getLogger().log(Level.WARNING,
                                    "Failed to apply " + path + " from " + itemsAdderPack.getName());
                        } else {
                            hasSentTrace = true;
                            ItemsAdderManager.getInstance().getLogger().log(Level.WARNING,
                                    "Failed to apply " + path + " from " + itemsAdderPack.getName(), e);
                        }
                    }
                } else if (ItemsAdderManager.debug) {
                    ItemsAdderManager.getInstance().getLogger().info(
                            "No applier for: " + path + " in " + itemsAdderPack.getName());
                }
            }
        }
    }

    private ResourceApplier fromPath(HashSet<String> extraAllowedPaths, String path) {
        if (extraAllowedPaths.remove(path)) {
            return ResourceApplier.SPECIAL;
        } else if (path.startsWith("ItemsAdder/data/")) {
            if (path.startsWith("ItemsAdder/data/resource_pack/assets/minecraft/animations/") &&
                    (path.endsWith(".iaentitymodel") || path.endsWith(".player_animations")))
                return AnimationMergerResourceApplier.ANIMATION;
            return path.endsWith(".yml") ? YamlMergerResourceApplier.YAML : ResourceApplier.DEFAULT;
        } else if (path.startsWith("ItemsAdder/lang/") && path.endsWith(".yml")) {
            return YamlMergerResourceApplier.YAML;
        }
        return this.extraResourceAppliers.get(path);
    }
}
