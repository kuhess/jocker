package com.github.kuhess.jocker;

import org.junit.Rule;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RedisDemo {

    @Rule
    public RedisResource redisResource = new RedisResource();

    @Test
    public void demo_redis() throws IOException {
        Jedis jedis = new Jedis(redisResource.getHost(), redisResource.getPort());

        assertEquals("PONG", jedis.ping());
    }
}