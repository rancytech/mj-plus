package com.github.novicezk.midjourney.wss.handle;


import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.enums.AccountMode;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.enums.StylizeLevel;
import com.github.novicezk.midjourney.enums.VariationLevel;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.store.AccountStoreService;
import com.github.novicezk.midjourney.support.MessageButton;
import com.github.novicezk.midjourney.support.MessageSelector;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * settings消息处理.
 */
@Component
public class SettingsHandler extends MessageHandler {
	@Autowired
	private AccountStoreService accountStoreService;

	@Override
	public int order() {
		return 1;
	}

	@Override
	public void handle(DiscordInstance instance, MessageType messageType, DataObject message) {
		String interactionName = getInteractionName(message);
		DataArray components = message.optArray("components").orElse(DataArray.empty());
		if (!MessageType.UPDATE.equals(messageType) || !"settings".equals(interactionName) || components.isEmpty()) {
			return;
		}
		message.put(Constants.MJ_MESSAGE_HANDLED, true);
		String applicationId = message.getString("application_id", "");
		DiscordAccount account = instance.account();
		account.setProperty(applicationId + ":messageId", message.getString("id"));
		account.setProperty(applicationId + ":flags", message.getInt("flags", 0));

		List<MessageButton> messageButtons = new ArrayList<>();
		for (int i = 0; i < components.length(); i++) {
			DataArray array = components.getObject(i).optArray("components").orElse(DataArray.empty());
			for (int j = 0; j < array.length(); j++) {
				DataObject buttonObj = array.getObject(j);
				if (!buttonObj.hasKey("custom_id")) {
					continue;
				}
				String customId = buttonObj.getString("custom_id", "");
				String label = buttonObj.getString("label", "");
				int type = buttonObj.getInt("type", 2);
				int style = buttonObj.getInt("style", 2);
				if (customId.contains("VersionSelector")) {
					MessageSelector selector = new MessageSelector();
					selector.setCustomId(customId);
					selector.setType(type);
					selector.setPlaceholder(buttonObj.getString("placeholder", ""));
					List<MessageSelector.Option> selectorOptions = new ArrayList<>();
					selector.setOptions(selectorOptions);
					if (BotType.MID_JOURNEY.getValue().equals(applicationId)) {
						account.setVersionSelector(selector);
					} else {
						continue;
					}
					DataArray buttonOptions = buttonObj.getArray("options");
					for (int k = 0; k < buttonOptions.length(); k++) {
						DataObject optionObj = buttonOptions.getObject(k);
						MessageSelector.Option option = new MessageSelector.Option();
						if (optionObj.hasKey("default") && optionObj.getBoolean("default", false)) {
							account.setVersion(optionObj.getString("value"));
							option.setSelected(true);
						}
						option.setLabel(optionObj.getString("label", ""));
						option.setValue(optionObj.getString("value", ""));
						option.setDescription(optionObj.getString("description", ""));
						if (optionObj.hasKey("emoji")) {
							option.setEmoji(optionObj.getObject("emoji").getString("name"));
						}
						selectorOptions.add(option);
					}
				} else {
					MessageButton button = new MessageButton();
					button.setCustomId(customId);
					button.setStyle(style);
					button.setLabel(label);
					button.setType(type);
					if (buttonObj.hasKey("emoji")) {
						button.setEmoji(buttonObj.getObject("emoji").getString("name"));
					}
					messageButtons.add(button);
				}
				if (BotType.NIJI_JOURNEY.getValue().equals(applicationId)) {
					if (customId.contains("RemixMode")) {
						account.setNijiRemix(style == 3);
					} else if (customId.contains("TurboMode") && style == 3) {
						account.setNijiMode(AccountMode.TURBO);
					} else if (customId.contains("FastMode") && style == 3) {
						account.setNijiMode(AccountMode.FAST);
					} else if (customId.contains("RelaxMode") && style == 3) {
						account.setNijiMode(AccountMode.RELAX);
					}
				} else {
					if (customId.contains("Style::raw")) {
						account.setRaw(style == 3);
					} else if (customId.contains("Stylization") && style == 3) {
						String stylize = CharSequenceUtil.subAfter(label, "Stylize ", false);
						account.setStylize(StylizeLevel.fromValue(stylize));
					} else if (customId.contains("PrivateMode::off")) {
						account.setPublicMode(style == 3);
					} else if (customId.contains("RemixMode")) {
						account.setRemix(style == 3);
					} else if (customId.contains("HighVariabilityMode") && style == 3) {
						String variation = CharSequenceUtil.subBefore(label, " Variation Mode", false);
						account.setVariation(VariationLevel.fromValue(variation));
					} else if (customId.contains("TurboMode") && style == 3) {
						account.setMode(AccountMode.TURBO);
					} else if (customId.contains("FastMode") && style == 3) {
						account.setMode(AccountMode.FAST);
					} else if (customId.contains("RelaxMode") && style == 3) {
						account.setMode(AccountMode.RELAX);
					}
				}
			}
		}
		if (BotType.MID_JOURNEY.getValue().equals(applicationId)) {
			account.setButtons(messageButtons);
		} else if (BotType.NIJI_JOURNEY.getValue().equals(applicationId)) {
			account.setNijiButtons(messageButtons);
		}
		AsyncLockUtils.LockObject lock = AsyncLockUtils.getLock("settings-" + applicationId + ":" + account.getId());
		if (lock != null) {
			lock.awake();
		} else {
			this.accountStoreService.save(account);
		}
	}

}
