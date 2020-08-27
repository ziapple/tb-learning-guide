package com.ziapple.server.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringApplicationHolder implements ApplicationContextAware {
	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		SpringApplicationHolder.applicationContext = applicationContext;
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}


	public static <T> T getBean(String beanName) {
		return (T) applicationContext.getBean(beanName);
	}

	public static <T> T getBean(Class bean) {
		return (T) applicationContext.getBean(bean);
	}
}
