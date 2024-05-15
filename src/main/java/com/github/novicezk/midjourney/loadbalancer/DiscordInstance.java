package com.github.novicezk.midjourney.loadbalancer;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.condition.TaskCondition;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.exception.DiscordInstanceStartException;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.result.SubmitResultVO;
import com.github.novicezk.midjourney.service.DiscordService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public interface DiscordInstance extends DiscordService {

	String getInstanceId();

	DiscordAccount account();

	boolean isAlive();

	void start() throws DiscordInstanceStartException;

	void stop();

	boolean needModal(BotType botType, TaskAction action, String customId);

	List<Task> getRunningTasks();

	List<Task> getQueueTasks();

	String getChannelId();

	void exitTask(Task task);

	Map<String, Future<?>> getRunningFutures();

	SubmitResultVO submitTask(Task task, Callable<Message<Void>> discordSubmit);

	default Stream<Task> findRunningTask(TaskCondition condition) {
		return getRunningTasks().stream().filter(condition);
	}

	default Task getRunningTask(String id) {
		return getRunningTasks().stream().filter(t -> id.equals(t.getId())).findFirst().orElse(null);
	}

	default Task getRunningTaskByNonce(String nonce) {
		if (CharSequenceUtil.isBlank(nonce)) {
			return null;
		}
		TaskCondition condition = new TaskCondition().setNonce(nonce);
		return findRunningTask(condition).findFirst().orElse(null);
	}
}
