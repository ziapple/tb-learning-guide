package com.ziapple.test.spring;

import com.ziapple.dao.cache.CacheSpecs;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "caffeine")
public class ContextConfiguration {
    private Map<String, CacheSpecs> specs;
    private String name;

    @Bean
    public CacheManager cacheManager() {
        System.out.println("specs=" + specs);
        SimpleCacheManager manager = new SimpleCacheManager();
        return manager;
    }

    // Spring初始化构造函数,参数从Properties里面读取
    public ContextConfiguration(@Value("${cache.type}") String name){
        this.name = name;
        System.out.println("name=" + name);
    }
}
