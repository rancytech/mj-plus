package spring.config;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.support.ApiAuthorizeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
	@Autowired
	private ApiAuthorizeInterceptor apiAuthorizeInterceptor;
	@Autowired
	private ProxyProperties properties;

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/mj/doc.html").setViewName("redirect:/doc.html");
		registry.addViewController("/mj").setViewName("redirect:/doc.html");
		registry.addViewController("/doc").setViewName("redirect:/doc.html");
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedOrigins("*")
				.allowedMethods("*").allowedHeaders("*");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		if (CharSequenceUtil.isNotBlank(this.properties.getApiSecret())) {
			registry.addInterceptor(this.apiAuthorizeInterceptor)
					.addPathPatterns("/mj/task-admin/**", "/mj/account/**", "/mj/act/**", "/mj/submit/**", "/mj/insight-face/**", "/mj/task/**");
		}
	}

}
