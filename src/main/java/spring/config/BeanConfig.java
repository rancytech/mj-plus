package spring.config;

import cn.hutool.core.util.ReflectUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.enums.StoreType;
import com.github.novicezk.midjourney.enums.TranslateWay;
import com.github.novicezk.midjourney.loadbalancer.rule.IRule;
import com.github.novicezk.midjourney.service.TranslateService;
import com.github.novicezk.midjourney.service.translate.AdapterTranslateServiceImpl;
import com.github.novicezk.midjourney.service.translate.BaiduTranslateServiceImpl;
import com.github.novicezk.midjourney.service.translate.DeepLTranslateServiceImpl;
import com.github.novicezk.midjourney.service.translate.GPTTranslateServiceImpl;
import com.github.novicezk.midjourney.service.translate.NoTranslateServiceImpl;
import com.github.novicezk.midjourney.store.AccountStoreService;
import com.github.novicezk.midjourney.store.TaskStoreService;
import com.github.novicezk.midjourney.store.impl.InMemoryAccountServiceImpl;
import com.github.novicezk.midjourney.store.impl.InMemoryTaskStoreServiceImpl;
import com.github.novicezk.midjourney.store.impl.MySQLAccountServiceImpl;
import com.github.novicezk.midjourney.store.impl.MySQLTaskStoreServiceImpl;
import com.github.novicezk.midjourney.store.impl.RedisAccountStoreServiceImpl;
import com.github.novicezk.midjourney.store.impl.RedisTaskStoreServiceImpl;
import com.github.novicezk.midjourney.util.RedisHelper;
import com.github.novicezk.midjourney.wss.handle.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Configuration
public class BeanConfig {
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private ProxyProperties properties;

	@Bean
	RedisTemplate<String, Task> taskRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		return RedisHelper.createTaskRedisTemplate(redisConnectionFactory, Task.class);
	}

	@Bean
	TaskStoreService taskStoreService(RedisConnectionFactory redisConnectionFactory) {
		StoreType type = this.properties.getTaskStore().getType();
		Duration timeout = this.properties.getTaskStore().getTimeout();
		return switch (type) {
			case IN_MEMORY -> new InMemoryTaskStoreServiceImpl(timeout);
			case REDIS -> new RedisTaskStoreServiceImpl(timeout, taskRedisTemplate(redisConnectionFactory));
			case MYSQL -> new MySQLTaskStoreServiceImpl();
		};
	}

	@Bean
	RedisTemplate<String, DiscordAccount> accountRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		return RedisHelper.createTaskRedisTemplate(redisConnectionFactory, DiscordAccount.class);
	}

	@Bean
	AccountStoreService accountStoreService(RedisConnectionFactory redisConnectionFactory) {
		StoreType type = this.properties.getAccountStoreType();
		return switch (type) {
			case IN_MEMORY -> new InMemoryAccountServiceImpl();
			case REDIS -> new RedisAccountStoreServiceImpl(accountRedisTemplate(redisConnectionFactory));
			case MYSQL -> new MySQLAccountServiceImpl();
		};
	}

	@Bean
	TranslateService translateService() {
		var translateZh2EnService = getTranslateServiceByWay(this.properties.getTranslateWay());
		var translateEn2ZhService = getTranslateServiceByWay(this.properties.getTranslateZhWay());
		return new AdapterTranslateServiceImpl(translateZh2EnService, translateEn2ZhService);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public IRule loadBalancerRule() {
		String ruleClassName = IRule.class.getPackageName() + "." + this.properties.getAccountChooseRule();
		return ReflectUtil.newInstance(ruleClassName);
	}

	@Bean
	List<MessageHandler> messageHandlers() {
		return this.applicationContext.getBeansOfType(MessageHandler.class).values().stream()
				.sorted(Comparator.comparingInt(MessageHandler::order))
				.toList();
	}

	private TranslateService getTranslateServiceByWay(TranslateWay translateWay) {
		return switch (translateWay) {
			case BAIDU -> new BaiduTranslateServiceImpl(this.properties.getBaiduTranslate());
			case GPT -> new GPTTranslateServiceImpl(this.properties);
			case DEEPL -> new DeepLTranslateServiceImpl(this.properties.getDeeplTranslate());
			default -> new NoTranslateServiceImpl();
		};
	}
}
