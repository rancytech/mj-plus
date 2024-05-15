package com.github.novicezk.midjourney.wss.handle;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.condition.TaskCondition;
import com.github.novicezk.midjourney.util.ContentParseData;
import com.github.novicezk.midjourney.util.ConvertUtils;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Vary(Region)消息处理.
 * 开始(create): **cat** - <@1012983546824114217> (Waiting to start)
 */
@Component
public class RegionStartHandler extends MessageHandler {
	private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - <@\\d+> \\((.*?)\\)";

	@Override
	public int order() {
		return 89;
	}

	@Override
	public void handle(DiscordInstance instance, MessageType messageType, DataObject message) {
		String content = getMessageContent(message);
		ContentParseData parseData = ConvertUtils.parseContent(content, CONTENT_REGEX);
		String referenceMessageId = getReferenceMessageId(message);
		if (MessageType.CREATE.equals(messageType) && parseData != null && CharSequenceUtil.isNotBlank(referenceMessageId)) {
			TaskCondition condition = new TaskCondition()
					.setActionSet(Set.of(TaskAction.VARIATION))
					.setDescriptionContains("Region")
					.setProgressMessageId(referenceMessageId);
			// 任务开始
			Task task = instance.findRunningTask(condition).findFirst().orElse(null);
			if (task == null) {
				return;
			}
			message.put(Constants.MJ_MESSAGE_HANDLED, true);
			task.setProperty(Constants.TASK_PROPERTY_PROGRESS_MESSAGE_ID, message.getString("id"));
			task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, parseData.getPrompt());
			task.setProperty(Constants.TASK_PROPERTY_MESSAGE_CONTENT, content);
			task.setStatus(TaskStatus.IN_PROGRESS);
			withCancelComponent(task, message);
			task.awake();
		}
	}


}
