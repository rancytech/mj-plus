package com.github.novicezk.midjourney.store.impl;

import cn.hutool.core.util.EnumUtil;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.store.AbstractMySQLService;
import com.github.novicezk.midjourney.store.AccountStoreService;
import com.github.novicezk.midjourney.store.mapper.AccountRecordMapper;
import com.github.novicezk.midjourney.store.mapper.DomainRecordMapper;
import com.github.novicezk.midjourney.support.MessageSelector;
import com.github.novicezk.midjourney.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Field;
import org.jooq.JSON;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.unquotedName;

@Slf4j
public class MySQLAccountServiceImpl extends AbstractMySQLService<DiscordAccount> implements AccountStoreService {
	@Override
	protected String tableName() {
		return "mj_account";
	}

	@Override
	protected DomainRecordMapper<DiscordAccount> recordMapper() {
		return new AccountRecordMapper();
	}

	@Override
	protected Map<Field<?>, Object> domainDSLMap(DiscordAccount account) {
		Map<Field<?>, Object> map = new HashMap<>();
		map.put(field("guild_id"), account.getGuildId());
		map.put(field("channel_id"), account.getChannelId());
		map.put(field("user_id"), account.getUserId());
		map.put(field("user_token"), account.getUserToken());
		map.put(field("mj_bot_channel_id"), account.getMjBotChannelId());
		map.put(field("niji_bot_channel_id"), account.getNijiBotChannelId());
		map.put(field("user_agent"), account.getUserAgent());
		map.put(field("enable"), account.isEnable());
		map.put(field("date_created"), account.getDateCreated());
		map.put(field("core_size"), account.getCoreSize());
		map.put(field("queue_size"), account.getQueueSize());
		map.put(field("timeout_minutes"), account.getTimeoutMinutes());
		map.put(field("remix_auto_submit"), account.isRemixAutoSubmit());
		map.put(field("remark"), account.getRemark());
		map.put(field("name"), account.getName());
		map.put(field("email"), account.getEmail());
		map.put(field("remix"), account.isRemix());
		map.put(field("niji_remix"), account.isNijiRemix());
		map.put(field("raw"), account.isRaw());
		map.put(field("public_mode"), account.isPublicMode());
		map.put(field("stylize"), EnumUtil.toString(account.getStylize()));
		map.put(field("variation"), EnumUtil.toString(account.getVariation()));
		map.put(field("version"), account.getVersion());
		map.put(field("mode"), EnumUtil.toString(account.getMode()));
		map.put(field("niji_mode"), EnumUtil.toString(account.getNijiMode()));
		map.put(field("fast_time_remaining"), account.getFastTimeRemaining());
		map.put(field("lifetime_usage"), account.getLifetimeUsage());
		map.put(field("relaxed_usage"), account.getRelaxedUsage());
		map.put(field("subscribe_plan"), EnumUtil.toString(account.getSubscribePlan()));
		map.put(field("billed_way"), EnumUtil.toString(account.getBilledWay()));
		map.put(field("renew_date"), account.getRenewDate());
		map.put(field("buttons", JSON.class), JSON.valueOf(JsonUtils.convertObject2JSON(account.getButtons())));
		map.put(field("niji_buttons", JSON.class), JSON.valueOf(JsonUtils.convertObject2JSON(account.getNijiButtons())));
		MessageSelector versionSelector = account.getVersionSelector();
		if (versionSelector != null) {
			map.put(field("version_selector", JSON.class), JSON.valueOf(JsonUtils.convertObject2JSON(versionSelector)));
		}
		return map;
	}

	@Override
	protected void initTable() {
		List<Field<?>> fieldList = new ArrayList<>();
		fieldList.add(field("id", SQLDataType.VARCHAR(32)));
		fieldList.add(field("properties", SQLDataType.JSON));
		fieldList.add(field("guild_id", SQLDataType.VARCHAR(32)));
		fieldList.add(field("channel_id", SQLDataType.VARCHAR(32)));
		fieldList.add(field("user_id", SQLDataType.VARCHAR(32)));
		fieldList.add(field("user_token", SQLDataType.VARCHAR(100)));
		fieldList.add(field("mj_bot_channel_id", SQLDataType.VARCHAR(32)));
		fieldList.add(field("niji_bot_channel_id", SQLDataType.VARCHAR(32)));
		fieldList.add(field("user_agent", SQLDataType.VARCHAR(200)));
		fieldList.add(field("enable", SQLDataType.BIT.defaultValue(true)));
		fieldList.add(field("date_created", new DefaultDataType<>(null, Timestamp.class, "datetime").precision(6)));
		fieldList.add(field("core_size", SQLDataType.INTEGER));
		fieldList.add(field("queue_size", SQLDataType.INTEGER));
		fieldList.add(field("timeout_minutes", SQLDataType.INTEGER));
		fieldList.add(field("remix_auto_submit", SQLDataType.BIT.defaultValue(false)));
		fieldList.add(field("remark", SQLDataType.VARCHAR(500)));
		fieldList.add(field("name", SQLDataType.VARCHAR(100)));
		fieldList.add(field("email", SQLDataType.VARCHAR(100)));
		fieldList.add(field("remix", SQLDataType.BIT.defaultValue(false)));
		fieldList.add(field("niji_remix", SQLDataType.BIT.defaultValue(false)));
		fieldList.add(field("raw", SQLDataType.BIT.defaultValue(false)));
		fieldList.add(field("public_mode", SQLDataType.BIT.defaultValue(true)));
		fieldList.add(field("stylize", SQLDataType.VARCHAR(20)));
		fieldList.add(field("variation", SQLDataType.VARCHAR(20)));
		fieldList.add(field("version", SQLDataType.VARCHAR(50)));
		fieldList.add(field("mode", SQLDataType.VARCHAR(20)));
		fieldList.add(field("niji_mode", SQLDataType.VARCHAR(20)));
		fieldList.add(field("fast_time_remaining", SQLDataType.VARCHAR(200)));
		fieldList.add(field("lifetime_usage", SQLDataType.VARCHAR(200)));
		fieldList.add(field("relaxed_usage", SQLDataType.VARCHAR(200)));
		fieldList.add(field("subscribe_plan", SQLDataType.VARCHAR(20)));
		fieldList.add(field("billed_way", SQLDataType.VARCHAR(20)));
		fieldList.add(field("renew_date", new DefaultDataType<>(null, Timestamp.class, "datetime").precision(6)));
		fieldList.add(field("buttons", SQLDataType.JSON));
		fieldList.add(field("niji_buttons", SQLDataType.JSON));
		fieldList.add(field("version_selector", SQLDataType.JSON));
		try (var ddl = this.dslContext.createTableIfNotExists(tableName())) {
			ddl.columns(fieldList).constraint(constraint(unquotedName(tableName() + "_pkey"))
					.primaryKey(field("id"))).execute();
		}
		addColumnIfNotExists("niji_bot_channel_id", "varchar(32)");
		addColumnIfNotExists("niji_remix", "bit default b'0'");
		addColumnIfNotExists("niji_mode", "varchar(20)");
		addColumnIfNotExists("niji_buttons", "json");
		addColumnIfNotExists("user_id", "varchar(32)");
		addColumnIfNotExists("email", "varchar(100)");
	}

	private void addColumnIfNotExists(String column, String datatype) {
		try (var query = this.dslContext.resultQuery("show columns from `" + tableName() + "` like '" + column + "'")) {
			if (query.fetch().isEmpty()) {
				this.dslContext.execute("alter table `" + tableName() + "` add column " + column + " " + datatype + " null");
			}
		}
	}

}
