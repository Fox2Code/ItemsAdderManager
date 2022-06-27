package com.fox2code.itemsaddermanager.compat;

import com.fox2code.itemsaddermanager.ItemsAdderManager;
import com.fox2code.itemsaddermanager.utils.IOUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SlimeFunCompat {
    private static final String ITEM_MODELS = "com/fox2code/itemsaddermanager/compat/item-models.yml";

    public static void applyCompat(JavaPlugin self, File plugins) {
        File target = new File(plugins, "Slimefun" + File.separator + "item-models.yml");
        if (!target.exists()) {
            File parent = target.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) return;
            InputStream inputStream = self.getResource(ITEM_MODELS);
            if (inputStream == null) return;
            try (inputStream; FileOutputStream fileOutputStream = new FileOutputStream(target)) {
                IOUtils.copyStream(inputStream, fileOutputStream);
            } catch (IOException e) {
                ItemsAdderManager.getInstance().getLogger().warning("Failed to apply SlimeFun compat.");
            }
        }
    }
}
