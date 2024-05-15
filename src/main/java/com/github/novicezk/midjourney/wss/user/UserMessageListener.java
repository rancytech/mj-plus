package com.github.novicezk.midjourney.wss.user;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.wss.handle.MessageHandler;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Slf4j
@RequiredArgsConstructor
public class UserMessageListener {
	private final List<MessageHandler> messageHandlers;
	private DiscordInstance instance;

	public void onMessage(DataObject raw) {
		MessageType messageType = MessageType.of(raw.getString("t"));
		if (messageType == null || MessageType.DELETE == messageType) {
			return;
		}
		DataObject data = raw.getObject("d");
		if (ignoreAndLogMessage(data, messageType)) {
			return;
		}
		ThreadUtil.sleep(300);
		for (MessageHandler messageHandler : this.messageHandlers) {
			if (data.getBoolean(Constants.MJ_MESSAGE_HANDLED, false)) {
				return;
			}
			messageHandler.handle(instance, messageType, data);
		}
	}

	private boolean ignoreAndLogMessage(DataObject data, MessageType messageType) {
		String channelId = data.getString("channel_id", "");
		if (CharSequenceUtil.isBlank(channelId)) {
			return true;
		}

		DiscordAccount account = this.instance.account();
		if (!CharSequenceUtil.equalsAny(channelId, account.getChannelId(), account.getMjBotChannelId(), account.getNijiBotChannelId())) {
			return true;
		}

		String userId = account.getUserId();
		if (!CharSequenceUtil.equals(getUserId(data, messageType, userId), userId)) {
			return true;
		}

		String authorName = data.optObject("author").map(a -> a.getString("username")).orElse("System");
		log.debug("[{}] {} - {} - {}: {}", userId, account.getDisplay(), messageType.name(), authorName, data.opt("content").orElse(""));
		log.trace("[{}] {} - {}", userId, account.getDisplay(), data);
		return false;
	}

	private String getUserId(DataObject data, MessageType messageType, String defaultUserId) {
		Optional<DataObject> interaction = data.optObject("interaction");
		if (interaction.isPresent()) {
			return interaction.get().optObject("user").map(i -> i.getString("id")).orElse(defaultUserId);
		}

		if (data.hasKey("content") && MessageType.CREATE == messageType) {
			String content = data.getString("content");
			Matcher matcher = Pattern.compile(" <@(\\d+)>").matcher(content);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}

		return defaultUserId;
	}
}
