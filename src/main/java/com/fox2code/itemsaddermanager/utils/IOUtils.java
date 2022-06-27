package com.fox2code.itemsaddermanager.utils;

import com.fox2code.itemsaddermanager.ItemsAdderManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Objects;

public class IOUtils {
    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        copyStream(inputStream, buffer);
        return buffer.toByteArray();
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            outputStream.write(data, 0, nRead);
        }
    }

    public static void copyFolderContent(File source, File target) throws IOException {
        if (!target.isDirectory() && !target.mkdirs()) {
            throw new IOException("Failed to make directory: " + target.getPath());
        }
        if (!source.isDirectory()) return;
        for (String f : Objects.requireNonNull(source.list())) {
            if (f.equals("pack.zip") || f.equals("index.html")) continue;
            File source2 = new File(source, f);
            if (!source2.isDirectory()) continue;
            copyFolderContent0(source2, new File(target, f));
        }
    }

    private static void copyFolderContent0(File source, File target) throws IOException {
        if (!target.isDirectory() && !target.mkdirs()) {
            throw new IOException("Failed to make directory: " + target.getPath());
        }
        for (String f : Objects.requireNonNull(source.list())) {
            File source2 = new File(source, f);
            if (source2.isDirectory()) {
                copyFolderContent0(source2, new File(target, f));
            } else {
                copyFile0(source2, new File(target, f));
            }
        }
    }

    private static void copyFile0(File source, File target) throws IOException {
        Files.copy(source.toPath(), target.toPath());
    }

    public static void deleteDataFolderContent(File folder) throws IOException {
        if (!folder.isDirectory()) return;
        for (String f : Objects.requireNonNull(folder.list())) {
            if (f.equals(".iam")) continue;
            if (f.equals("resource_pack")) {
                deleteResourceFolderContent(new File(folder, f));
                continue;
            }
            deleteRecursively(new File(folder, f));
        }
    }

    public static void deleteResourceFolderContent(File folder) throws IOException {
        if (!folder.isDirectory()) return;
        for (String f : Objects.requireNonNull(folder.list())) {
            if (f.equals("pack.zip") || f.equals("index.html")) continue;
            deleteRecursively(new File(folder, f));
        }
    }

    public static void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            for (File c : Objects.requireNonNull(file.listFiles()))
                deleteRecursively(c);
        }
        if (file.exists() && !file.delete())
            throw new IOException("Failed to delete file: " + file.getPath());
    }

    public static InputStream openUrl(String url) throws IOException {
        URLConnection urlConnection = new URL(url).openConnection();
        urlConnection.setAllowUserInteraction(false);
        urlConnection.setDefaultUseCaches(false);
        if (urlConnection instanceof HttpURLConnection httpURLConnection) {
            httpURLConnection.setInstanceFollowRedirects(true);
        }
        urlConnection.setRequestProperty(
                "Upgrade-Insecure-Requests", "1");
        urlConnection.setRequestProperty("User-Agent",
                "ItemsAdderManager/" + ItemsAdderManager.getVersion());
        if (url.startsWith("https://api.github.com/")) {
            urlConnection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        }
        return urlConnection.getInputStream();
    }
}
