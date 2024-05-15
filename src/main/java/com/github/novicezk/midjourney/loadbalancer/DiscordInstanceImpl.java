package com.github.novicezk.midjourney.loadbalancer;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.enums.BlendDimensions;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.exception.DiscordInstanceStartException;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.result.SubmitResultVO;
import com.github.novicezk.midjourney.service.DiscordServiceImpl;
import com.github.novicezk.midjourney.service.NotifyService;
import com.github.novicezk.midjourney.store.TaskStoreService;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import com.github.novicezk.midjourney.wss.WebSocketStarter;
import com.github.novicezk.midjourney.wss.user.UserWebSocketStarter;
import eu.maxschuster.dataurl.DataUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
public class DiscordInstanceImpl implements DiscordInstance {
	private final DiscordAccount account;
	private final WebSocketStarter socketStarter;
	private final DiscordServiceImpl service;
	private final TaskStoreService taskStoreService;
	private final NotifyService notifyService;

	private final ThreadPoolTaskExecutor taskExecutor;
	private final List<Task> runningTasks;
	private final List<Task> queueTasks;
	private final Map<String, Future<?>> taskFutureMap = Collections.synchronizedMap(new HashMap<>());

	public DiscordInstanceImpl(DiscordAccount account, UserWebSocketStarter socketStarter, RestTemplate restTemplate,
			TaskStoreService taskStoreService, NotifyService notifyService) {
		this.account = account;
		this.socketStarter = socketStarter;
		this.taskStoreService = taskStoreService;
		this.notifyService = notifyService;
		this.service = new DiscordServiceImpl(account, restTemplate);
		this.runningTasks = new CopyOnWriteArrayList<>();
		this.queueTasks = new CopyOnWriteArrayList<>();
		this.taskExecutor = new ThreadPoolTaskExecutor();
		this.taskExecutor.setCorePoolSize(account.getCoreSize());
		this.taskExecutor.setMaxPoolSize(account.getCoreSize());
		this.taskExecutor.setQueueCapacity(account.getQueueSize());
		this.taskExecutor.setThreadNamePrefix("TaskQueue-" + account.getDisplay() + "-");
		this.taskExecutor.initialize();
	}

	@Override
	public String getInstanceId() {
		return this.account.getId();
	}

	@Override
	public DiscordAccount account() {
		return this.account;
	}

	@Override
	public boolean isAlive() {
		return this.account.isEnable();
	}

	@Override
	public void start() throws DiscordInstanceStartException {
		try {
			this.service.initApplicationCommands();
			this.socketStarter.setTrying(true);
			this.socketStarter.start();
			AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("wss:" + this.account.getChannelId(),
					Duration.ofSeconds(30));
			if (ReturnCode.SUCCESS != lock.getProperty("code", Integer.class, 0)) {
				throw new ValidateException(lock.getProperty("description", String.class));
			}
			this.service.startInteractionInterval();
		} catch (Exception e) {
			log.error("Start discord instance error", e);
			this.socketStarter.setTrying(false);
			this.socketStarter.stop();
			throw new DiscordInstanceStartException(e.getMessage());
		}
	}

	@Override
	public void stop() {
		this.socketStarter.stop();
		this.service.clearInteractionInterval();
	}

	@Override
	public boolean needModal(BotType botType, TaskAction action, String customId) {
		if (CharSequenceUtil.containsAny(customId, "CustomZoom", "PicReader", "Job::PromptAnalyzer::", "Inpaint")) {
			return true;
		}
		if (!this.account.customRemix(botType) || this.account.isRemixAutoSubmit()) {
			return false;
		}
		return customId.contains("reroll") || Set.of(TaskAction.VARIATION, TaskAction.PAN).contains(action);
	}

	@Override
	public List<Task> getRunningTasks() {
		return this.runningTasks;
	}

	@Override
	public void exitTask(Task task) {
		try {
			Future<?> future = this.taskFutureMap.get(task.getId());
			if (future != null) {
				future.cancel(true);
			}
			saveAndNotify(task);
		} finally {
			this.runningTasks.remove(task);
			this.queueTasks.remove(task);
			this.taskFutureMap.remove(task.getId());
		}
	}

	@Override
	public Map<String, Future<?>> getRunningFutures() {
		return this.taskFutureMap;
	}

	@Override
	public synchronized SubmitResultVO submitTask(Task task, Callable<Message<Void>> discordSubmit) {
		this.taskStoreService.save(task);
		int currentWaitNumbers;
		try {
			currentWaitNumbers = this.taskExecutor.getThreadPoolExecutor().getQueue().size();
			Future<?> future = this.taskExecutor.submit(() -> executeTask(task, discordSubmit));
			this.taskFutureMap.put(task.getId(), future);
			this.queueTasks.add(task);
		} catch (RejectedExecutionException e) {
			this.taskStoreService.delete(task.getId());
			return SubmitResultVO.fail(ReturnCode.QUEUE_REJECTED, "队列已满，请稍后尝试")
					.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, this.getInstanceId());
		} catch (Exception e) {
			log.error("submit task error", e);
			return SubmitResultVO.fail(ReturnCode.FAILURE, "提交失败，系统异常")
					.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, this.getInstanceId());
		}
		if (currentWaitNumbers == 0) {
			return SubmitResultVO.of(ReturnCode.SUCCESS, "提交成功", task.getId())
					.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, this.getInstanceId());
		} else {
			return SubmitResultVO.of(ReturnCode.IN_QUEUE, "排队中，前面还有" + currentWaitNumbers + "个任务", task.getId())
					.setProperty("numberOfQueues", currentWaitNumbers)
					.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, this.getInstanceId());
		}
	}

	private void executeTask(Task task, Callable<Message<Void>> discordSubmit) {
		this.runningTasks.add(task);
		try {
			Message<Void> result = discordSubmit.call();
			task.setStartTime(System.currentTimeMillis());
			if (result.getCode() != ReturnCode.SUCCESS) {
				task.fail(result.getDescription());
				saveAndNotify(task);
				log.debug("task finished, id: {}, status: {}", task.getId(), task.getStatus());
				return;
			}
			task.setStatus(TaskStatus.SUBMITTED);
			task.setProgress("0%");
			asyncSaveAndNotify(task);
			do {
				task.sleep();
				asyncSaveAndNotify(task);
			} while (task.getStatus() == TaskStatus.IN_PROGRESS);
			log.debug("task finished, id: {}, status: {}", task.getId(), task.getStatus());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			log.error("task execute error", e);
			task.fail("执行错误，系统异常");
			saveAndNotify(task);
		} finally {
			this.runningTasks.remove(task);
			this.queueTasks.remove(task);
			this.taskFutureMap.remove(task.getId());
		}
	}

	private void asyncSaveAndNotify(Task task) {
		ThreadUtil.execute(() -> saveAndNotify(task));
	}

	private void saveAndNotify(Task task) {
		this.taskStoreService.save(task);
		this.notifyService.notifyTaskChange(task);
	}

	@Override
	public Message<Void> imagine(BotType botType, String prompt, String nonce) {
		return this.service.imagine(botType, prompt, nonce);
	}

	@Override
	public Message<Void> action(BotType botType, String messageId, int messageFlags, int componentType, String customId,
			String nonce) {
		return this.service.action(botType, messageId, messageFlags, componentType, customId, nonce);
	}

	@Override
	public Message<Void> modal(BotType botType, String messageId, String customId, String promptCustomId, String prompt,
			String nonce) {
		return this.service.modal(botType, messageId, customId, promptCustomId, prompt, nonce);
	}

	@Override
	public Message<Void> regionModal(String customId, String prompt, String maskBase64) {
		return this.service.regionModal(customId, prompt, maskBase64);
	}

	public Message<List<String>> sendImageMessages(String content, List<String> finalFileNames) {
		return this.service.sendImageMessages(content, finalFileNames);
	}

	@Override
	public Message<Void> describe(BotType botType, String finalFileName, String nonce) {
		return this.service.describe(botType, finalFileName, nonce);
	}

	@Override
	public Message<Void> blend(BotType botType, List<String> finalFileNames, BlendDimensions dimensions, String nonce) {
		return this.service.blend(botType, finalFileNames, dimensions, nonce);
	}

	@Override
	public Message<Void> shorten(BotType botType, String prompt, String nonce) {
		return this.service.shorten(botType, prompt, nonce);
	}

	@Override
	public Message<Void> cancel(BotType botType, String messageId, String nonce) {
		return this.service.cancel(botType, messageId, nonce);
	}

	@Override
	public List<Task> getQueueTasks() {
		return this.queueTasks;
	}

	@Override
	public Message<Void> info(BotType botType, String nonce) {
		return this.service.info(botType, nonce);
	}

	@Override
	public Message<Void> settings(BotType botType, String nonce) {
		return this.service.settings(botType, nonce);
	}

	@Override
	public Message<Void> changeVersion(String version, String nonce) {
		return this.service.changeVersion(version, nonce);
	}

	public String getChannelId() {
		return this.account.getChannelId();
	}

	@Override
	public Message<Void> seed(String messageId, int messageFlags) {
		return this.service.seed(messageId, messageFlags);
	}

	@Override
	public Message<String> upload(String fileName, DataUrl dataUrl) {
		return this.service.upload(fileName, dataUrl);
	}

	@Override
	public Message<String> sendImageMessage(String content, String finalFileName) {
		return this.service.sendImageMessage(content, finalFileName);
	}

	@Override
	public Message<Void> saveInsightFace(String name, String finalFileName, String nonce) {
		return this.service.saveInsightFace(name, finalFileName, nonce);
	}

	@Override
	public Message<Void> swapInsightFace(String name, String finalFileName, String nonce) {
		return this.service.swapInsightFace(name, finalFileName, nonce);
	}

	@Override
	public Message<Void> delInsightFace(String name, String nonce) {
		return this.service.delInsightFace(name, nonce);
	}
}
