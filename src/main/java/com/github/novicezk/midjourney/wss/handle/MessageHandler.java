package com.github.novicezk.midjourney.wss.handle;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.condition.TaskCondition;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.support.DiscordHelper;
import com.github.novicezk.midjourney.support.MessageButton;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public abstract class MessageHandler {
	@Autowired
	protected DiscordLoadBalancer discordLoadBalancer;
	@Autowired
	protected DiscordHelper discordHelper;

	public abstract void handle(DiscordInstance instance, MessageType messageType, DataObject message);

	public int order() {
		return 100;
	}

	protected String getMessageContent(DataObject message) {
		return message.getString("content", "");
	}

	protected String getMessageNonce(DataObject message) {
		return message.getString("nonce", "");
	}

	protected String getInteractionName(DataObject message) {
		Optional<DataObject> interaction = message.optObject("interaction");
		return interaction.map(dataObject -> dataObject.getString("name", "")).orElse("");
	}

	protected String getReferenceMessageId(DataObject message) {
		Optional<DataObject> reference = message.optObject("message_reference");
		return reference.map(dataObject -> dataObject.getString("message_id", "")).orElse("");
	}

	@Deprecated
	protected DiscordInstance getInstanceByMessage0(DataObject message) {
		String channelId = message.getString("channel_id", "");
		return this.discordLoadBalancer.getDiscordInstance(channelId);
	}

	protected void withCancelComponent(Task task, DataObject message) {
		DataArray components = message.optArray("components").orElse(DataArray.empty());
		if (components.isEmpty()) {
			return;
		}
		for (int i = 0; i < components.length(); i++) {
			DataArray array = components.getObject(i).optArray("components").orElse(DataArray.empty());
			for (int j = 0; j < array.length(); j++) {
				DataObject buttonObj = array.getObject(j);
				String customId = buttonObj.getString("custom_id", "");
				if (CharSequenceUtil.isBlank(customId) || !CharSequenceUtil.startWith(customId, "MJ::CancelJob")) {
					continue;
				}
				JSONObject cancelButton = new JSONObject().put("customId", customId)
						.put("type", buttonObj.getInt("type", 2))
						.put("flags", message.getInt("flags", 0))
						.put("messageId", message.get("id"));
				task.setProperty("cancelComponent", cancelButton.toString());
			}
		}
	}

	protected void findAndFinishImageTask(DiscordInstance instance, TaskCondition condition, String finalPrompt, DataObject message) {
		String imageUrl = getImageUrl(message);
		String messageHash = this.discordHelper.getMessageHash(imageUrl);
		condition.setMessageHash(messageHash);
		Task task = instance.findRunningTask(condition)
				.findFirst().orElseGet(() -> {
					condition.setMessageHash(null);
					return instance.findRunningTask(condition)
							.filter(t -> t.getStartTime() != null)
							.min(Comparator.comparing(Task::getStartTime))
							.orElse(null);
				});
		if (task == null) {
			return;
		}
		message.put(Constants.MJ_MESSAGE_HANDLED, true);
		task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, finalPrompt);
		task.setProperty(Constants.TASK_PROPERTY_MESSAGE_HASH, messageHash);
		task.setImageUrl(imageUrl);
		finishTask(task, message);
		task.awake();
	}

	protected void finishTask(Task task, DataObject message) {
		task.setProperty(Constants.TASK_PROPERTY_MESSAGE_ID, message.getString("id"));
		task.setProperty(Constants.TASK_PROPERTY_FLAGS, message.getInt("flags", 0));
		String content = getMessageContent(message);
		if (CharSequenceUtil.isNotBlank(content)) {
			task.setProperty(Constants.TASK_PROPERTY_MESSAGE_CONTENT, content);
		}
		DataArray components = message.optArray("components").orElse(DataArray.empty());
		List<MessageButton> buttons = new ArrayList<>();
		for (int i = 0; i < components.length(); i++) {
			DataArray array = components.getObject(i).optArray("components").orElse(DataArray.empty());
			for (int j = 0; j < array.length(); j++) {
				MessageButton button = convertButton(array.getObject(j));
				if (button != null) {
					buttons.add(button);
				}
			}
		}
		task.setButtons(buttons);
		task.success();
	}

	protected MessageButton convertButton(DataObject buttonObj) {
		if (!buttonObj.hasKey("custom_id")) {
			return null;
		}
		MessageButton button = new MessageButton();
		button.setCustomId(buttonObj.getString("custom_id"));
		button.setType(buttonObj.getInt("type", 2));
		button.setStyle(buttonObj.getInt("style", 2));
		button.setLabel(buttonObj.getString("label", ""));
		if (buttonObj.hasKey("emoji")) {
			button.setEmoji(buttonObj.getObject("emoji").getString("name"));
		}
		return button;
	}

	protected boolean hasImage(DataObject message) {
		DataArray attachments = message.optArray("attachments").orElse(DataArray.empty());
		return !attachments.isEmpty();
	}

	protected String getImageUrl(DataObject message) {
		DataArray attachments = message.getArray("attachments");
		if (!attachments.isEmpty()) {
			String imageUrl = attachments.getObject(0).getString("url");
			return replaceCdnUrl(imageUrl);
		}
		return null;
	}

	protected String replaceCdnUrl(String imageUrl) {
		if (CharSequenceUtil.isBlank(imageUrl)) {
			return imageUrl;
		}
		String cdn = this.discordHelper.getCdn();
		if (CharSequenceUtil.startWith(imageUrl, cdn)) {
			return imageUrl;
		}
		return CharSequenceUtil.replaceFirst(imageUrl, DiscordHelper.DISCORD_CDN_URL, cdn);
	}

}
