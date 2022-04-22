package com.neu.edu.info7255.demo.dao;

import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;


@Repository
public class MessageQueueDao {

    private static Jedis jedis = new Jedis();

    public void addToQueue(String queue, String value) {
        jedis.lpush(queue, value);
    }
}
