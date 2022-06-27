package com.fox2code.itemsaddermanager.iapack;

import com.fox2code.itemsaddermanager.ItemsAdderManager;
import com.fox2code.itemsaddermanager.applier.MissingDeclaredResourceException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PackZipResource implements Closeable {
    private static final HashSet<String> noRedirect = new HashSet<>(Arrays.asList(
            "plugin.yml", "config.yml", "bungee.yml",
            "iapack.yml", "pack.mcmeta", "pack.png",
            "LICENSE.md", "LICENSE", "COPYING"
    ));
    private static final HashSet<String> illegals = new HashSet<>(Arrays.asList("iapack.yml",
            "ItemsAdder/data/resource_pack/assets/minecraft/animations/.player_animations",
            "ItemsAdder/data/resource_pack/assets/minecraft/blockstates/brown_mushroom_block.json",
            "ItemsAdder/data/resource_pack/assets/minecraft/blockstates/chorus_plant.json",
            "ItemsAdder/data/resource_pack/assets/minecraft/blockstates/fire.json",
            "ItemsAdder/data/resource_pack/assets/minecraft/blockstates/mushroom_stem.json",
            "ItemsAdder/data/resource_pack/assets/minecraft/blockstates/red_mushroom_block.json",
            "ItemsAdder/data/resource_pack/assets/minecraft/font/default.json",
            "ItemsAdder/data/index.html", "ItemsAdder/data/pack.zip"
    ));
    private final File file;
    private final ZipFile zipFile;
    final StorageType storageType;
    final String tooMuchExplicit;
    final LinkedHashSet<String> entries;
    final HashMap<String, byte[]> injectedData;
    final boolean hasMetadata;
    final long length;
    boolean closed;

    public PackZipResource(File file) throws IOException {
        this(file, null);
    }

    PackZipResource(File file, StorageType storageType) throws IOException {
        boolean blockUnsafeRead = storageType == null;
        this.file = file;
        this.length = file.length();
        this.zipFile = new ZipFile(file);
        if (blockUnsafeRead && this.zipFile.getEntry("pack.mcmeta") != null && // Check for ItemsAdder packs
                this.zipFile.getEntry("assets/minecraft/animations/.player_animations") != null &&
                this.zipFile.getEntry("assets/minecraft/font/default.json") != null) {
            throw ItemsAdderResourcePackException.INSTANCE;
        }
        boolean doClose = true;
        try {
            this.hasMetadata = this.zipFile.getEntry("iapack.yml") != null;
            for (StorageType storageTypeCheck : StorageType.values()) {
                if (storageTypeCheck.check(this.zipFile)) {
                    storageType = storageTypeCheck;
                    break;
                }
            }
            for (StorageType storageTypeCheck : StorageType.values()) {
                if (storageTypeCheck == storageType) break;
                if (storageTypeCheck.deepCheck(this.zipFile)) {
                    storageType = storageTypeCheck;
                    break;
                }
            }
            if (storageType == null && this.hasMetadata) {
                storageType = StorageType.IMPLICIT;
            }
            String tooMuchExplicit = null;
            if (storageType == null) {
                Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();
                while (zipEntryEnumeration.hasMoreElements()) {
                    String entryName = zipEntryEnumeration.nextElement().getName();
                    int i = entryName.indexOf("/ItemsAdder/data/items_packs/");
                    if (i != -1) {
                        tooMuchExplicit = entryName.substring(0, i + 1);
                        storageType = StorageType.EXPLICIT;
                        break;
                    }
                    i = entryName.indexOf("/data/items_packs/");
                    if (i != -1) {
                        tooMuchExplicit = entryName.substring(0, i + 1);
                        storageType = StorageType.IMPLICIT;
                        break;
                    }
                }
                if (tooMuchExplicit == null)
                    throw new IOException("Zip file is not an ItemsAdder pack!");
            }
            this.storageType = storageType;
            this.tooMuchExplicit = tooMuchExplicit;
            this.entries = new LinkedHashSet<>();
            this.injectedData = new HashMap<>();
            Enumeration<? extends ZipEntry> zipEntryEnumeration = this.zipFile.entries();
            while (zipEntryEnumeration.hasMoreElements()) {
                ZipEntry zipEntry = zipEntryEnumeration.nextElement();
                String entryName = zipEntry.getName();
                if (blockUnsafeRead && entryName.endsWith(".jar"))
                    throw new IOException("Found unsafe \"" + entryName + "\" in " + this.getDefaultName());
                if (zipEntry.isDirectory() || noRedirect.contains(zipEntry.getName()) || entryName.contains("..") ||
                        entryName.contains("//") || entryName.endsWith(".class") || entryName.endsWith(".html"))
                    continue; // Skip directories, special entries, and dangerous paths
                String path;
                if (tooMuchExplicit != null) {
                    path = entryName.startsWith(tooMuchExplicit) ?
                            entryName.substring(tooMuchExplicit.length()) : null;
                    if (path == null) continue;
                    path = storageType.encodePath(path);
                } else {
                    path = storageType.encodePath(entryName);
                }
                if (path != null && !illegals.contains(path)) {
                    this.entries.add(path);
                }
            }
            if (ItemsAdderManager.isDebug()) {
                ItemsAdderManager.getInstance().getLogger().info(
                        "Loaded resource for " + this.file.getName() + " type " + this.storageType + " prefix " +
                                ns(this.tooMuchExplicit) + " implementation " + this.getClass().getSimpleName());
            }
            doClose = false;
        } finally {
            if (doClose) {
                this.closed = true;
                this.zipFile.close();
            }
        }
    }

    private static String ns(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    // Only used internally for ItemsAdder plugin
    final void addPrefixAsRoot(String prefix) {
        Enumeration<? extends ZipEntry> zipEntryEnumeration = this.zipFile.entries();
        while (zipEntryEnumeration.hasMoreElements()) {
            ZipEntry zipEntry = zipEntryEnumeration.nextElement();
            String entryName = zipEntry.getName();
            if (!entryName.startsWith(prefix)) continue;
            entryName = entryName.substring(prefix.length());
            if (zipEntry.isDirectory() || noRedirect.contains(zipEntry.getName()) || entryName.contains("..") ||
                    entryName.contains("//") || entryName.endsWith(".class") || entryName.endsWith(".html"))
                continue; // Skip directories, special entries, and dangerous paths
            String path = this.storageType.encodePath(entryName);
            if (path != null && !illegals.contains(path)) {
                this.entries.add(path);
            }
        }
    }

    public File getFile() {
        return this.file;
    }

    @NotNull
    public String getProviderName() {
        return this.file.getName();
    }

    @NotNull
    public String getDefaultName() {
        if (this.tooMuchExplicit != null) {
            int i = this.tooMuchExplicit.lastIndexOf('-');
            if (i != -1 && this.tooMuchExplicit.indexOf(' ', i) == -1) {
                return this.tooMuchExplicit.substring(0, i);
            }
            return this.tooMuchExplicit.substring(0,
                    this.tooMuchExplicit.length() - 1);
        }
        return this.file.getName();
    }

    @NotNull
    public String getDefaultVersion() {
        if (this.tooMuchExplicit != null) {
            int i = this.tooMuchExplicit.lastIndexOf('-');
            if (i != -1 && this.tooMuchExplicit.indexOf(' ', i) == -1) {
                return this.tooMuchExplicit.substring(i + 1,
                        this.tooMuchExplicit.length() - 1);
            }
        }
        return "";
    }

    public int getDefaultPriority() {
        return this.storageType == StorageType.RESOURCE_PACK ? 500 : 1000;
    }

    @Nullable
    public String getDefaultWebsite() {
        return null;
    }

    public Set<String> getEntries() {
        return Collections.unmodifiableSet(this.entries);
    }

    @Nullable
    public final InputStream getResource(@NotNull String path, boolean notMissing)
            throws MissingDeclaredResourceException {
        InputStream inputStream;
        try {
            path = noRedirect.contains(path) ? path :
                    this.storageType.decodePath(path);
            if (path == null) {
                return null;
            }
            if (this.tooMuchExplicit != null) {
                if (noRedirect.contains(path)) {
                    try {
                        inputStream =
                                this.getResourceImpl(path);
                        if (inputStream != null)
                            return inputStream;
                    } catch (IOException ignored) {}
                }
                path = this.tooMuchExplicit + path;
            }
            inputStream = this.getResourceImpl(path);
        } catch (IOException ioe) {
            return null;
        }
        if (inputStream == null && notMissing) {
            throw new MissingDeclaredResourceException(path);
        }
        return inputStream;
    }

    @Nullable
    InputStream getResourceImpl(@NotNull String path) throws IOException {
        if (this.closed) return null;
        return this.zipFile.getInputStream(new ZipEntry(path));
    }

    @Override
    public void close() throws IOException {
        if (this.closed) return;
        try {
            this.zipFile.close();
        } finally {
            this.closed = true;
        }
    }

    public boolean isClosed() {
        return this.closed;
    }

    public boolean isClosable() {
        return true;
    }

    public boolean shouldReopen() {
        return this.file.exists() && (this.closed ||
                this.length != this.file.length());
    }

    public PackZipResource reopen() throws IOException {
        return new PackZipResource(this.file);
    }

    enum StorageType {
        PLUGIN("ItemsAdder/") {
            @Override
            boolean check(ZipFile zipFile) {
                return zipFile.getEntry("plugin.yml") != null &&
                        zipFile.getEntry("data/items_packs/") != null ||
                        zipFile.getEntry("data/resource_pack/") != null;
            }

            @Override
            String encodePath(String entry) {
                if ("ia_gui.yml".equals(entry)) {
                    return "ItemsAdder/ia_gui.yml";
                }
                if (!entry.startsWith("data/"))
                    return null;
                return super.encodePath(entry);
            }

            @Override
            String decodePath(String entry) {
                if ("ItemsAdder/ia_gui.yml".equals(entry)) {
                    return "ia_gui.yml";
                }
                return super.decodePath(entry);
            }
        },
        EXPLICIT("", "ItemsAdder/data") {
            @Override
            boolean check(ZipFile zipFile) {
                return zipFile.getEntry("ItemsAdder/data/items_packs/") != null ||
                        zipFile.getEntry("ItemsAdder/data/resource_pack/") != null;
            }

            @Override
            String encodePath(String entry) {
                return entry;
            }

            @Override
            String decodePath(String entry) {
                return entry;
            }
        }, IMPLICIT("ItemsAdder/", "data") {
            @Override
            boolean check(ZipFile zipFile) {
                return zipFile.getEntry("data/items_packs/") != null ||
                        zipFile.getEntry("data/resource_pack/") != null;
            }
        }, CATEGORIZED("ItemsAdder/data/", "") {
            @Override
            boolean check(ZipFile zipFile) {
                return (zipFile.getEntry("items_packs/") != null ||
                        zipFile.getEntry("resource_pack/") != null) &&
                        zipFile.getEntry("plugin.yml") == null;
            }
        }, RESOURCE_PACK("ItemsAdder/data/resource_pack/") {
            @Override
            boolean check(ZipFile zipFile) {
                return zipFile.getEntry("pack.mcmeta") != null;
            }
        };

        final String prefix;
        final String deepCheck1;
        final String deepCheck2;

        StorageType() {
            this.prefix = "";
            this.deepCheck1 = "";
            this.deepCheck2 = "";
        }

        StorageType(String prefix) {
            this.prefix = prefix;
            this.deepCheck1 = "";
            this.deepCheck2 = "";
        }

        StorageType(String prefix, String deepCheck) {
            this.prefix = prefix;
            this.deepCheck1 = deepCheck + "/items_packs";
            this.deepCheck2 = deepCheck + "/resource_pack";
        }

        abstract boolean check(ZipFile zipFile) throws IOException;

        boolean deepCheck(ZipFile zipFile) {
            if (this.deepCheck1.isEmpty()) return false;
            Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();
            while (zipEntryEnumeration.hasMoreElements()) {
                String entryName = zipEntryEnumeration.nextElement().getName();
                if (entryName.startsWith(this.deepCheck1) || entryName.startsWith(this.deepCheck2))
                    return true;
            }
            return false;
        }

        String encodePath(String entry) {
            return this.prefix + entry;
        }

        String decodePath(String entry) { //
            if (!entry.startsWith(this.prefix))
                return null;
            return entry.substring(this.prefix.length());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.file.getName().hashCode(), this.length);
    }
}
