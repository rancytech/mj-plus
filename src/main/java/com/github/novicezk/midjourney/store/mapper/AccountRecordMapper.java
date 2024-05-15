package com.github.novicezk.midjourney.store.mapper;


import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.EnumUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.enums.AccountMode;
import com.github.novicezk.midjourney.enums.BilledWay;
import com.github.novicezk.midjourney.enums.StylizeLevel;
import com.github.novicezk.midjourney.enums.SubscribePlan;
import com.github.novicezk.midjourney.enums.VariationLevel;
import com.github.novicezk.midjourney.support.MessageButton;
import com.github.novicezk.midjourney.support.MessageSelector;
import com.github.novicezk.midjourney.util.JsonUtils;

import org.jetbrains.annotations.Nullable;
import org.jooq.Record;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.List;

public class AccountRecordMapper extends DomainRecordMapper<DiscordAccount> {
	@Override
	public @Nullable DiscordAccount map(Record record) {
		DiscordAccount account = new DiscordAccount();
		account.setId(record.get("id", String.class));
		account.setProperties(new JSONObject(record.get("properties", String.class)).toMap());
		account.setGuildId(record.get("guild_id", String.class));
		account.setUserId(record.get("user_id", String.class));
		account.setChannelId(record.get("channel_id", String.class));
		account.setUserToken(record.get("user_token", String.class));
		account.setMjBotChannelId(record.get("mj_bot_channel_id", String.class));
		account.setNijiBotChannelId(record.get("niji_bot_channel_id", String.class));
		account.setUserAgent(record.get("user_agent", String.class));
		account.setEnable(record.get("enable", Boolean.class));
		account.setDateCreated(record.get("date_created", Timestamp.class));
		account.setCoreSize(record.get("core_size", Integer.class));
		account.setQueueSize(record.get("queue_size", Integer.class));
		account.setTimeoutMinutes(record.get("timeout_minutes", Integer.class));
		account.setRemixAutoSubmit(record.get("remix_auto_submit", Boolean.class));
		account.setRemark(record.get("remark", String.class));
		account.setName(record.get("name", String.class));
		account.setEmail(record.get("email", String.class));
		account.setRemix(record.get("remix", Boolean.class));
		account.setNijiRemix(record.get("niji_remix", Boolean.class));
		account.setRaw(record.get("raw", Boolean.class));
		account.setPublicMode(record.get("public_mode", Boolean.class));
		account.setVersion(record.get("version", String.class));
		account.setFastTimeRemaining(record.get("fast_time_remaining", String.class));
		account.setLifetimeUsage(record.get("lifetime_usage", String.class));
		account.setRelaxedUsage(record.get("relaxed_usage", String.class));
		account.setRenewDate(record.get("renew_date", Timestamp.class));

		String stylize = record.get("stylize", String.class);
		if (CharSequenceUtil.isNotBlank(stylize)) {
			account.setStylize(EnumUtil.fromString(StylizeLevel.class, stylize));
		}
		String variation = record.get("variation", String.class);
		if (CharSequenceUtil.isNotBlank(variation)) {
			account.setVariation(EnumUtil.fromString(VariationLevel.class, variation));
		}
		String mode = record.get("mode", String.class);
		if (CharSequenceUtil.isNotBlank(mode)) {
			account.setMode(EnumUtil.fromString(AccountMode.class, mode));
		}
		String nijiMode = record.get("niji_mode", String.class);
		if (CharSequenceUtil.isNotBlank(nijiMode)) {
			account.setNijiMode(EnumUtil.fromString(AccountMode.class, nijiMode));
		}
		String subscribePlan = record.get("subscribe_plan", String.class);
		if (CharSequenceUtil.isNotBlank(subscribePlan)) {
			account.setSubscribePlan(EnumUtil.fromString(SubscribePlan.class, subscribePlan));
		}
		String billedWay = record.get("billed_way", String.class);
		if (CharSequenceUtil.isNotBlank(billedWay)) {
			account.setBilledWay(EnumUtil.fromString(BilledWay.class, billedWay));
		}

		String buttonStr = record.get("buttons", String.class);
		List<MessageButton> messageButtons = JsonUtils.convertJSON2Object(buttonStr, new TypeReference<>() {
		});
		account.setButtons(messageButtons);

		String nijiButtonStr = record.get("niji_buttons", String.class);
		List<MessageButton> nijiButtons = JsonUtils.convertJSON2Object(nijiButtonStr, new TypeReference<>() {
		});
		account.setNijiButtons(nijiButtons);

		String versionSelectorStr = record.get("version_selector", String.class);
		if (CharSequenceUtil.isNotBlank(versionSelectorStr)) {
			account.setVersionSelector(JsonUtils.convertJSON2Object(versionSelectorStr, MessageSelector.class));
		}
		return account;
	}

}
