package com.github.novicezk.midjourney.admin;

import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.dto.TaskQueryDTO;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.store.TaskStoreService;
import com.github.novicezk.midjourney.support.DomainHelper;
import com.github.novicezk.midjourney.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/mj/task-admin")
@RequiredArgsConstructor
public class TaskAdminController {
	private final TaskStoreService taskStoreService;
	private final DiscordLoadBalancer discordLoadBalancer;
	private final DomainHelper domainHelper;

	@GetMapping("/{id}/fetch")
	public Map<String, Object> fetch(@PathVariable String id) {
		Task task = this.taskStoreService.get(id);
		if (task == null) {
			return null;
		}
		return this.domainHelper.convertDomainVO(task);
	}

	@PostMapping("/query")
	public Page<Map<String, Object>> query(@RequestBody TaskQueryDTO queryDTO) {
		Sort sort = PageUtils.convertSort(queryDTO.getSort(), Sort.by(Sort.Direction.DESC, "submitTime"));
		Pageable pageable = PageRequest.of(queryDTO.getPageNumber(), queryDTO.getPageSize(), sort);
		Page<Task> pageResult = this.taskStoreService.search(queryDTO.getCondition(), pageable);
		List<Map<String, Object>> voList = pageResult.getContent().stream().map(this.domainHelper::convertDomainVO).toList();
		return new PageImpl<>(voList, pageable, pageResult.getTotalElements());
	}

	@GetMapping("/{instanceId}/queue")
	public List<Map<String, Object>> instanceQueue(@PathVariable String instanceId) {
		DiscordInstance instance = this.discordLoadBalancer.getDiscordInstance(instanceId);
		if (instance == null || !instance.isAlive()) {
			return Collections.emptyList();
		}
		return instance.getRunningFutures().keySet().stream()
				.map(this.taskStoreService::get).filter(Objects::nonNull)
				.sorted(Comparator.comparing(Task::getSubmitTime))
				.map(this.domainHelper::convertDomainVO)
				.toList();
	}
}
