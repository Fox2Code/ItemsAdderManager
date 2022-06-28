package com.fox2code.itemsaddermanager;

import com.fox2code.itemsaddermanager.compat.SlimeFunCompat;
import com.fox2code.itemsaddermanager.config.ItemAdderConfiguration;
import com.fox2code.itemsaddermanager.iapack.ItemsAdderPack;
import com.fox2code.itemsaddermanager.iapack.ItemsAdderResourcePackException;
import com.fox2code.itemsaddermanager.permission.PermissionHelper;
import com.fox2code.itemsaddermanager.utils.DummyCommandSender;
import com.fox2code.itemsaddermanager.utils.IOUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class ItemsAdderManager extends JavaPlugin implements Listener {
    static String version; static boolean debug, verbose;
    private static ItemsAdderManager instance;
    private final DummyCommandSender dummyCommandSender;
    private final File iaPacks = new File(this.getDataFolder()
            .getParentFile().getParentFile().getAbsoluteFile(), "iapacks");
    private final File iaPackConfigs = new File(this.iaPacks, "configs.yml");
    private final File iaPackData = new File(this.iaPacks, "data");
    private final File iaData = new File(this.getDataFolder().getAbsoluteFile()
            .getParentFile(), "ItemsAdder" + File.separator + "data");
    private final File iaConfig = new File(this.getDataFolder().getAbsoluteFile()
            .getParentFile(), "ItemsAdder" + File.separator + "config.yml");
    private final File iaDataMarker = new File(this.iaData, ".iam");
    private boolean allowModifications = (!this.iaData.exists()) || this.iaDataMarker.exists();
    private final HashMap<String, ItemsAdderPack> itemsAdderPacks = new HashMap<>();
    private final HashMap<String, ItemAdderConfiguration> itemsAdderConfigurations = new HashMap<>();
    private boolean lockBasedFilesystem;
    private CompletableFuture<Boolean> reloading;

    public static ItemsAdderManager getInstance() {
        return instance;
    }

    public static String getVersion() {
        return version;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public ItemsAdderManager() {
        this.dummyCommandSender = new DummyCommandSender("ItemsAdderManager");
        ItemsAdderManager.instance = this; // Set instance early
        version = this.getDescription().getVersion();
        this.itemsAdderConfigurations.put("thirst", new ItemAdderConfiguration(
                "items_packs/iasurvival/thirst/hud_thirst.yml",
                "huds.thirst_bar.enabled", true));
    }

    public boolean tryAllowModifications() {
        if (this.iaDataMarker.exists()) {
            try {
                Files.setAttribute(this.iaDataMarker.toPath(), "dos:hidden",
                        Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
            } catch (IOException ignored) {}
            this.allowModifications = true;
            return true;
        }
        boolean marker = false;
        try {
            marker = this.iaDataMarker.createNewFile();
            Files.setAttribute(this.iaDataMarker.toPath(), "dos:hidden",
                    Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException ignored) {}
        if (!marker) {
            this.allowModifications = false;
            return false;
        }
        this.allowModifications = true;
        return true;
    }

    @Override
    public void onLoad() {
        debug = this.getConfig().getBoolean("debug", false);
        verbose = this.getConfig().getBoolean("verbose", false);
        if (System.getProperties().containsKey("ItemsAdderLoaded")) {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                this.lockBasedFilesystem = true; // This is a very specific error.
                throw new IllegalStateException("Can't load ItemsAdderManager after ItemsAdder on Windows!");
            }
        } else if (this.getConfig().getBoolean("autoCleanup", true)) {
            // This is an ItemsAdder recommendation.
            File cache = new File(this.iaData.getParentFile(), "cache" + File.separator +
                    "various" + File.separator + "fixed_glitched_blocks.nbt");
            if (cache.exists() && cache.length() >= 8192) {
                cache.delete();
            }
        }
        if (!this.iaPacks.isDirectory() && !this.iaPacks.mkdirs()) {
            this.getLogger().severe("Failed to create \"iapacks\" folder.");
        } else {
            this.saveDefaultConfig();
        }
        File iaPacksItems = new File(this.iaPackData, "items_packs");
        File iaPacksResources = new File(this.iaPackData, "resource_pack");
        if (!iaPacksItems.isDirectory() && !iaPacksItems.mkdirs()) {
            this.getLogger().warning("Failed to create \"iapacks/data/items_packs\"");
        }
        if (!iaPacksResources.isDirectory() && !iaPacksResources.mkdirs()) {
            this.getLogger().warning("Failed to create \"iapacks/data/resource_pack\"");
        }
    }

    @Override
    public void onEnable() {
        debug = this.getConfig().getBoolean("debug", false);
        verbose = this.getConfig().getBoolean("verbose", false);
        boolean shouldForceRebuild = false;
        if (this.allowModifications) {
            if (!this.iaData.exists() && this.iaData.mkdirs()) {
                boolean marker = false;
                try {
                    marker = this.iaDataMarker.createNewFile();
                } catch (IOException ignored) {}
                if (!marker) {
                    this.getLogger().severe(
                            "ItemsAdder data folder is read only!");
                    this.allowModifications = false;
                    return;
                }
                shouldForceRebuild = true;
            }
        } else {
            String[] path = this.iaData.list();
            if (path == null || path.length == 0) {
                boolean marker = false;
                try {
                    marker = this.iaDataMarker.createNewFile();
                } catch (IOException ignored) {}
                if (!marker) {
                    this.getLogger().severe(
                            "ItemsAdder data folder is read only!");
                    return;
                }
                this.allowModifications = true;
                shouldForceRebuild = true;
            }
        }
        this.getServer().getPluginManager().registerEvents(this, this);
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            this.getLogger().warning("Windows is currently not supported due to filesystem restrictions.");
            this.lockBasedFilesystem = true; //
        } else this.lockBasedFilesystem = false;
        if (!PermissionHelper.getPermissionHelper().isSupported()) {
            this.getLogger().warning("No supported permission manager found!");
            this.getLogger().warning("Please install a supported permission manager like LuckPerms");
        }
        if (this.getConfig().getBoolean("autoReload", true)) {
            try {
                this.rebuildPacksImpl(shouldForceRebuild, true);
            } catch (IOException e) {
                this.getLogger().log(Level.SEVERE, "Failed to reload packs", e);
            }
        }
        this.reloadConfigurations();
        this.reloading = null;
    }

    @Override
    public void onDisable() {

    }

    @NotNull
    public CompletableFuture<Boolean> rebuildPacks(final boolean force) {
        if (this.reloading != null) {
            return this.reloading;
        }
        if (this.lockBasedFilesystem) {
            this.getLogger().severe("Can't reload after init on Windows.");
            this.getLogger().severe("Please restart your server instead.");
            return CompletableFuture.completedFuture(false);
        }
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        new Thread(() -> {
            try {
                this.reloading = completableFuture;
                this.rebuildPacksImpl(force, false);
                this.reloadConfigurations();
                completableFuture.complete(true);
            } catch (Exception e) {
                this.getLogger().log(Level.SEVERE, "Failed to reload packs", e);
                completableFuture.complete(false);
            } finally {
                this.reloading = null;
            }
        }, "ItemsAdderManager rebuild thread").start();
        return completableFuture;
    }

    public void reloadConfigurations() {
        if (!this.allowModifications) return;
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        if (this.iaPackConfigs.exists()) try {
            yamlConfiguration.load(this.iaPackConfigs);
        } catch (Exception e) {
            this.getLogger().warning("Failed to read iapacks/configs.yml");
        }
        for (Map.Entry<String, ItemAdderConfiguration> entry : this.itemsAdderConfigurations.entrySet()) {
            entry.getValue().markLoaded(this.iaData, yamlConfiguration, entry.getKey());
        }
        try {
            yamlConfiguration.save(this.iaPackConfigs);
        } catch (Exception e) {
            this.getLogger().warning("Failed to write iapacks/configs.yml");
        }
    }

    private void rebuildPacksImpl(boolean force, boolean load) throws IOException {
        if (!this.iaData.canWrite()) {
            this.getLogger().severe("ItemsAdder data folder is read only!");
            return;
        }
        if (!this.iaPacks.isDirectory() && !this.iaPacks.mkdirs()) {
            this.getLogger().severe("Failed to create \"iapacks\" folder.");
            return;
        }
        if (!this.allowModifications) {
            this.getLogger().severe("Allow modifications is disabled, execute " +
                    "\"/iamanager allowModifications\" to enable plugin");
            this.getLogger().severe("Also don't forget to backup you ItemsAdder data folder before doing so");
            return;
        }
        this.getLogger().info("Loading packs...");
        // Step-1 reload active ItemsAdder packs
        Iterator<Map.Entry<String, ItemsAdderPack>>
                pluginPacksIterator = this.itemsAdderPacks.entrySet().iterator();
        while (pluginPacksIterator.hasNext()) {
            Map.Entry<String, ItemsAdderPack> entry = pluginPacksIterator.next();
            ItemsAdderPack itemsAdderPack = entry.getValue();
            if (!itemsAdderPack.reload()) {
                pluginPacksIterator.remove();
            }
        }
        // Step-2 load plugin ItemsAdder packs
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (!(plugin instanceof JavaPlugin javaPlugin) || this.itemsAdderPacks.containsKey(plugin.getName())
                    || plugin.getName().endsWith(".zip") || plugin.getName().indexOf(' ') != -1)
                continue; // Invalid plugin name or pack already fully loaded, skipping.
            if (javaPlugin.getName().equals("ItemsAdder")) {
                try {
                    this.itemsAdderPacks.put("ItemsAdder", new ItemsAdderPack(javaPlugin));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to register ItemsAdder pack", e);
                }
            } else if (javaPlugin.getResource("iapack.yml") != null) {
                try {
                    this.itemsAdderPacks.put(javaPlugin.getName(), new ItemsAdderPack(javaPlugin));
                } catch (IOException e) {
                    this.getLogger().log(Level.WARNING, "Failed to load plugin iapack: " + javaPlugin.getName());
                }
            }
        }
        // Step-3 load zip-file ItemsAdder packs
        for (File file : Objects.requireNonNull(this.iaPacks.listFiles())) {
            if (file.isDirectory() || !file.getName().endsWith(".zip")) continue;
            try {
                this.itemsAdderPacks.put(file.getName(), new ItemsAdderPack(file));
            } catch (IOException e) {
                if (e == ItemsAdderResourcePackException.INSTANCE) {
                    this.getLogger().warning("Skipping ItemsAdder generated resource pack: " + file.getName());
                } else {
                    this.getLogger().log(Level.WARNING, "Failed to load zip iapack: " + file.getName(), e);
                }
            }
        }
        // Step-4 Order ItemsPacks and check if an update is needed
        ArrayList<ItemsAdderPack> itemsAdderPacks =
                new ArrayList<>(this.itemsAdderPacks.values());
        Collections.sort(itemsAdderPacks);
        final int newHashCode = itemsAdderPacks.hashCode();
        if (!force) {
            int oldHashCode = 0,  oldVersionHashCode = 0;
            if (this.iaDataMarker.length() == 8) {
                try (DataInputStream dataInputStream = new DataInputStream(
                        Files.newInputStream(this.iaDataMarker.toPath()))){
                    oldHashCode = dataInputStream.readInt();
                    oldVersionHashCode = dataInputStream.readInt();
                } catch (IOException ignored) {}
            }
            if (oldHashCode == newHashCode && oldVersionHashCode == version.hashCode()) {
                this.getLogger().info("Skipping applying packs! (Up to date)");
                return;
            }
        }
        try (DataOutputStream dataOutputStream = new DataOutputStream(
                Files.newOutputStream(this.iaDataMarker.toPath()))) {
            dataOutputStream.writeInt(newHashCode);
            dataOutputStream.writeInt(version.hashCode());
        }
        // Step-5 apply ItemsAdder packs
        this.getLogger().info("Applying packs...");
        PluginManager pluginManager = this.getServer().getPluginManager();
        ItemsAdderPackApplier itemsAdderPackApplier = new ItemsAdderPackApplier(itemsAdderPacks,
                this.getDataFolder().getParentFile(), this.iaPackData, this.iaData);
        if (pluginManager.getPlugin("Slimefun") != null) {
            SlimeFunCompat.applyCompat(this, this.getDataFolder().getParentFile());
            itemsAdderPackApplier.addAllowedFile("Slimefun/item-models.yml");
        }
        List<String> specialPaths = this.getConfig().getStringList("specialPaths");
        for (String specialPath : specialPaths) {
            itemsAdderPackApplier.addAllowedFile(specialPath);
        }
        itemsAdderPackApplier.applyPacks();
        // Step-6 reload ItemsAdder
        if (load) return; // Skip if ItemsAdder didn't loaded yet.
        this.getServer().getScheduler().runTask(this, () ->
                this.dummyCommandSender.dispatchCommand("iazip all"));
    }

    private static final String helpSeparator = ChatColor.WHITE + " -> " + ChatColor.DARK_AQUA;
    public void sendHelper(CommandSender sender, ChatColor chatColor) {
        sender.sendMessage(chatColor + "/iamanager help " + helpSeparator + "Show this page!");
        sender.sendMessage(chatColor + "/iamanager help " + helpSeparator + "Show this page!");
        if (sender.hasPermission("ia.admin.manager.unsafe")) {
            sender.sendMessage(chatColor + "/iamanager allowModifications " + helpSeparator +
                    "Allow ItemsAdderManager to write inside item adder tada file!");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("ia.admin.manager")) return false; // Should not happen
        if (!this.allowModifications && !sender.hasPermission("ia.admin.manager.unsafe")) {
            sender.sendMessage(ChatColor.DARK_RED + "You do not have the permission to use ItemsAdderManager while modifications are disabled");
            sender.sendMessage(ChatColor.DARK_RED + "Please, ask an admin to type \"/iamanager allowModifications\" if you think this is a mistake.");
            return true;
        }
        if (command.getName().equals("iareloadpacks")) {
            this.rebuildPacks(true).thenAccept(result -> {
                if (result) {
                    sender.sendMessage(ChatColor.DARK_AQUA + "Packs reloaded!");
                } else {
                    sender.sendMessage(ChatColor.DARK_RED +
                            "Failed to reload packs, read console log for more details.");
                }
            });
            return true;
        } else if (command.getName().equals("iasetup")) {
            this.setupBasicItemAdder(sender, "/iasetup force", args.length == 0 ? "" : args[0]);
            return true;
        }
        if (args.length == 0) {
            sendHelper(sender, ChatColor.DARK_RED);
            return true;
        }
        switch (args[0]) {
            case "allowModifications":
                if (!sender.hasPermission("ia.admin.manager.unsafe")) {
                    sendHelper(sender, ChatColor.DARK_RED);
                } else if (this.tryAllowModifications()) {
                    sender.sendMessage(ChatColor.DARK_AQUA + "Successfully allowed modifications!");
                } else {
                    sender.sendMessage(ChatColor.DARK_RED +
                            "Failed to allow modifications, check console for more details!");
                }
                break;
            case "reload":
                this.rebuildPacks(true).thenAccept(result -> {
                    if (result) {
                        sender.sendMessage(ChatColor.DARK_AQUA + "Packs reloaded!");
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED +
                                "Failed to reload packs, read console log for more details.");
                    }
                });
                break;
            case "setup":
                this.setupBasicItemAdder(sender, "/iamanager setup force", args.length == 1 ? "" : args[1]);
                break;
            case "list":
                this.sendPacks(sender);
                break;
            case "help":
                sendHelper(sender, ChatColor.GOLD);
                break;
            case "delete":
                if (!(sender instanceof ConsoleCommandSender &&
                        sender.hasPermission("ia.admin.manager.unsafe"))) {
                    sendHelper(sender, ChatColor.DARK_RED);
                    break;
                }
                try {
                    IOUtils.deleteDataFolderContent(this.iaData);
                    sender.sendMessage(ChatColor.GREEN + "Data cleared successfully!");
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.DARK_RED + "Failed to delete data!");
                }
                break;
            default:
                sendHelper(sender, ChatColor.DARK_RED);
                break;
        }
        return true;
    }

    private void sendPacks(CommandSender commandSender) {
        if (this.itemsAdderPacks.isEmpty()) {
            commandSender.sendMessage(ChatColor.RED + "No pack loaded!");
            return;
        }
        if (this.itemsAdderPacks.size() == 1) {
            commandSender.sendMessage(ChatColor.GREEN +
                    "1" + ChatColor.WHITE +  " pack loaded!");
        } else {
            commandSender.sendMessage(ChatColor.GREEN + "" +
                    this.itemsAdderPacks.size() + ChatColor.WHITE + " packs loaded!");
        }
        for (ItemsAdderPack itemsAdderPack : this.itemsAdderPacks.values()) {
            TextComponent textComponent = new TextComponent(
                    itemsAdderPack.getName() + " " + itemsAdderPack.getVersion());
            textComponent.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);
            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(ChatColor.WHITE + "Provided by: " +
                            ChatColor.GREEN + itemsAdderPack.getProviderName())));
            String website = itemsAdderPack.getWebsite();
            if (website != null && !website.isEmpty()) {
                textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, website));
            }
            commandSender.spigot().sendMessage(textComponent);
        }
    }

    private void setupBasicItemAdder(CommandSender sender, String forceCmd, String arg) {
        if ("permissions".equals(arg)) {
            if (!PermissionHelper.getPermissionHelper().isSupported()) {
                sender.sendMessage(ChatColor.DARK_RED + "No supported permission manager found.");
                return;
            }
            ItemsAdderSetup.setupPermissions(sender);
            sender.sendMessage(ChatColor.DARK_AQUA + "Done!");
            return;
        }
        String[] packs = this.iaPacks.list();
        boolean requireForce = !this.allowModifications;
        if (packs != null) {
            for (String pack : packs) {
                if (pack.endsWith(".zip")) {
                    requireForce = true;
                    break;
                }
            }
        }
        if (requireForce) {
            boolean allowUnsafe = sender.hasPermission("ia.admin.manager.unsafe");
            if (!(arg.equals("force")) || !allowUnsafe) {
                if (allowUnsafe) {
                    sender.sendMessage(ChatColor.DARK_RED + "This operation may be unsafe and break your server.");
                    sender.sendMessage(ChatColor.DARK_RED + "Run " + forceCmd + " if you know what you are doing!");
                } else {
                    sender.sendMessage(ChatColor.DARK_RED + "You are not allowed to do unsafe operations.");
                }
                return;
            }
        }
        if (!this.allowModifications && !this.tryAllowModifications()) {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to allow modifications...");
            return;
        }
        if (!PermissionHelper.getPermissionHelper().isSupported()) {
            sender.sendMessage(ChatColor.DARK_RED + "No supported permission manager found.");
            return;
        }

        if (this.iaConfig.exists()) {
            try { // Make sure at least one hosting method is enabled
                YamlConfiguration config = new YamlConfiguration();
                config.load(this.iaConfig);
                if (!(config.getBoolean("resource-pack.hosting.no-host.enabled", false) ||
                        config.getBoolean("resource-pack.hosting.auto-external-host.enabled", false) ||
                        config.getBoolean("resource-pack.hosting.external-host.enabled", false) ||
                        config.getBoolean("resource-pack.hosting.self-host.enabled", false))) {
                    config.set("resource-pack.hosting.self-host.enabled", true);
                    config.save(this.iaConfig);
                } else if (config.getBoolean("resource-pack.hosting.no-host.enabled", false)) {
                    this.getLogger().severe(ChatColor.DARK_RED + "The option " + ChatColor.GOLD + "no-host" +
                            ChatColor.DARK_RED + " mean the resource-pack will not be received by players.");
                    this.getLogger().severe(ChatColor.DARK_RED + "You can't complain about missing textures " +
                            "until you have setup a valid hosting mode like " + ChatColor.GOLD + "self-host");
                }
            } catch (Exception ignored) {}
        }
        ItemsAdderSetup.setup(sender, this.iaPacks, this::rebuildPacks);
    }

    private static final List<String> baseCommands = Arrays.asList("allowModifications", "reload", "setup", "list");
    private static final List<String> setupComplete = Arrays.asList("force", "permissions");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("iareload") && args.length == 1 &&
                sender.hasPermission("ia.admin.manager.unsafe")) {
            return complete(args[0], setupComplete, false);
        }
        if (!command.getName().equalsIgnoreCase("iamanager"))
            return Collections.emptyList();
        if (args.length == 1) {
            return complete(args[0], baseCommands,
                    !sender.hasPermission("ia.admin.manager.unsafe"));
        }
        if (args.length == 2 && "setup".equals(args[0]) &&
                sender.hasPermission("ia.admin.manager.unsafe")) {
            return complete(args[1], setupComplete, false);
        }
        return Collections.emptyList();
    }

    private List<String> complete(@Nullable String arg,@NotNull List<String> completions, boolean skipFirst) {
        if (arg == null || arg.isEmpty())
            return completions;
        ArrayList<String> arrayList = new ArrayList<>();
        for (String completion : completions) {
            if (skipFirst) {
                skipFirst = false;
                continue;
            }
            if (completion.startsWith(arg)) {
                arrayList.add(completion);
            }
        }
        return arrayList;
    }
}
