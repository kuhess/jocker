package com.github.kuhess.jocker;

import com.jayway.awaitility.Awaitility;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public abstract class ResourceChecker {
    public void waitForAvailability(String host, Map<Integer, Integer> ports) {
        Awaitility.await()
                .pollInterval(1000, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(this.isAvailableWrapper(host, ports));
    }

    private Callable<Boolean> isAvailableWrapper(final String host, final Map<Integer, Integer> ports) {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                try {
                    return isAvailable(host, ports);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        };
    }

    protected abstract boolean isAvailable(String host, Map<Integer, Integer> ports) throws Exception;

    public static ResourceChecker alwaysTrue() {
        return new ResourceChecker() {
            @Override
            protected boolean isAvailable(String host, Map<Integer, Integer> ports) throws Exception {
                return true;
            }
        };
    }
}
