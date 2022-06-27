package com.fox2code.itemsaddermanager.applier;

import com.fox2code.itemsaddermanager.iapack.ItemsAdderPack;

import java.io.File;
import java.io.IOException;

public class ResourceApplier {
    public static final ResourceApplier DEFAULT = new ResourceApplier(true);
    public static final ResourceApplier SPECIAL = new ResourceApplier(false);

    private final boolean protect;

    protected ResourceApplier(boolean protect) {
        this.protect = protect;
    }

    public void applyResource(ItemsAdderPack itemsAdderPack, String path, File destination) throws IOException {
        if ( this.protect && destination.exists()) return;
        itemsAdderPack.tryExtractEntryToNotMissing(path, destination);
    }
}
