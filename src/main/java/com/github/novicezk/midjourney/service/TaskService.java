package com.github.novicezk.midjourney.service;

import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.enums.BlendDimensions;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.result.SubmitResultVO;
import eu.maxschuster.dataurl.DataUrl;

import java.util.List;

public interface TaskService {

	SubmitResultVO submitImagine(DiscordInstance instance, Task task, List<DataUrl> dataUrls);

	SubmitResultVO submitAction(DiscordInstance instance, Task task, String targetMessageId, int messageFlags, int componentType, String customId);

	SubmitResultVO submitDescribe(DiscordInstance instance, Task task, DataUrl dataUrl);

	SubmitResultVO submitBlend(DiscordInstance instance, Task task, List<DataUrl> dataUrls, BlendDimensions dimensions);

	SubmitResultVO submitShorten(DiscordInstance instance, Task task);

	SubmitResultVO submitModal(Task task);

	SubmitResultVO submitRegionModal(Task task, String maskBase64);

}
