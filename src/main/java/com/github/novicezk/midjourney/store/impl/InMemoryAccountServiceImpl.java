package com.github.novicezk.midjourney.store.impl;

import cn.hutool.core.collection.ListUtil;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.store.AccountStoreService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class InMemoryAccountServiceImpl implements AccountStoreService {
	private final Map<String, DiscordAccount> accountMap = Collections.synchronizedMap(new HashMap<>());

	@Override
	public void save(DiscordAccount account) {
		this.accountMap.put(account.getId(), account);
	}

	@Override
	public void delete(String key) {
		this.accountMap.remove(key);
	}

	@Override
	public DiscordAccount get(String key) {
		return this.accountMap.get(key);
	}

	@Override
	public List<DiscordAccount> listAll() {
		return ListUtil.toList(this.accountMap.values());
	}

}
