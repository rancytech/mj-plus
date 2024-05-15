package com.github.novicezk.midjourney.wss.handle;


import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.enums.BilledWay;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.enums.SubscribePlan;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.store.AccountStoreService;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 账号info消息处理.
 */
@Component
public class AccountInfoHandler extends MessageHandler {
	@Autowired
	private AccountStoreService accountStoreService;

	@Override
	public int order() {
		return 1;
	}

	@Override
	public void handle(DiscordInstance instance, MessageType messageType, DataObject message) {
		// Optional<DataObject> interaction = message.optObject("interaction");
		Optional<DataArray> embedsOptional = message.optArray("embeds");
		String interactionName = getInteractionName(message);
		if (!MessageType.UPDATE.equals(messageType) || !"info".equals(interactionName) || embedsOptional.isEmpty()) {
			return;
		}
		message.put(Constants.MJ_MESSAGE_HANDLED, true);
		DiscordAccount account = instance.account();
		DataObject embed = embedsOptional.get().getObject(0);
		// String username = interaction.get().getObject("user").getString("username");
		// account.setName(username);
		String description = embed.getString("description", "");
		List<String> lines = CharSequenceUtil.split(description, "\n");
		for (String line : lines) {
			String[] split = line.split("\\*\\*: ");
			if (split.length != 2) {
				continue;
			}
			String key = split[0].substring(2);
			String value = split[1].trim();
			switch (key) {
				case "Subscription" -> {
					// Standard (Active monthly, renews next on <t:1692258749>)
					account.setSubscribePlan(SubscribePlan.fromValue(CharSequenceUtil.subBefore(value, " (", false)));
					account.setBilledWay(BilledWay.fromValue(CharSequenceUtil.subBetween(value, "Active ", ",")));
					String timeStr = CharSequenceUtil.subBetween(value, "<t:", ">");
					long seconds = Long.parseLong(timeStr);
					account.setRenewDate(new Date(seconds * 1000));
				}
				case "Fast Time Remaining" -> account.setFastTimeRemaining(value);
				case "Lifetime Usage" -> account.setLifetimeUsage(value);
				case "Relaxed Usage" -> account.setRelaxedUsage(value);
			}
		}
		AsyncLockUtils.LockObject lock = AsyncLockUtils.getLock("info:" + account.getId());
		if (lock != null) {
			lock.awake();
		} else {
			this.accountStoreService.save(account);
		}
	}

}
