package com.github.novicezk.midjourney.util;


import com.github.novicezk.midjourney.domain.DomainObject;
import lombok.experimental.UtilityClass;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class RedisHelper {

	public static <T extends DomainObject> RedisTemplate<String, T> createTaskRedisTemplate(RedisConnectionFactory redisConnectionFactory, Class<T> clazz) {
		RedisTemplate<String, T> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(clazz));
		return redisTemplate;
	}

	public static <T extends DomainObject> List<T> list(RedisTemplate<String, T> redisTemplate, String keyPrefix) {
		Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
			Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(keyPrefix + "*").count(1000).build());
			return cursor.stream().map(String::new).collect(Collectors.toSet());
		});
		if (keys == null || keys.isEmpty()) {
			return Collections.emptyList();
		}
		ValueOperations<String, T> operations = redisTemplate.opsForValue();
		return keys.stream().map(operations::get)
				.filter(Objects::nonNull)
				.toList();
	}
}
