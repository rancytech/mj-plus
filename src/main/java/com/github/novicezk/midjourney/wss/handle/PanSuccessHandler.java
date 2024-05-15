package com.github.novicezk.midjourney.wss.handle;

import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.condition.TaskCondition;
import com.github.novicezk.midjourney.util.ContentParseData;
import com.github.novicezk.midjourney.util.ConvertUtils;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * pan消息处理.
 * 完成(create): **cat** - Pan Up by <@1012983546824114217> (relaxed)
 * 完成(create): **cat** - Pan Down by <@1012983546824114217> (relaxed)
 * 完成(create): **cat** - Pan Left by <@1012983546824114217> (relaxed)
 * 完成(create): **cat** - Pan Right by <@1012983546824114217> (relaxed)
 */
@Component
public class PanSuccessHandler extends MessageHandler {
	private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - Pan .*? by <@\\d+> \\((.*?)\\)";

	@Override
	public void handle(DiscordInstance instance, MessageType messageType, DataObject message) {
		String content = getMessageContent(message);
		ContentParseData parseData = ConvertUtils.parseContent(content, CONTENT_REGEX);
		if (MessageType.CREATE.equals(messageType) && parseData != null && hasImage(message)) {
			TaskCondition condition = new TaskCondition()
					.setActionSet(Set.of(TaskAction.PAN))
					.setFinalPrompt(parseData.getPrompt());
			findAndFinishImageTask(instance, condition, parseData.getPrompt(), message);
		}
	}

}
