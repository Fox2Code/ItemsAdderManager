package com.fox2code.itemsaddermanager.applier;

import java.io.IOException;

public final class MissingDeclaredResourceException extends IOException {
    public MissingDeclaredResourceException(String path) {
        super(path);
    }
}
