package com.github.kuhess.jocker;

import com.jayway.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public abstract class ResourceChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceChecker.class);

    public static final int DEFAULT_POLL_INTERVAL_IN_MS = 1000;
    public static final int DEFAULT_MAX_WAIT_TIME_IN_MS = 30000;

    private final int pollIntervalInMs;
    private final int maxWaitTimeInMs;

    public ResourceChecker() {
        this(DEFAULT_POLL_INTERVAL_IN_MS, DEFAULT_MAX_WAIT_TIME_IN_MS);
    }

    public ResourceChecker(int pollIntervalInMs, int maxWaitTimeInMs) {
        this.pollIntervalInMs = pollIntervalInMs;
        this.maxWaitTimeInMs = maxWaitTimeInMs;
    }

    protected abstract boolean isAvailable(String host, Map<Integer, Integer> ports) throws Exception;

    public void waitForAvailability(String host, Map<Integer, Integer> ports) {
        Awaitility.await()
                .pollInterval(this.pollIntervalInMs, TimeUnit.MILLISECONDS)
                .atMost(maxWaitTimeInMs, TimeUnit.MILLISECONDS)
                .until(this.isAvailableWrapper(host, ports));
    }

    private Callable<Boolean> isAvailableWrapper(final String host, final Map<Integer, Integer> ports) {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                try {
                    return isAvailable(host, ports);
                } catch (Exception e) {
                    LOGGER.debug("Jocker resource not (yet) available", e);
                    return false;
                }
            }
        };
    }
}
