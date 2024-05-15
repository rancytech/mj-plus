package com.github.novicezk.midjourney.store.impl;

import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.store.TaskStoreService;
import com.github.novicezk.midjourney.util.RedisHelper;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;

public class RedisTaskStoreServiceImpl implements TaskStoreService {
	private static final String KEY_PREFIX = "mj-task-store::";

	private final Duration timeout;
	private final RedisTemplate<String, Task> redisTemplate;

	public RedisTaskStoreServiceImpl(Duration timeout, RedisTemplate<String, Task> redisTemplate) {
		this.timeout = timeout;
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void save(Task task) {
		this.redisTemplate.opsForValue().set(getRedisKey(task.getId()), task, this.timeout);
	}

	@Override
	public void delete(String id) {
		this.redisTemplate.delete(getRedisKey(id));
	}

	@Override
	public Task get(String id) {
		return this.redisTemplate.opsForValue().get(getRedisKey(id));
	}

	@Override
	public List<Task> listAll() {
		return RedisHelper.list(this.redisTemplate, KEY_PREFIX);
	}

	private String getRedisKey(String id) {
		return KEY_PREFIX + id;
	}

}
