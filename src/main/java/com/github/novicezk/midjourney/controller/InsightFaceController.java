package com.github.novicezk.midjourney.controller;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.dto.SwapFaceDTO;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.service.NotifyService;
import com.github.novicezk.midjourney.store.TaskStoreService;
import com.github.novicezk.midjourney.support.DiscordInstanceCondition;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import com.github.novicezk.midjourney.util.FileUtils;
import com.github.novicezk.midjourney.util.MimeTypeUtils;
import com.github.novicezk.midjourney.util.SnowFlake;
import com.github.novicezk.midjourney.wss.handle.SwapFaceHandler;
import eu.maxschuster.dataurl.DataUrl;
import eu.maxschuster.dataurl.DataUrlSerializer;
import eu.maxschuster.dataurl.IDataUrlSerializer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;


@Slf4j
@Api(tags = "InsightFace任务提交")
@RestController
@RequestMapping("/mj/insight-face")
@RequiredArgsConstructor
public class InsightFaceController {
	private final ProxyProperties properties;
	private final TaskStoreService taskStoreService;
	private final NotifyService notifyService;
	private final DiscordLoadBalancer discordLoadBalancer;
	private final SwapFaceHandler swapFaceHandler;

	@ApiOperation(value = "提交swap_face任务")
	@PostMapping("/swap")
	public Message<String> swap(@RequestBody SwapFaceDTO swapFaceDTO) {
		if (!CharSequenceUtil.isAllNotBlank(swapFaceDTO.getSourceBase64(), swapFaceDTO.getTargetBase64())) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "sourceBase64、targetBase64不能为空");
		}
		DiscordInstanceCondition condition = new DiscordInstanceCondition(BotType.INSIGHT_FACE);
		if (swapFaceDTO.getAccountFilter() != null) {
			condition.setInstanceId(swapFaceDTO.getAccountFilter().getInstanceId());
		}
		DiscordInstance discordInstance = this.discordLoadBalancer.chooseInstanceByFilter(condition);
		if (discordInstance == null) {
			return Message.of(ReturnCode.NOT_FOUND, "无可用的InsightFace账号实例");
		}
		IDataUrlSerializer serializer = new DataUrlSerializer();
		DataUrl sourceDataUrl;
		DataUrl targetDataUrl;
		try {
			sourceDataUrl = serializer.unserialize(swapFaceDTO.getSourceBase64());
			targetDataUrl = serializer.unserialize(swapFaceDTO.getTargetBase64());
			FileUtils.checkFileSizeAndThrows(List.of(sourceDataUrl, targetDataUrl));
		} catch (MalformedURLException e) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "base64格式错误");
		} catch (ValidateException e) {
			return Message.of(ReturnCode.VALIDATION_ERROR, e.getMessage());
		}
		Task task = new Task();
		task.setId(System.currentTimeMillis() + RandomUtil.randomNumbers(3));
		task.setSubmitTime(System.currentTimeMillis());
		task.setState(swapFaceDTO.getState());
		task.setAction(TaskAction.SWAP_FACE);
		String notifyHook = CharSequenceUtil.isBlank(swapFaceDTO.getNotifyHook()) ? this.properties.getNotifyHook() : swapFaceDTO.getNotifyHook();
		task.setProperty(Constants.TASK_PROPERTY_NOTIFY_HOOK, notifyHook);
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE, BotType.INSIGHT_FACE.name());
		task.setProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID, discordInstance.getInstanceId());
		String sourceName = "source" + RandomUtil.randomNumbers(4);
		String sourceFileName = sourceName + "." + MimeTypeUtils.guessFileSuffix(sourceDataUrl.getMimeType());
		Message<String> uploadResult = discordInstance.upload(sourceFileName, sourceDataUrl);
		if (uploadResult.getCode() != ReturnCode.SUCCESS) {
			return Message.of(uploadResult.getCode(), uploadResult.getDescription());
		}
		String sourceFinalFileName = uploadResult.getResult();
		Message<Void> result = discordInstance.saveInsightFace(sourceName, sourceFinalFileName, SnowFlake.INSTANCE.nextId());
		if (result.getCode() != ReturnCode.SUCCESS) {
			return Message.of(result.getCode(), result.getDescription());
		}
		long waitStartTime = System.currentTimeMillis();
		try {
			AsyncLockUtils.waitForLock("saveid:" + sourceName, Duration.ofSeconds(30));
		} catch (TimeoutException e) {
			List<SwapFaceHandler.MessageData> errorMessageList = this.swapFaceHandler.getErrorMessageList(discordInstance.getInstanceId());
			return Message.of(ReturnCode.FAILURE, convertErrorList(errorMessageList, waitStartTime, "saveid操作失败"));
		}
		String targetName = IdUtil.fastSimpleUUID();
		String targetFileName = targetName + "." + MimeTypeUtils.guessFileSuffix(targetDataUrl.getMimeType());
		uploadResult = discordInstance.upload(targetFileName, targetDataUrl);
		if (uploadResult.getCode() != ReturnCode.SUCCESS) {
			discordInstance.delInsightFace(sourceName, SnowFlake.INSTANCE.nextId());
			return Message.of(uploadResult.getCode(), uploadResult.getDescription());
		}
		task.setStatus(TaskStatus.SUBMITTED);
		task.setDescription("/swap " + sourceName + " " + targetFileName);
		String targetFinalFileName = uploadResult.getResult();
		ThreadUtil.execute(() -> {
			try {
				executeSwapAndWait(discordInstance, task, sourceName, targetName, targetFinalFileName);
			} finally {
				discordInstance.delInsightFace(sourceName, SnowFlake.INSTANCE.nextId());
			}
		});
		this.taskStoreService.save(task);
		return Message.success(task.getId());
	}

	private void executeSwapAndWait(DiscordInstance instance, Task task, String sourceName, String targetName, String targetFinalFileName) {
		Message<Void> result = instance.swapInsightFace(sourceName, targetFinalFileName, SnowFlake.INSTANCE.nextId());
		task.setStartTime(System.currentTimeMillis());
		if (result.getCode() != ReturnCode.SUCCESS) {
			task.fail(result.getDescription());
			saveAndNotify(task);
			return;
		}
		task.setStatus(TaskStatus.IN_PROGRESS);
		task.setProgress("0%");
		asyncSaveAndNotify(task);
		try {
			AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("swapid:" + targetName, Duration.ofMinutes(instance.account().getTimeoutMinutes()));
			task.setImageUrl(lock.getProperty("imageUrl", String.class));
			task.setProperty(Constants.TASK_PROPERTY_MESSAGE_CONTENT, lock.getProperty("messageContent", String.class));
			task.setProperty(Constants.TASK_PROPERTY_MESSAGE_ID, lock.getProperty("messageId", String.class));
			task.success();
			saveAndNotify(task);
		} catch (TimeoutException e) {
			List<SwapFaceHandler.MessageData> errorMessageList = this.swapFaceHandler.getErrorMessageList(instance.getInstanceId());
			String reason = convertErrorList(errorMessageList, task.getStartTime(), "swapid操作失败");
			task.fail(reason);
			saveAndNotify(task);
		}
	}

	private String convertErrorList(List<SwapFaceHandler.MessageData> errorList, long waitStartTime, String defaultMsg) {
		if (errorList == null) {
			return defaultMsg;
		}
		return errorList.stream().filter(m -> m.time() > waitStartTime)
				.map(SwapFaceHandler.MessageData::content)
				.reduce((a, b) -> a + "\n" + b).orElse(defaultMsg);
	}

	private void asyncSaveAndNotify(Task task) {
		ThreadUtil.execute(() -> saveAndNotify(task));
	}

	private void saveAndNotify(Task task) {
		this.taskStoreService.save(task);
		this.notifyService.notifyTaskChange(task);
	}

}
