package com.github.novicezk.midjourney.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.enums.BlendDimensions;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.result.SubmitResultVO;
import com.github.novicezk.midjourney.store.TaskStoreService;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import com.github.novicezk.midjourney.util.MimeTypeUtils;
import eu.maxschuster.dataurl.DataUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
	private final TaskStoreService taskStoreService;
	private final DiscordLoadBalancer discordLoadBalancer;

	@Override
	public SubmitResultVO submitImagine(DiscordInstance instance, Task task, List<DataUrl> dataUrls) {
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, instance.getInstanceId());
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_CHANNEL_ID, instance.getChannelId());
		return instance.submitTask(task, () -> {
			List<String> imageUrls = new ArrayList<>();
			for (DataUrl dataUrl : dataUrls) {
				String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
				Message<String> uploadResult = instance.upload(taskFileName, dataUrl);
				if (uploadResult.getCode() != ReturnCode.SUCCESS) {
					return Message.of(uploadResult.getCode(), uploadResult.getDescription());
				}
				imageUrls.add(uploadResult.getResult());
			}

			if (!dataUrls.isEmpty()) {
				Message<List<String>> sendImage = instance.sendImageMessages("upload images", imageUrls);
				if (sendImage.getCode() != 1) {
					return Message.of(sendImage.getCode(), sendImage.getDescription());
				}

				String sendImageResult = String.join(" ", sendImage.getResult());
				task.setPrompt(sendImageResult + " " + task.getPrompt());
				task.setPromptEn(sendImageResult + " " + task.getPromptEn());
				task.setDescription("/imagine " + task.getPrompt());
				this.taskStoreService.save(task);
			}

			BotType botType = BotType.valueOf(task.getPropertyGeneric(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE));
			return instance.imagine(botType, task.getPromptEn(), task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE));
		});
	}

	@Override
	public SubmitResultVO submitAction(DiscordInstance instance, Task task, String targetMessageId, int messageFlags, int componentType, String customId) {
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, instance.getInstanceId());
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_CHANNEL_ID, instance.getChannelId());
		task.setProperty(Constants.TASK_PROPERTY_CUSTOM_ID, customId);
		BotType botType = BotType.valueOf(task.getProperty(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE, String.class, BotType.MID_JOURNEY.name()));
		boolean needModal = instance.needModal(botType, task.getAction(), customId);
		if (Boolean.TRUE.equals(task.getProperty("inpaintReroll"))) {
			needModal = false;
		}

		task.setProperty(Constants.TASK_PROPERTY_NEED_MODAL, needModal);
		String nonce = task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE);
		if (!needModal) {
			return instance.submitTask(task, () -> instance.action(botType, targetMessageId, messageFlags, componentType, customId, nonce));
		}

		Message<Void> actionResult = instance.action(botType, targetMessageId, messageFlags, componentType, customId, nonce);
		if (actionResult.getCode() != ReturnCode.SUCCESS) {
			return SubmitResultVO.fail(actionResult.getCode(), actionResult.getDescription());
		}

		try {
			AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("modal:" + nonce, Duration.ofSeconds(30));
			task.setStatus(TaskStatus.MODAL);
			task.setProperty(Constants.TASK_PROPERTY_MODAL_MESSAGE_ID, lock.getProperty(Constants.TASK_PROPERTY_MODAL_MESSAGE_ID));
			task.setProperty(Constants.TASK_PROPERTY_MODAL_CUSTOM_ID, lock.getProperty(Constants.TASK_PROPERTY_MODAL_CUSTOM_ID));
			task.setProperty(Constants.TASK_PROPERTY_MODAL_PROMPT_CUSTOM_ID, lock.getProperty(Constants.TASK_PROPERTY_MODAL_PROMPT_CUSTOM_ID));
			this.taskStoreService.save(task);
			return SubmitResultVO.of(ReturnCode.MODAL, "等待窗口确认", task.getId())
					.setProperty("remix", instance.account().customRemix(botType));
		} catch (TimeoutException e) {
			return SubmitResultVO.fail(ReturnCode.FAILURE, "执行动作(modal)超时");
		}
	}

	@Override
	public SubmitResultVO submitDescribe(DiscordInstance instance, Task task, DataUrl dataUrl) {
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, instance.getInstanceId());
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_CHANNEL_ID, instance.getChannelId());
		return instance.submitTask(task, () -> {
			String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
			Message<String> uploadResult = instance.upload(taskFileName, dataUrl);
			if (uploadResult.getCode() != ReturnCode.SUCCESS) {
				return Message.of(uploadResult.getCode(), uploadResult.getDescription());
			}
			String finalFileName = uploadResult.getResult();
			BotType botType = BotType.valueOf(task.getPropertyGeneric(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE));
			return instance.describe(botType, finalFileName, task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE));
		});
	}

	@Override
	public SubmitResultVO submitBlend(DiscordInstance instance, Task task, List<DataUrl> dataUrls, BlendDimensions dimensions) {
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, instance.getInstanceId());
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_CHANNEL_ID, instance.getChannelId());
		return instance.submitTask(task, () -> {
			List<String> finalFileNames = new ArrayList<>();
			for (DataUrl dataUrl : dataUrls) {
				String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
				Message<String> uploadResult = instance.upload(taskFileName, dataUrl);
				if (uploadResult.getCode() != ReturnCode.SUCCESS) {
					return Message.of(uploadResult.getCode(), uploadResult.getDescription());
				}
				finalFileNames.add(uploadResult.getResult());
			}
			BotType botType = BotType.valueOf(task.getPropertyGeneric(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE));
			return instance.blend(botType, finalFileNames, dimensions, task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE));
		});
	}

	@Override
	public SubmitResultVO submitShorten(DiscordInstance instance, Task task) {
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, instance.getInstanceId());
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_CHANNEL_ID, instance.getChannelId());
		BotType botType = BotType.valueOf(task.getPropertyGeneric(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE));
		return instance.submitTask(task, () -> instance.shorten(botType, task.getPromptEn(), task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE)));
	}

	@Override
	public SubmitResultVO submitModal(Task task) {
		String instanceId = task.getPropertyGeneric(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID);
		DiscordInstance discordInstance = this.discordLoadBalancer.getDiscordInstance(instanceId);
		if (discordInstance == null || !discordInstance.isAlive()) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "账号不可用: " + instanceId);
		}
		String modalMessageId = task.getPropertyGeneric(Constants.TASK_PROPERTY_MODAL_MESSAGE_ID);
		String modalCustomId = task.getPropertyGeneric(Constants.TASK_PROPERTY_MODAL_CUSTOM_ID);
		String modalPromptCustomId = task.getPropertyGeneric(Constants.TASK_PROPERTY_MODAL_PROMPT_CUSTOM_ID);
		if (!CharSequenceUtil.isAllNotBlank(modalMessageId, modalCustomId, modalPromptCustomId)) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "不支持的Modal");
		}
		String nonce = task.getPropertyGeneric(Constants.TASK_PROPERTY_NONCE);
		BotType botType = BotType.valueOf(task.getPropertyGeneric(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE));
		return discordInstance.submitTask(task, () -> discordInstance.modal(botType, modalMessageId, modalCustomId, modalPromptCustomId, task.getPromptEn(), nonce));
	}

	@Override
	public SubmitResultVO submitRegionModal(Task task, String maskBase64) {
		String instanceId = task.getPropertyGeneric(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID);
		DiscordInstance discordInstance = this.discordLoadBalancer.getDiscordInstance(instanceId);
		if (discordInstance == null || !discordInstance.isAlive()) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "账号不可用: " + instanceId);
		}
		String modalCustomId = task.getPropertyGeneric(Constants.TASK_PROPERTY_MODAL_CUSTOM_ID);
		String customId = CharSequenceUtil.replaceFirst(modalCustomId, "MJ::iframe::", "");
		return discordInstance.submitTask(task, () -> discordInstance.regionModal(customId, task.getPromptEn(), maskBase64));
	}

}
