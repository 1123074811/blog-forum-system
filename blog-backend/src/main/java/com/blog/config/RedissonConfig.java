package com.blog.config;

import org.redisson.api.RedissonClient;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.database}")
    private int database;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        var singleServer = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(8);

        String normalizedPassword = password == null ? null : password.trim();
        if (StringUtils.hasText(normalizedPassword)) {
            singleServer.setPassword(normalizedPassword);
        }

        return Redisson.create(config);
    }
}
