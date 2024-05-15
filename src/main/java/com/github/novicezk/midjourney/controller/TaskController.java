package com.github.novicezk.midjourney.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.dto.TaskConditionDTO;
import com.github.novicezk.midjourney.dto.TaskQueryDTO;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.service.NotifyService;
import com.github.novicezk.midjourney.store.TaskStoreService;
import com.github.novicezk.midjourney.support.DiscordHelper;
import com.github.novicezk.midjourney.support.DomainHelper;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import com.github.novicezk.midjourney.util.PageUtils;
import com.github.novicezk.midjourney.util.SnowFlake;

import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;

@Api(tags = "任务查询")
@RestController
@RequestMapping("/mj/task")
@RequiredArgsConstructor
public class TaskController {
	private final DomainHelper domainHelper;
	private final TaskStoreService taskStoreService;
	private final DiscordLoadBalancer discordLoadBalancer;
	private final DiscordHelper discordHelper;
	private final NotifyService notifyService;

	@ApiOperation("指定ID获取任务")
	@GetMapping("/{id}/fetch")
	public Task fetch(@ApiParam("任务ID") @PathVariable String id) {
		return this.discordLoadBalancer.getQueueTasks().stream().filter(task -> CharSequenceUtil.equals(task.getId(), id)).findFirst()
				.orElseGet(() -> this.taskStoreService.get(id));
	}

	@ApiOperation(value = "查询任务队列")
	@GetMapping("/queue")
	public List<Task> queue() {
		return this.discordLoadBalancer.getQueueTasks().stream().sorted(Comparator.comparing(Task::getSubmitTime)).toList();
	}

	@ApiOperation(value = "查询所有任务")
	@GetMapping("/list")
	public List<Task> list() {
		return this.taskStoreService.listAll().stream().sorted((t1, t2) -> CompareUtil.compare(t2.getSubmitTime(), t1.getSubmitTime())).toList();
	}

	@ApiOperation(value = "分页查询任务")
	@PostMapping("/query")
	public Page<Task> query(@RequestBody TaskQueryDTO queryDTO) {
		Sort sort = PageUtils.convertSort(queryDTO.getSort(), Sort.by(Sort.Direction.DESC, "submitTime"));
		Pageable pageable = PageRequest.of(queryDTO.getPageNumber(), queryDTO.getPageSize(), sort);
		return this.taskStoreService.search(queryDTO.getCondition(), pageable);
	}

	@ApiOperation("根据ID列表查询任务")
	@PostMapping("/list-by-condition")
	public List<Task> listByCondition(@RequestBody TaskConditionDTO conditionDTO) {
		if (conditionDTO.getIds() == null) {
			return Collections.emptyList();
		}

		List<Task> result = new ArrayList<>();
		Set<String> ids = new HashSet<>(conditionDTO.getIds());
		this.discordLoadBalancer.getQueueTasks().forEach(task -> {
			if (conditionDTO.getIds().contains(task.getId())) {
				result.add(task);
				ids.remove(task.getId());
			}
		});
		ids.forEach(id -> {
			Task task = this.taskStoreService.get(id);
			if (task != null) {
				result.add(task);
			}
		});
		return result;
	}

	@ApiOperation(value = "取消任务")
	@PostMapping("/{id}/cancel")
	public Message<Void> cancel(@ApiParam(value = "任务ID") @PathVariable String id) {
		for (DiscordInstance instance : this.discordLoadBalancer.getAliveInstances()) {
			Task task = instance.getRunningTask(id);
			if (task != null) {
				String customId = task.getPropertyGeneric(Constants.TASK_PROPERTY_CUSTOM_ID);
				BotType botType = BotType.valueOf(task.getPropertyGeneric(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE));
				if (CharSequenceUtil.isNotBlank(customId)) {
					// 执行动作
					String buttonJson = task.getPropertyGeneric("cancelComponent");
					if (CharSequenceUtil.isBlank(buttonJson)) {
						return Message.of(ReturnCode.VALIDATION_ERROR, "该任务暂不支持取消");
					}
					task.cancel();
					instance.exitTask(task);
					JSONObject json = new JSONObject(buttonJson);
					return instance.action(botType, json.getString("messageId"), json.getInt("flags"), json.getInt("type"), json.getString("customId"),
							SnowFlake.INSTANCE.nextId());
				} else {
					task.cancel();
					instance.exitTask(task);
					return instance.cancel(botType, task.getPropertyGeneric(Constants.TASK_PROPERTY_PROGRESS_MESSAGE_ID), SnowFlake.INSTANCE.nextId());
				}
			} else if (instance.getRunningFutures().containsKey(id)) {
				task = this.taskStoreService.get(id);
				task.cancel();
				instance.exitTask(task);
				return Message.success();
			}
		}
		Task task = this.taskStoreService.get(id);
		if (task == null) {
			return Message.notFound();
		}
		if (TaskStatus.MODAL.equals(task.getStatus())) {
			task.cancel();
			this.taskStoreService.save(task);
			this.notifyService.notifyTaskChange(task);
			return Message.success();
		}
		return Message.of(ReturnCode.VALIDATION_ERROR, "该任务不可取消: " + task.getAction() + "-" + task.getStatus());
	}

	@ApiOperation(value = "获取任务图片的seed（需设置mj或niji的私信ID）")
	@GetMapping("/{id}/image-seed")
	public Message<String> getImageSeed(@ApiParam(value = "任务ID") @PathVariable String id) {
		Task task = this.taskStoreService.get(id);
		if (task == null) {
			return Message.notFound();
		}
		if (!Set.of(TaskAction.IMAGINE, TaskAction.VARIATION, TaskAction.UPSCALE, TaskAction.BLEND, TaskAction.PAN, TaskAction.ZOOM)
				.contains(task.getAction())) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "该任务类型不支持获取图片seed");
		}
		if (!TaskStatus.SUCCESS.equals(task.getStatus())) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "该任务状态错误");
		}
		String imageSeed = task.getProperty(Constants.TASK_PROPERTY_IMAGE_SEED, String.class);
		if (CharSequenceUtil.isNotBlank(imageSeed)) {
			return Message.success(imageSeed);
		}
		String imageUrl = task.getImageUrl();
		String messageHash = this.discordHelper.getMessageHash(imageUrl);
		if (CharSequenceUtil.isBlank(messageHash)) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "图片链接格式错误");
		}
		String instanceId = task.getProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, String.class);
		DiscordInstance discordInstance = this.discordLoadBalancer.getDiscordInstance(instanceId);
		if (discordInstance == null || !discordInstance.isAlive()) {
			return Message.of(ReturnCode.NOT_FOUND, "账号不可用: " + instanceId);
		}
		String messageId = task.getPropertyGeneric(Constants.TASK_PROPERTY_MESSAGE_ID);
		int messageFlags = task.getProperty(Constants.TASK_PROPERTY_FLAGS, Integer.class, 0);
		Message<Void> message = discordInstance.seed(messageId, messageFlags);
		if (message.getCode() != ReturnCode.SUCCESS) {
			return Message.of(message.getCode(), message.getDescription());
		}
		try {
			AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("seed:" + messageHash, Duration.ofMinutes(1));
			String seed = lock.getPropertyGeneric("seed");
			task.setProperty(Constants.TASK_PROPERTY_IMAGE_SEED, seed);
			this.taskStoreService.save(task);
			return Message.success(seed);
		} catch (TimeoutException e) {
			return Message.failure("获取seed超时");
		}
	}

	@ApiOperation("根据ID列表查询任务-字段displays")
	@PostMapping("/list-by-ids")
	public List<Map<String, Object>> listByIds(@RequestBody TaskConditionDTO conditionDTO) {
		Objects.requireNonNull(this.domainHelper);
		return listByCondition(conditionDTO).stream().map(domainHelper::convertDomainVO).toList();
	}
}
