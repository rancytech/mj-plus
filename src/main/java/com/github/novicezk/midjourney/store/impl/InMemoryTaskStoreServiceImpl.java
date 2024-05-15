package com.github.novicezk.midjourney.store.impl;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.ListUtil;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.store.TaskStoreService;

import java.time.Duration;
import java.util.List;


public class InMemoryTaskStoreServiceImpl implements TaskStoreService {
	private final TimedCache<String, Task> taskMap;

	public InMemoryTaskStoreServiceImpl(Duration timeout) {
		this.taskMap = CacheUtil.newTimedCache(timeout.toMillis());
	}

	@Override
	public void save(Task task) {
		this.taskMap.put(task.getId(), task);
	}

	@Override
	public void delete(String key) {
		this.taskMap.remove(key);
	}

	@Override
	public Task get(String key) {
		return this.taskMap.get(key);
	}

	@Override
	public List<Task> listAll() {
		return ListUtil.toList(this.taskMap.iterator());
	}

}
