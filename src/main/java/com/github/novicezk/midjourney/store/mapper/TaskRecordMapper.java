package com.github.novicezk.midjourney.store.mapper;


import cn.hutool.core.util.EnumUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.support.MessageButton;
import com.github.novicezk.midjourney.util.JsonUtils;
import org.jetbrains.annotations.Nullable;
import org.jooq.Record;
import org.json.JSONObject;

import java.util.List;

public class TaskRecordMapper extends DomainRecordMapper<Task> {
	@Override
	public @Nullable Task map(Record record) {
		Task task = new Task();
		task.setId(record.get("id", String.class));
		task.setProperties(new JSONObject(record.get("properties", String.class)).toMap());
		task.setAction(EnumUtil.fromString(TaskAction.class, record.get("action", String.class)));
		task.setStatus(EnumUtil.fromString(TaskStatus.class, record.get("status", String.class)));
		task.setPrompt(record.get("prompt", String.class));
		task.setPromptEn(record.get("prompt_en", String.class));
		task.setDescription(record.get("description", String.class));
		task.setSubmitTime(record.get("submit_time", Long.class));
		task.setStartTime(record.get("start_time", Long.class));
		task.setFinishTime(record.get("finish_time", Long.class));
		task.setProgress(record.get("progress", String.class));
		task.setImageUrl(record.get("image_url", String.class));
		task.setFailReason(record.get("fail_reason", String.class));
		task.setState(record.get("state", String.class));
		String buttonStr = record.get("buttons", String.class);
		List<MessageButton> messageButtons = JsonUtils.convertJSON2Object(buttonStr, new TypeReference<>() {
		});
		task.setButtons(messageButtons);
		return task;
	}

}
