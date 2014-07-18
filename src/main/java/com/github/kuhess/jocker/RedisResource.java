package com.github.kuhess.jocker;

public class RedisResource extends DockerResource {
    public RedisResource() {
        super("redis:latest", 6379, ResourceChecker.alwaysTrue());
    }
}
