package com.github.novicezk.midjourney.support;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.novicezk.midjourney.jsr.ActivationFilter;

/**
 * 不需要激活功能此处注释掉
 */
// @Configuration
public class FilterConfig {
	// @Bean
	FilterRegistrationBean<ActivationFilter> filter1(ActivationHelper activationHelper) {
		FilterRegistrationBean<ActivationFilter> registr = new FilterRegistrationBean<>();
		registr.setFilter(new ActivationFilter(activationHelper));
		registr.addUrlPatterns(new String[] { "/mj/submit/*", "/mj/task/*", "/mj/task-admin/*", "/mj/account/*", "/mj/insight-face/*" });
		return registr;
	}
}
