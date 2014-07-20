package com.github.kuhess.jocker;

import redis.clients.jedis.Jedis;

import java.util.Map;

public class RedisResource extends DockerResource {
    public RedisResource() {
        super(
                "redis:latest",
                new ResourceChecker() {
                    @Override
                    protected boolean isAvailable(String host, Map<Integer, Integer> ports) throws Exception {
                        Jedis jedis = new Jedis(host, ports.get(6379));
                        return "PONG".equals(jedis.ping());
                    }
                }
        );
    }
}
