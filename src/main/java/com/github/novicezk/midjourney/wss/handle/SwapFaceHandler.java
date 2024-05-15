package com.github.novicezk.midjourney.wss.handle;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * face swapid/saveid消息处理.
 * command sent
 * idname test12 created
 * idname test12 updated
 * idname test12 deleted
 * face detection failed
 * Oops! ...
 * Picsi.Ai ver.0.6.2.u20231105 [SaveID: 'test12'] [@novicezk] (3/50 credits used)
 */
@Slf4j
@Component
public class SwapFaceHandler extends MessageHandler {
	private final TimedCache<String, List<MessageData>> errorMessageCache = CacheUtil.newTimedCache(Duration.ofDays(1).toMillis());

	public List<MessageData> getErrorMessageList(String channelId) {
		return this.errorMessageCache.get(channelId);
	}

	@Override
	public int order() {
		return 0;
	}

	@Override
	public void handle(DiscordInstance instance, MessageType messageType, DataObject message) {
		String authorId = message.optObject("author").map(a -> a.getString("id", "")).orElse(null);
		if (!BotType.INSIGHT_FACE.getValue().equals(authorId)) {
			return;
		}
		message.put(Constants.MJ_MESSAGE_HANDLED, true);
		String content = getMessageContent(message);
		if (content.startsWith("idname ") && CharSequenceUtil.endWithAny(content, " created", " updated")) {
			String id = CharSequenceUtil.split(content, " ").get(1);
			AsyncLockUtils.LockObject lock = AsyncLockUtils.getLock("saveid:" + id);
			if (lock != null) {
				lock.awake();
			}
			return;
		}
		DataArray attachments = message.getArray("attachments");
		if (!attachments.isEmpty()) {
			DataObject object = attachments.getObject(0);
			String filename = object.getString("filename");
			String swapId;
			if (filename.contains("_")) {
				swapId = CharSequenceUtil.subBefore(filename, "_", false);
			} else {
				swapId = CharSequenceUtil.subBefore(filename, ".", false);
			}
			AsyncLockUtils.LockObject lock = AsyncLockUtils.getLock("swapid:" + swapId);
			if (lock != null) {
				String imageUrl = object.getString("url");
				lock.setProperty("imageUrl", replaceCdnUrl(imageUrl));
				lock.setProperty("messageContent", content);
				lock.setProperty("messageId", message.getString("id", ""));
				lock.awake();
			}
			return;
		}
		if (isError(content)) {
			String channelId = message.getString("channel_id", "");
			synchronized (this.errorMessageCache) {
				List<MessageData> errorList = this.errorMessageCache.get(channelId);
				if (errorList == null) {
					errorList = new ArrayList<>();
					this.errorMessageCache.put(channelId, errorList);
				}
				errorList.add(new MessageData(channelId, content, System.currentTimeMillis()));
			}
		}
	}

	private boolean isError(String content) {
		if (content.startsWith("idname ") && CharSequenceUtil.endWithAny(content, " created", " updated", " deleted")) {
			return false;
		}
		if (CharSequenceUtil.containsAll(content, " [SaveID: '", " credits used)")) {
			return false;
		}
		if (CharSequenceUtil.equals(content, "command sent")) {
			return false;
		}
		return true;
	}

	public record MessageData(String channelId, String content, long time) {
	}

}
