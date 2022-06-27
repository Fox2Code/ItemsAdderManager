package com.fox2code.itemsaddermanager.iapack;

import java.io.IOException;

public final class ItemsAdderResourcePackException extends IOException {
    public static final ItemsAdderResourcePackException
            INSTANCE = new ItemsAdderResourcePackException();

    private ItemsAdderResourcePackException() {}

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
