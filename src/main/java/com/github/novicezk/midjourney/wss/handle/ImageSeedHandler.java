package com.github.novicezk.midjourney.wss.handle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.util.AsyncLockUtils;

import net.dv8tion.jda.api.utils.data.DataObject;

/**
 * seed消息处理. 完成(create): **Cat**\n**Job ID**:
 * c75a92bf-4fb8-4393-aece-64ed25f28197\n**seed** 407905718
 */
@Component
public class ImageSeedHandler extends MessageHandler {
	private static final String CONTENT_REGEX = "\\*\\*Job ID\\*\\*: (.*?)\n\\*\\*seed\\*\\* (\\d+)";

	@Override
	public int order() {
		return 1;
	}

	@Override
	public void handle(DiscordInstance instance, MessageType messageType, DataObject message) {
		String content = getMessageContent(message);
		Matcher matcher = Pattern.compile(CONTENT_REGEX).matcher(content);
		if (!matcher.find()) {
			return;
		}
		message.put(Constants.MJ_MESSAGE_HANDLED, true);
		String messageHash = matcher.group(1);
		String seed = matcher.group(2);
		AsyncLockUtils.LockObject lock = AsyncLockUtils.getLock("seed:" + messageHash);
		if (lock != null) {
			lock.setProperty("seed", seed);
			lock.awake();
		}
	}

}
