package com.fox2code.itemsaddermanager.applier;

import com.fox2code.itemsaddermanager.iapack.ItemsAdderPack;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AnimationMergerResourceApplier extends ResourceApplier {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson gsonRaw = new GsonBuilder().create();
    public static final AnimationMergerResourceApplier
            ANIMATION = new AnimationMergerResourceApplier();

    private AnimationMergerResourceApplier() {
        super(true);
    }

    @Override
    public void applyResource(ItemsAdderPack itemsAdderPack, String path, File destination) throws IOException {
        if (!destination.exists()) {
            super.applyResource(itemsAdderPack, path, destination);
        }
        InputStream inputStream = itemsAdderPack.getResourceNotMissing(path);
        JsonObject jsonObjectPack;
        try (inputStream) {
            jsonObjectPack = JsonParser.parseReader(new InputStreamReader(
                    inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        if (!jsonObjectPack.has("animations")) return;
        JsonArray packAnimations = jsonObjectPack.getAsJsonArray("animations");
        if (packAnimations == null || packAnimations.isEmpty()) return;
        JsonObject jsonObjectReal;
        try (FileInputStream fileInputStream = new FileInputStream(destination)) {
            jsonObjectReal = JsonParser.parseReader(new InputStreamReader(
                    fileInputStream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (String completer : new String[]{"bones", "outliner", "elements", "visible_box"}) {
            if (jsonObjectPack.has(completer) && !jsonObjectReal.has(completer)) {
                jsonObjectReal.add(completer, jsonObjectPack.get(completer));
            }
        }
        if (!jsonObjectReal.has("animations")) {
            if (jsonObjectPack.has("resolution")) {
                jsonObjectReal.add("resolution",
                        jsonObjectPack.get("resolution"));
            } else {
                jsonObjectPack.remove("resolution");
            }
            jsonObjectReal.add("animations", packAnimations);
        } else {
            JsonArray realAnimations = jsonObjectReal.getAsJsonArray("animations");
            if (realAnimations == null) {
                jsonObjectReal.add("animations", packAnimations);
                if (jsonObjectPack.has("resolution")) {
                    jsonObjectReal.add("resolution",
                            jsonObjectPack.get("resolution"));
                } else {
                    jsonObjectPack.remove("resolution");
                }
            } else {
                boolean resolution; // Check if resolution is matching
                if ((resolution = jsonObjectPack.has("resolution")) !=
                        jsonObjectReal.has("resolution")) {
                    throw new IOException(resolution ?
                            "New pack try to remove resolution element" :
                            "New pack try to add resolution element");
                }
                if (resolution) {
                    JsonObject resolutionReal = jsonObjectReal.getAsJsonObject("resolution");
                    JsonObject resolutionPack = jsonObjectPack.getAsJsonObject("resolution");
                    if (!resolutionReal.get("width").equals(resolutionPack.get("width")) ||
                            !resolutionReal.get("height").equals(resolutionPack.get("height"))) {
                        throw new IOException("Trying to change resolution from " +
                                gsonRaw.toJson(resolutionReal) + " to " + gsonRaw.toJson(resolutionPack));
                    }
                }
                // Avoid animations duplicates?
                realAnimations.addAll(packAnimations);
            }
        }
        if (jsonObjectPack.has("textures")) {
            JsonArray packTextures = jsonObjectPack.getAsJsonArray("textures");
            if (packTextures != null && !packTextures.isEmpty()) {
                if (!jsonObjectReal.has("textures")) {
                    jsonObjectReal.add("textures", jsonObjectPack.get("textures"));
                } else {
                    JsonArray realTextures = jsonObjectReal.getAsJsonArray("textures");
                    if (realTextures == null || realTextures.isEmpty()) {
                        jsonObjectReal.add("textures", jsonObjectPack.get("textures"));
                    } else {
                        realTextures.addAll(packTextures);
                    }
                }
            }
        }
        Files.writeString(destination.toPath(), gson.toJson(jsonObjectReal));
    }
}
