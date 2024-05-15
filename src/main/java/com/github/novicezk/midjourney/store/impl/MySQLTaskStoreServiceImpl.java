package com.github.novicezk.midjourney.store.impl;

import cn.hutool.core.util.EnumUtil;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.store.AbstractMySQLService;
import com.github.novicezk.midjourney.store.TaskStoreService;
import com.github.novicezk.midjourney.store.mapper.DomainRecordMapper;
import com.github.novicezk.midjourney.store.mapper.TaskRecordMapper;
import com.github.novicezk.midjourney.util.JsonUtils;
import org.jooq.Field;
import org.jooq.JSON;
import org.jooq.impl.SQLDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.unquotedName;


public class MySQLTaskStoreServiceImpl extends AbstractMySQLService<Task> implements TaskStoreService {

	@Override
	protected String tableName() {
		return "mj_task";
	}

	@Override
	protected DomainRecordMapper<Task> recordMapper() {
		return new TaskRecordMapper();
	}

	@Override
	protected Map<Field<?>, Object> domainDSLMap(Task task) {
		Map<Field<?>, Object> map = new HashMap<>();
		map.put(field("action"), EnumUtil.toString(task.getAction()));
		map.put(field("status"), EnumUtil.toString(task.getStatus()));
		map.put(field("prompt"), task.getPrompt());
		map.put(field("prompt_en"), task.getPromptEn());
		map.put(field("description"), task.getDescription());
		map.put(field("submit_time"), task.getSubmitTime());
		map.put(field("start_time"), task.getStartTime());
		map.put(field("finish_time"), task.getFinishTime());
		map.put(field("progress"), task.getProgress());
		map.put(field("image_url"), task.getImageUrl());
		map.put(field("fail_reason"), task.getFailReason());
		map.put(field("state"), task.getState());
		map.put(field("buttons", JSON.class), JSON.valueOf(JsonUtils.convertObject2JSON(task.getButtons())));
		return map;
	}

	@Override
	protected void initTable() {
		List<Field<?>> fieldList = new ArrayList<>();
		fieldList.add(field("id", SQLDataType.VARCHAR(32)));
		fieldList.add(field("properties", SQLDataType.JSON));
		fieldList.add(field("action", SQLDataType.VARCHAR(20)));
		fieldList.add(field("status", SQLDataType.VARCHAR(20)));
		fieldList.add(field("prompt", SQLDataType.VARCHAR(2000)));
		fieldList.add(field("prompt_en", SQLDataType.VARCHAR(2000)));
		fieldList.add(field("description", SQLDataType.VARCHAR(2000)));
		fieldList.add(field("submit_time", SQLDataType.BIGINT));
		fieldList.add(field("start_time", SQLDataType.BIGINT));
		fieldList.add(field("finish_time", SQLDataType.BIGINT));
		fieldList.add(field("progress", SQLDataType.VARCHAR(50)));
		fieldList.add(field("image_url", SQLDataType.VARCHAR(2000)));
		fieldList.add(field("fail_reason", SQLDataType.VARCHAR(1000)));
		fieldList.add(field("state", SQLDataType.VARCHAR(500)));
		fieldList.add(field("buttons", SQLDataType.JSON));
		try (var ddl = this.dslContext.createTableIfNotExists(tableName()).columns(fieldList)
				.constraint(constraint(unquotedName(tableName() + "_pkey"))
						.primaryKey(field("id")))) {
			ddl.execute();
		}
	}
}
