package com.github.kuhess.jocker;

import com.jayway.awaitility.Awaitility;

import java.util.concurrent.Callable;

public abstract class ResourceChecker {
    public void waitForAvailability() {
        Awaitility.await().until(isAvailableWrapper());
    }

    private Callable<Boolean> isAvailableWrapper() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                try {
                    return isAvailable();
                } catch (Exception e) {
                    return false;
                }
            }
        };
    }

    protected abstract boolean isAvailable() throws Exception;

    public static ResourceChecker alwaysTrue() {
        return new ResourceChecker() {
            @Override
            protected boolean isAvailable() throws Exception {
                return true;
            }
        };
    }
}
