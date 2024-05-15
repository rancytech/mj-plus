package com.github.novicezk.midjourney.controller;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.dto.AccountFilter;
import com.github.novicezk.midjourney.dto.BaseSubmitDTO;
import com.github.novicezk.midjourney.dto.SubmitActionDTO;
import com.github.novicezk.midjourney.dto.SubmitBlendDTO;
import com.github.novicezk.midjourney.dto.SubmitChangeDTO;
import com.github.novicezk.midjourney.dto.SubmitDescribeDTO;
import com.github.novicezk.midjourney.dto.SubmitImagineDTO;
import com.github.novicezk.midjourney.dto.SubmitModalDTO;
import com.github.novicezk.midjourney.dto.SubmitShortenDTO;
import com.github.novicezk.midjourney.dto.SubmitSimpleChangeDTO;
import com.github.novicezk.midjourney.dto.UploadImagesDTO;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.enums.TranslateWay;
import com.github.novicezk.midjourney.exception.BannedPromptException;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.result.SubmitResultVO;
import com.github.novicezk.midjourney.service.TaskService;
import com.github.novicezk.midjourney.service.TranslateService;
import com.github.novicezk.midjourney.store.TaskStoreService;
import com.github.novicezk.midjourney.support.DiscordInstanceCondition;
import com.github.novicezk.midjourney.support.MessageButton;
import com.github.novicezk.midjourney.util.BannedPromptUtils;
import com.github.novicezk.midjourney.util.ConvertUtils;
import com.github.novicezk.midjourney.util.FileUtils;
import com.github.novicezk.midjourney.util.MimeTypeUtils;
import com.github.novicezk.midjourney.util.SnowFlake;
import com.github.novicezk.midjourney.util.TaskChangeParams;
import eu.maxschuster.dataurl.DataUrl;
import eu.maxschuster.dataurl.DataUrlSerializer;
import eu.maxschuster.dataurl.IDataUrlSerializer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Api(tags = "任务提交")
@RestController
@RequestMapping("/mj/submit")
@RequiredArgsConstructor
public class SubmitController {
	private final TranslateService translateService;
	private final TaskStoreService taskStoreService;
	private final ProxyProperties properties;
	private final TaskService taskService;
	private final DiscordLoadBalancer discordLoadBalancer;

	@ApiOperation(value = "提交Imagine任务")
	@PostMapping("/imagine")
	public SubmitResultVO imagine(@RequestBody SubmitImagineDTO imagineDTO) {
		String prompt = imagineDTO.getPrompt();
		if (CharSequenceUtil.isBlank(prompt)) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "prompt不能为空");
		}
		prompt = prompt.trim();
		Task task = newTask(imagineDTO);
		task.setAction(TaskAction.IMAGINE);
		task.setPrompt(prompt);
		BotType botType = Optional.ofNullable(imagineDTO.getBotType()).orElse(BotType.MID_JOURNEY);
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE, botType.name());
		String promptEn = botType == BotType.MID_JOURNEY ? translatePrompt(prompt) : prompt;
		try {
			BannedPromptUtils.checkBanned(promptEn);
		} catch (BannedPromptException e) {
			return SubmitResultVO.fail(ReturnCode.BANNED_PROMPT, "可能包含敏感词")
					.setProperty("promptEn", promptEn).setProperty("bannedWord", e.getMessage());
		}
		List<String> base64Array = Optional.ofNullable(imagineDTO.getBase64Array()).orElse(new ArrayList<>());
		if (CharSequenceUtil.isNotBlank(imagineDTO.getBase64())) {
			base64Array.add(imagineDTO.getBase64());
		}
		if (new HashSet<>(base64Array).size() < base64Array.size()) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "垫图base64重复");
		}
		List<DataUrl> dataUrls;
		try {
			dataUrls = ConvertUtils.convertBase64Array(base64Array);
			FileUtils.checkFileSizeAndThrows(dataUrls);
		} catch (MalformedURLException e) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64格式错误");
		} catch (ValidateException e) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, e.getMessage());
		}
		task.setPromptEn(promptEn);
		task.setDescription("/imagine " + prompt);
		var instanceCondition = new DiscordInstanceCondition(imagineDTO.getAccountFilter(), botType);
		DiscordInstance instance = this.discordLoadBalancer.chooseInstanceByFilter(instanceCondition);
		if (instance == null) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "无可用的账号实例");
		}
		return this.taskService.submitImagine(instance, task, dataUrls);
	}

	@ApiOperation(value = "提交Describe任务")
	@PostMapping("/describe")
	public SubmitResultVO describe(@RequestBody SubmitDescribeDTO describeDTO) {
		if (CharSequenceUtil.isBlank(describeDTO.getBase64())) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64不能为空");
		}
		IDataUrlSerializer serializer = new DataUrlSerializer();
		DataUrl dataUrl;
		try {
			dataUrl = serializer.unserialize(describeDTO.getBase64());
			FileUtils.checkFileSizeAndThrows(List.of(dataUrl));
		} catch (MalformedURLException e) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64格式错误");
		} catch (ValidateException e) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, e.getMessage());
		}
		Task task = newTask(describeDTO);
		task.setAction(TaskAction.DESCRIBE);
		String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
		task.setDescription("/describe " + taskFileName);
		BotType botType = Optional.ofNullable(describeDTO.getBotType()).orElse(BotType.MID_JOURNEY);
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE, botType.name());
		var instanceCondition = new DiscordInstanceCondition(describeDTO.getAccountFilter(), botType);
		DiscordInstance instance = this.discordLoadBalancer.chooseInstanceByFilter(instanceCondition);
		if (instance == null) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "无可用的账号实例");
		}
		return this.taskService.submitDescribe(instance, task, dataUrl);
	}

	@ApiOperation(value = "提交Blend任务")
	@PostMapping("/blend")
	public SubmitResultVO blend(@RequestBody SubmitBlendDTO blendDTO) {
		List<String> base64Array = blendDTO.getBase64Array();
		if (base64Array == null || base64Array.size() < 2 || base64Array.size() > 5) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64List参数错误");
		}
		if (new HashSet<>(base64Array).size() < base64Array.size()) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "图片base64重复");
		}
		if (blendDTO.getDimensions() == null) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "dimensions参数错误");
		}
		IDataUrlSerializer serializer = new DataUrlSerializer();
		List<DataUrl> dataUrlList = new ArrayList<>();
		try {
			for (String base64 : base64Array) {
				DataUrl dataUrl = serializer.unserialize(base64);
				dataUrlList.add(dataUrl);
			}
			FileUtils.checkFileSizeAndThrows(dataUrlList);
		} catch (MalformedURLException e) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64格式错误");
		} catch (ValidateException e) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, e.getMessage());
		}
		Task task = newTask(blendDTO);
		task.setAction(TaskAction.BLEND);
		task.setDescription("/blend " + task.getId() + " " + dataUrlList.size());
		BotType botType = Optional.ofNullable(blendDTO.getBotType()).orElse(BotType.MID_JOURNEY);
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE, botType.name());
		var instanceCondition = new DiscordInstanceCondition(blendDTO.getAccountFilter(), botType);
		DiscordInstance instance = this.discordLoadBalancer.chooseInstanceByFilter(instanceCondition);
		if (instance == null) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "无可用的账号实例");
		}
		return this.taskService.submitBlend(instance, task, dataUrlList, blendDTO.getDimensions());
	}

	@ApiOperation(value = "提交Shorten任务")
	@PostMapping("/shorten")
	public SubmitResultVO shorten(@RequestBody SubmitShortenDTO shortenDTO) {
		String prompt = shortenDTO.getPrompt();
		if (CharSequenceUtil.isBlank(prompt)) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "prompt不能为空");
		}
		prompt = prompt.trim();
		Task task = newTask(shortenDTO);
		task.setAction(TaskAction.SHORTEN);
		task.setPrompt(prompt);
		BotType botType = Optional.ofNullable(shortenDTO.getBotType()).orElse(BotType.MID_JOURNEY);
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE, botType.name());
		String promptEn = botType == BotType.MID_JOURNEY ? translatePrompt(prompt) : prompt;
		try {
			BannedPromptUtils.checkBanned(promptEn);
		} catch (BannedPromptException e) {
			return SubmitResultVO.fail(ReturnCode.BANNED_PROMPT, "可能包含敏感词")
					.setProperty("promptEn", promptEn).setProperty("bannedWord", e.getMessage());
		}
		task.setPromptEn(promptEn);
		task.setDescription("/shorten " + prompt);
		var instanceCondition = new DiscordInstanceCondition(shortenDTO.getAccountFilter(), botType);
		DiscordInstance instance = this.discordLoadBalancer.chooseInstanceByFilter(instanceCondition);
		if (instance == null) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "无可用的账号实例");
		}
		return this.taskService.submitShorten(instance, task);
	}

	@ApiOperation(value = "执行动作")
	@PostMapping("/action")
	public SubmitResultVO action(@RequestBody SubmitActionDTO actionDTO) {
		if (!CharSequenceUtil.isAllNotBlank(actionDTO.getTaskId(), actionDTO.getCustomId())) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "参数错误");
		}
		Task targetTask = this.taskStoreService.get(actionDTO.getTaskId());
		if (targetTask == null) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "关联任务不存在或已失效");
		}
		if (!TaskStatus.SUCCESS.equals(targetTask.getStatus())) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "关联任务状态错误");
		}
		TaskAction taskAction = ConvertUtils.convertCustomId2Action(targetTask.getAction(), actionDTO.getCustomId());
		if (taskAction == null || CharSequenceUtil.containsAny(actionDTO.getCustomId(), "BOOKMARK", "PicReader::all")) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "不支持的动作");
		}
		MessageButton button = targetTask.getButtons().stream().filter(b -> actionDTO.getCustomId().equals(b.getCustomId())).findFirst().orElse(null);
		if (button == null) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "关联动作不存在");
		}
		button.setStyle(3);
		this.taskStoreService.save(targetTask);
		Task task = newTask(actionDTO);
		task.setAction(taskAction);
		String botType = targetTask.getProperty(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE, String.class, BotType.MID_JOURNEY.name());
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE, botType);
		String finalPrompt = targetTask.getPropertyGeneric(Constants.TASK_PROPERTY_FINAL_PROMPT);
		task.setProperty(Constants.TASK_PROPERTY_PROGRESS_MESSAGE_ID, targetTask.getProperty(Constants.TASK_PROPERTY_MESSAGE_ID));
		// task.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID,
		// targetTask.getProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID));
		String instanceId = targetTask.getProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, String.class);
		String channelId = targetTask.getProperty(Constants.TASK_PROPERTY_DISCORD_CHANNEL_ID, String.class, "");

		DiscordInstance instance;
		if (Boolean.TRUE.equals(actionDTO.getChooseSameChannel())) {
			AccountFilter filter = Optional.ofNullable(actionDTO.getAccountFilter()).orElse(new AccountFilter());
			filter.setChannelId(channelId);
			DiscordInstanceCondition condition = new DiscordInstanceCondition(filter, BotType.valueOf(botType));
			instance = discordLoadBalancer.chooseInstanceByFilter(condition);
		} else {
			instance = discordLoadBalancer.getDiscordInstance(instanceId);
		}
		if (instance == null) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "账号不可用或accountFilter设置错误");
		}

		String messageId = targetTask.getPropertyGeneric(Constants.TASK_PROPERTY_MESSAGE_ID);
		String customId = button.getCustomId();
		if (customId.contains("Job::PicReader::") || customId.contains("Job::PromptAnalyzer::")) {
			// describe, shorten 后选择生图
			String prompt = CharSequenceUtil.subBetween(finalPrompt + "\n\n", button.getEmoji() + " ", "\n\n");
			task.setPrompt(prompt);
			task.setPromptEn(prompt);
			task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, prompt);
		} else if (TaskAction.DESCRIBE.equals(targetTask.getAction()) && customId.contains("Retry")) {
			task.setProperty(Constants.TASK_PROPERTY_PROGRESS_MESSAGE_ID, messageId);
			task.setImageUrl(targetTask.getImageUrl());
		} else {
			task.setPrompt(targetTask.getPrompt());
			task.setPromptEn(targetTask.getPromptEn());
			task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, finalPrompt);
		}
		String targetCustomId = targetTask.getProperty(Constants.TASK_PROPERTY_CUSTOM_ID, String.class, "");
		if (customId.contains("reroll") && (targetCustomId.contains("Inpaint") || Boolean.TRUE.equals(targetTask.getProperty("inpaintReroll")))) {
			task.setProperty("inpaintReroll", true);
		}
		task.setDescription("/up " + targetTask.getId() + " " + button.getEmoji() + button.getLabel());
		int messageFlags = targetTask.getPropertyGeneric(Constants.TASK_PROPERTY_FLAGS);
		SubmitResultVO resultVO = this.taskService.submitAction(instance, task, messageId, messageFlags, button.getType(), customId);
		if (resultVO.getCode() == ReturnCode.MODAL) {
			resultVO.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, task.getProperty(Constants.TASK_PROPERTY_FINAL_PROMPT));
		}
		return resultVO;
	}

	@ApiOperation(value = "提交Modal")
	@PostMapping("/modal")
	public SubmitResultVO modal(@RequestBody SubmitModalDTO modalDTO) {
		Task task = this.taskStoreService.get(modalDTO.getTaskId());
		if (task == null) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "任务不存在或已失效");
		}
		if (!TaskStatus.MODAL.equals(task.getStatus())) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "任务状态错误");
		}
		String botType = task.getProperty(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE, String.class, BotType.MID_JOURNEY.name());
		String prompt = CharSequenceUtil.isBlank(modalDTO.getPrompt()) ? task.getPropertyGeneric(Constants.TASK_PROPERTY_FINAL_PROMPT) : modalDTO.getPrompt();
		String promptEn = BotType.MID_JOURNEY.name().equals(botType) ? translatePrompt(prompt) : prompt;
		try {
			BannedPromptUtils.checkBanned(promptEn);
		} catch (BannedPromptException e) {
			return SubmitResultVO.fail(ReturnCode.BANNED_PROMPT, "可能包含敏感词")
					.setProperty("promptEn", promptEn).setProperty("bannedWord", e.getMessage());
		}
		task.setPrompt(prompt);
		task.setPromptEn(promptEn);
		task.setProperty(Constants.TASK_PROPERTY_NONCE, SnowFlake.INSTANCE.nextId());
		String customId = task.getPropertyGeneric(Constants.TASK_PROPERTY_CUSTOM_ID);
		if (CharSequenceUtil.contains(customId, "Inpaint")) {
			if (CharSequenceUtil.isBlank(modalDTO.getMaskBase64())) {
				return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "蒙版base64不能为空");
			}
			if (CharSequenceUtil.contains(modalDTO.getMaskBase64(), "base64,")) {
				modalDTO.setMaskBase64(CharSequenceUtil.subAfter(modalDTO.getMaskBase64(), "base64,", false));
			}
			return this.taskService.submitRegionModal(task, modalDTO.getMaskBase64());
		}
		return this.taskService.submitModal(task);
	}

	@Deprecated(since = "3.0", forRemoval = true)
	@ApiOperation(value = "绘图变化-simple[替换为执行动作]", hidden = true)
	@PostMapping("/simple-change")
	public SubmitResultVO simpleChange(@RequestBody SubmitSimpleChangeDTO simpleChangeDTO) {
		TaskChangeParams changeParams = ConvertUtils.convertChangeParams(simpleChangeDTO.getContent());
		if (changeParams == null) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "content参数错误");
		}
		SubmitChangeDTO changeDTO = new SubmitChangeDTO();
		changeDTO.setAction(changeParams.getAction());
		changeDTO.setTaskId(changeParams.getId());
		changeDTO.setIndex(changeParams.getIndex());
		changeDTO.setState(simpleChangeDTO.getState());
		changeDTO.setNotifyHook(simpleChangeDTO.getNotifyHook());
		return change(changeDTO);
	}

	@Deprecated(since = "3.0", forRemoval = true)
	@ApiOperation(value = "绘图变化[替换为执行动作]", hidden = true)
	@PostMapping("/change")
	public SubmitResultVO change(@RequestBody SubmitChangeDTO changeDTO) {
		if (CharSequenceUtil.isBlank(changeDTO.getTaskId())) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "taskId不能为空");
		}
		Task targetTask = this.taskStoreService.get(changeDTO.getTaskId());
		if (targetTask == null) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "关联任务不存在或已失效");
		}
		if (!TaskStatus.SUCCESS.equals(targetTask.getStatus())) {
			return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "关联任务状态错误");
		}
		String label = "" + changeDTO.getAction().name().charAt(0) + changeDTO.getIndex();
		MessageButton messageButton = targetTask.getButtons().stream().filter(b -> label.equals(b.getLabel())).findFirst().orElse(null);
		if (messageButton == null) {
			return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "关联动作不存在");
		}
		SubmitActionDTO actionDTO = new SubmitActionDTO();
		actionDTO.setState(changeDTO.getState());
		actionDTO.setNotifyHook(changeDTO.getNotifyHook());
		actionDTO.setTaskId(changeDTO.getTaskId());
		actionDTO.setCustomId(messageButton.getCustomId());
		return action(actionDTO);
	}

	@ApiOperation("上传文件到discord")
	@PostMapping("/upload-discord-images")
	public Message<List<String>> uploadDiscordImages(@RequestBody UploadImagesDTO imagesDTO) {
		if (imagesDTO.getBase64Array() == null || imagesDTO.getBase64Array().isEmpty()) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "base64Array不能为空");
		}

		List<DataUrl> dataUrlList;
		try {
			dataUrlList = ConvertUtils.convertBase64Array(imagesDTO.getBase64Array());
			FileUtils.checkFileSizeAndThrows(dataUrlList);
		} catch (MalformedURLException e) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "base64格式错误");
		} catch (ValidateException e) {
			return Message.of(ReturnCode.VALIDATION_ERROR, e.getMessage());
		}

		DiscordInstance instance = discordLoadBalancer.chooseInstanceByFilter(imagesDTO.getFilter());
		if (imagesDTO.getFilter() == null) {
			instance = discordLoadBalancer.chooseInstance();
		}
		if (instance == null) {
			return Message.of(ReturnCode.NOT_FOUND, "无可用的账号实例");
		}

		List<String> messages = new ArrayList<>();
		for (DataUrl dataUrl : dataUrlList) {
			String fileName = SnowFlake.INSTANCE.nextId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
			Message<String> message = instance.upload(fileName, dataUrl);
			if (message.getCode() != ReturnCode.SUCCESS) {
				return Message.of(message.getCode(), message.getDescription());
			}

			messages.add(message.getResult());
		}

		return instance.sendImageMessages("upload images", messages);
	}

	private Task newTask(BaseSubmitDTO base) {
		Task task = new Task();
		task.setId(System.currentTimeMillis() + RandomUtil.randomNumbers(3));
		task.setSubmitTime(System.currentTimeMillis());
		task.setState(base.getState());
		String notifyHook = CharSequenceUtil.isBlank(base.getNotifyHook()) ? this.properties.getNotifyHook() : base.getNotifyHook();
		task.setProperty(Constants.TASK_PROPERTY_NOTIFY_HOOK, notifyHook);
		task.setProperty(Constants.TASK_PROPERTY_NONCE, SnowFlake.INSTANCE.nextId());
		return task;
	}

	private String translatePrompt(String prompt) {
		if (TranslateWay.NULL.equals(this.properties.getTranslateWay()) || CharSequenceUtil.isBlank(prompt)) {
			return prompt;
		}
		List<String> imageUrls = new ArrayList<>();
		Matcher imageMatcher = Pattern.compile("https?://[a-z0-9-_:@&?=+,.!/~*'%$]+\\x20+", Pattern.CASE_INSENSITIVE).matcher(prompt);
		while (imageMatcher.find()) {
			imageUrls.add(imageMatcher.group(0));
		}
		String paramStr = "";
		Matcher paramMatcher = Pattern.compile("\\x20+-{1,2}[a-z]+.*$", Pattern.CASE_INSENSITIVE).matcher(prompt);
		if (paramMatcher.find()) {
			paramStr = paramMatcher.group(0);
		}
		String imageStr = CharSequenceUtil.join("", imageUrls);
		String text = prompt.substring(imageStr.length(), prompt.length() - paramStr.length());
		if (CharSequenceUtil.isNotBlank(text)) {
			text = this.translateService.translateToEnglish(text).trim();
		}
		if (CharSequenceUtil.isNotBlank(paramStr)) {
			Matcher paramNomatcher = Pattern.compile("-{1,2}no\\s+(.*?)(?=-|$)").matcher(paramStr);
			if (paramNomatcher.find()) {
				String paramNoStr = paramNomatcher.group(1).trim();
				String paramNoStrEn = this.translateService.translateToEnglish(paramNoStr).trim();
				paramStr = paramNomatcher.replaceFirst("--no " + paramNoStrEn + " ");
			}
		}
		return imageStr + text + paramStr;
	}


}
