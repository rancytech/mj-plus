package com.github.novicezk.midjourney.store.impl;

import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.store.AccountStoreService;
import com.github.novicezk.midjourney.util.RedisHelper;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

public class RedisAccountStoreServiceImpl implements AccountStoreService {
	private static final String KEY_PREFIX = "mj-account-store::";

	private final RedisTemplate<String, DiscordAccount> redisTemplate;

	public RedisAccountStoreServiceImpl(RedisTemplate<String, DiscordAccount> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void save(DiscordAccount account) {
		this.redisTemplate.opsForValue().set(getRedisKey(account.getId()), account);
	}

	@Override
	public void delete(String id) {
		this.redisTemplate.delete(getRedisKey(id));
	}

	@Override
	public DiscordAccount get(String id) {
		return this.redisTemplate.opsForValue().get(getRedisKey(id));
	}

	@Override
	public List<DiscordAccount> listAll() {
		return RedisHelper.list(this.redisTemplate, KEY_PREFIX);
	}

	private String getRedisKey(String id) {
		return KEY_PREFIX + id;
	}
}
