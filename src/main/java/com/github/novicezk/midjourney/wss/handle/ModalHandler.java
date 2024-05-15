package com.github.novicezk.midjourney.wss.handle;


import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import com.github.novicezk.midjourney.util.SnowFlake;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

/**
 * 弹框消息处理.
 * IFRAME_MODAL_CREATE 或 MODAL_CREATE
 */
@Component
public class ModalHandler extends MessageHandler {

	@Override
	public int order() {
		return 0;
	}

	@Override
	public void handle(DiscordInstance instance, MessageType messageType, DataObject message) {
		if (!MessageType.IFRAME_MODAL_CREATE.equals(messageType) && !MessageType.MODAL_CREATE.equals(messageType)) {
			return;
		}
		message.put(Constants.MJ_MESSAGE_HANDLED, true);
		String nonce = message.getString("nonce", "");
		if (MessageType.MODAL_CREATE.equals(messageType)) {
			Task task = instance.getRunningTaskByNonce(nonce);
			if (task != null && !Boolean.TRUE.equals(task.getPropertyGeneric(Constants.TASK_PROPERTY_NEED_MODAL))) {
				handleRemixAutoSubmit(instance, task, message);
				return;
			}
		}
		AsyncLockUtils.LockObject lock = AsyncLockUtils.getLock("modal:" + nonce);
		if (lock == null) {
			return;
		}
		String modalCustomId = message.getString("custom_id", "");
		lock.setProperty(Constants.TASK_PROPERTY_MODAL_MESSAGE_ID, message.getString("id"));
		lock.setProperty(Constants.TASK_PROPERTY_MODAL_CUSTOM_ID, modalCustomId);
		lock.setProperty(Constants.TASK_PROPERTY_MODAL_PROMPT_CUSTOM_ID, getModalPromptCustomId(message));
		lock.awake();
	}

	private void handleRemixAutoSubmit(DiscordInstance instance, Task task, DataObject message) {
		if (!instance.isAlive()) {
			task.fail("账号不可用: " + instance.getInstanceId());
			task.awake();
			return;
		}
		String modalMessageId = message.getString("id");
		String modalCustomId = message.getString("custom_id", "");
		String modalPromptCustomId = getModalPromptCustomId(message);
		String nonce = SnowFlake.INSTANCE.nextId();
		task.setProperty(Constants.TASK_PROPERTY_NONCE, nonce);
		BotType botType = BotType.valueOf(task.getPropertyGeneric(Constants.TASK_PROPERTY_DISCORD_BOT_TYPE));
		Message<Void> result = instance.modal(botType, modalMessageId, modalCustomId, modalPromptCustomId, task.getPromptEn(), nonce);
		if (result.getCode() != ReturnCode.SUCCESS) {
			task.fail(result.getDescription());
			task.awake();
		}
	}

	private String getModalPromptCustomId(DataObject message) {
		DataArray components = message.optArray("components").orElse(DataArray.empty());
		if (components.isEmpty()) {
			return "";
		}
		DataArray array = components.getObject(0).optArray("components").orElse(DataArray.empty());
		return array.isEmpty() ? "" : array.getObject(0).getString("custom_id", "");
	}

}
