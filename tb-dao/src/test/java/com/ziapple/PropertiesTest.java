package com.ziapple;

import com.ziapple.dao.cache.CacheSpecs;
import lombok.Data;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "caffeine")
@RunWith(SpringRunner.class)
@TestPropertySource(locations = {"classpath:application-test.properties"})
@ContextConfiguration(classes = {PropertiesTest.class})
public class PropertiesTest {
    private Map<String, CacheSpecs> specs;

    @Bean
    public CacheManager cacheManager() {
        System.out.println(specs);
        SimpleCacheManager manager = new SimpleCacheManager();
        return manager;
    }

    @Test
    public void testProperties(){
        System.out.println(specs);
    }
}
