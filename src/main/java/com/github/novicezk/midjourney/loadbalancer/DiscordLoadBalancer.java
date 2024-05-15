package com.github.novicezk.midjourney.loadbalancer;

import cn.hutool.core.text.CharSequenceUtil;

import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.loadbalancer.rule.IRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class DiscordLoadBalancer {
	private final IRule rule;

	private final List<DiscordInstance> instances = Collections.synchronizedList(new ArrayList<>());

	public List<DiscordInstance> getAllInstances() {
		return this.instances;
	}

	public List<DiscordInstance> getAliveInstances() {
		return this.instances.stream().filter(DiscordInstance::isAlive).toList();
	}

	public DiscordInstance chooseInstanceByFilter(Predicate<DiscordInstance> filter) {
		List<DiscordInstance> filterInstances = getAliveInstances().stream().filter(filter).toList();
		return this.rule.choose(filterInstances);
	}

	public DiscordInstance chooseInstance() {
		return this.rule.choose(getAliveInstances());
	}

	public DiscordInstance getDiscordInstance(String instanceId) {
		if (CharSequenceUtil.isBlank(instanceId)) {
			return null;
		}
		return this.instances.stream().filter(instance -> CharSequenceUtil.equals(instanceId, instance.getInstanceId())).findFirst().orElse(null);
	}

	@Deprecated
	public Set<String> getQueueTaskIds() {
		Set<String> taskIds = Collections.synchronizedSet(new HashSet<>());
		for (DiscordInstance instance : getAliveInstances()) {
			taskIds.addAll(instance.getRunningFutures().keySet());
		}
		return taskIds;
	}

	public List<Task> getQueueTasks() {
		List<Task> list = new ArrayList<>();
		for (DiscordInstance item : this.getAliveInstances()) {
			list.addAll(item.getQueueTasks());
		}

		return list;
	}
}
