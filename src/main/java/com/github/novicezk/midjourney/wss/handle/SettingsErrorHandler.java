package com.github.novicezk.midjourney.wss.handle;


import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

/**
 * settings错误消息处理.
 */
@Component
public class SettingsErrorHandler extends MessageHandler {

	@Override
	public int order() {
		return 0;
	}

	@Override
	public void handle(DiscordInstance instance, MessageType messageType, DataObject message) {
		String content = getMessageContent(message);
		if (!MessageType.CREATE.equals(messageType) || CharSequenceUtil.isBlank(content) || CharSequenceUtil.startWith(content, "Done")) {
			return;
		}
		String applicationId = message.getString("application_id", "");
		DiscordAccount account = instance.account();
		String referenceMessageId = getReferenceMessageId(message);
		if (!CharSequenceUtil.equals(referenceMessageId, account.getPropertyGeneric(applicationId + ":messageId"))) {
			return;
		}
		message.put(Constants.MJ_MESSAGE_HANDLED, true);
		if (CharSequenceUtil.startWith(content, "Stealth generation")) {
			// Public Mode 特殊处理
			return;
		}
		AsyncLockUtils.LockObject lock = AsyncLockUtils.getLock("settings-" + applicationId + ":" + account.getId());
		if (lock != null) {
			lock.setProperty("error", content);
			lock.awake();
		}
	}

}
