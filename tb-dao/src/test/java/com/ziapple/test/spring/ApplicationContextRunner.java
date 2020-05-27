package com.ziapple.test.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ApplicationContextRunner.class})
@TestPropertySource(locations = {"classpath:application-test.properties"})
@ComponentScan(basePackages="com.ziapple.test.spring")
public class ApplicationContextRunner {
    @Test
    public void testProperties(){
        System.out.println("initializing...");
    }
}
