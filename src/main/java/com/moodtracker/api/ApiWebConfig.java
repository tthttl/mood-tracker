package com.moodtracker.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class ApiWebConfig implements WebMvcConfigurer {

	static final String BASE_PATH = "/api/v1";

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.addPathPrefix(BASE_PATH, HandlerTypePredicate.forBasePackage("com.moodtracker.api"));
	}

}
