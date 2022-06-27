package com.fox2code.itemsaddermanager;

import com.fox2code.itemsaddermanager.permission.PermissionHelper;
import com.fox2code.itemsaddermanager.utils.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ItemsAdderSetup {
    static void setup(CommandSender sender, File packsRoot,
                      Function<Boolean, CompletableFuture<Boolean>> rebuildPacks) {
        if (!packsRoot.isDirectory() && !packsRoot.mkdirs()) {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to create \"iapacks\" folder.");
            return;
        }
        setupPermissions(sender);
        new Thread(() -> {
            sender.sendMessage(ChatColor.DARK_AQUA + "Downloading default pack...");
            try {
                downloadGithubPack("ItemsAdder/DefaultPack", packsRoot);
            } catch (IOException e) {
                sender.sendMessage(ChatColor.DARK_RED + "Failed to download default pack!");
                return;
            }
            sender.sendMessage(ChatColor.DARK_AQUA + "Downloading other packs...");
            try {
                downloadGithubPack("ItemsAdder/OtherPacks", packsRoot);
            } catch (IOException e) {
                sender.sendMessage(ChatColor.DARK_RED + "Failed to download other packs!");
                return;
            }
            sender.sendMessage(ChatColor.DARK_AQUA + "Reloading packs...");
            rebuildPacks.apply(false).thenAccept(result -> {
                if (result) {
                    sender.sendMessage(ChatColor.DARK_AQUA + "Packs reloaded!");
                    sender.sendMessage(ChatColor.DARK_AQUA  + "Note: " +
                            "You may want to wipe your world to apply new generation!");
                } else {
                    sender.sendMessage(ChatColor.DARK_RED +
                            "Failed to reload packs, read console log for more details.");
                }
            });
        }, "ItemsAdderManager setup thread").start();
    }

    public static void downloadGithubPack(String repo, File packsRoot) throws IOException {
        int i = repo.indexOf('/');
        if (i == -1 || i != repo.lastIndexOf('/'))
            throw new IllegalArgumentException("Invalid GitHub repo: " + repo);
        String reposUrl = "https://api.github.com/repos/" + repo + "/releases?per_page=5";
        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(new String(IOUtils.readAllBytes(
                IOUtils.openUrl(reposUrl)), StandardCharsets.UTF_8), JsonArray.class);
        String downloadUrl = null;
        String downloadName = repo.substring(i + 1) + ".zip";
        root:
        for (JsonElement jsonElement : jsonArray) {
            if (jsonElement instanceof JsonObject jsonObject) {
                if (jsonObject.get("prerelease").getAsBoolean() || !jsonObject.has("assets"))
                    continue; // Skip pre-releases or releases without any assets.
                for (JsonElement asset : jsonObject.getAsJsonArray("assets")) {
                    String tmpUrl = asset.getAsJsonObject().get("browser_download_url").getAsString();
                    if (tmpUrl.endsWith(".zip")) {
                        downloadUrl = tmpUrl;
                        downloadName = tmpUrl.substring(
                                tmpUrl.lastIndexOf('/') + 1);
                        if (downloadName.startsWith("ItemsAdder_")) {
                            downloadName = downloadName.substring(11);
                        }
                        break root;
                    }
                }
                // Fallback just in case, thx DefaultPack 2.0.3
                String version = jsonObject.getAsJsonObject().get("tag_name").getAsString();
                downloadUrl = "https://github.com/" + repo + "/archive/refs/tags/" + version + ".zip";
                downloadName = repo.substring(i + 1) + "-" + version + ".zip";
                break;
            }
        }
        if (downloadUrl == null) {
            throw new FileNotFoundException("Failed to get asset for repo " + repo);
        }
        File destination = new File(packsRoot, downloadName);
        if (destination.exists() && destination.length() != 0) return;
        try(FileOutputStream fileOutputStream = new FileOutputStream(destination);
            InputStream inputStream = IOUtils.openUrl(downloadUrl)) {
            IOUtils.copyStream(inputStream, fileOutputStream);
        }
    }

    static void setupPermissions(CommandSender sender) {
        final PermissionHelper permissionHelper = PermissionHelper.getPermissionHelper();
        if (permissionHelper.isSupported()) {
            sender.sendMessage(ChatColor.DARK_AQUA + "Setting up user permissions...");
            permissionHelper.addDefaultPerm("ia.user.*");
            permissionHelper.addDefaultPerm("ia.user.ia");
            permissionHelper.addDefaultPerm("ia.user.ia.*");
            permissionHelper.addDefaultPerm("ia.user.iatexture");
            permissionHelper.addDefaultPerm("ia.user.recipe.*");
            permissionHelper.addDefaultPerm("ia.user.iarecipe");
            permissionHelper.addDefaultPerm("ia.user.ia.seeitem.*");
            permissionHelper.addDefaultPerm("ia.user.ia.search");
            permissionHelper.addDefaultPerm("ia.user.image.*");
            permissionHelper.addDefaultPerm("ia.user.image.gui");
            permissionHelper.addDefaultPerm("ia.user.image.chat");
            permissionHelper.addDefaultPerm("ia.user.image.book");
            permissionHelper.addDefaultPerm("ia.user.image.sign");
            permissionHelper.addDefaultPerm("ia.user.image.anvil");
            permissionHelper.addDefaultPerm("ia.user.image.hints");
            permissionHelper.addDefaultPerm("ia.user.image.use.*");
            permissionHelper.addDefaultPerm("ia.user.text_effect.*");
            permissionHelper.addDefaultPerm("ia.user.text_effect.chat");
            permissionHelper.addDefaultPerm("ia.user.text_effect.book");
            permissionHelper.addDefaultPerm("ia.user.text_effect.sign");
            permissionHelper.addDefaultPerm("ia.user.text_effect.anvil");
            permissionHelper.addDefaultPerm("ia.user.text_effect.hints");
            permissionHelper.addDefaultPerm("ia.user.text_effect.use.*");
            permissionHelper.addDefaultPerm("ia.user.iaemote");
            permissionHelper.addDefaultPerm("ia.user.iaemote.use.*");
            permissionHelper.addDefaultPerm("ia.menu.seecategory.*");
        }
    }
}
