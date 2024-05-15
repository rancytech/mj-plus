package com.github.novicezk.midjourney.condition;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.domain.Task;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.field;


@Data
@Accessors(chain = true)
public class TaskCondition implements DomainCondition<Task> {
	private Set<String> ids;
	private Set<TaskStatus> statusSet;
	private Set<TaskAction> actionSet;

	// 模糊匹配
	private String promptContains;
	private String promptEnContains;
	private String descriptionContains;

	// 精确匹配
	private String state;
	private String finalPrompt;
	private String messageId;
	private String messageHash;
	private String progressMessageId;
	private String nonce;
	private String instanceId;
	private String channelId;

	@Override
	public boolean test(Task task) {
		if (task == null) {
			return false;
		}
		if (this.ids != null && !this.ids.isEmpty() && !this.ids.contains(task.getId())) {
			return false;
		}
		if (this.statusSet != null && !this.statusSet.isEmpty() && !this.statusSet.contains(task.getStatus())) {
			return false;
		}
		if (this.actionSet != null && !this.actionSet.isEmpty() && !this.actionSet.contains(task.getAction())) {
			return false;
		}

		if (CharSequenceUtil.isNotBlank(this.promptContains) && !CharSequenceUtil.contains(task.getPrompt(), this.promptContains)) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.promptEnContains) && !CharSequenceUtil.contains(task.getPromptEn(), this.promptEnContains)) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.descriptionContains) && !CharSequenceUtil.contains(task.getDescription(), this.descriptionContains)) {
			return false;
		}

		if (CharSequenceUtil.isNotBlank(this.state) && !this.state.equals(task.getState())) {
			return false;
		}

		if (CharSequenceUtil.isNotBlank(this.finalPrompt) && !this.finalPrompt.equals(task.getProperty(Constants.TASK_PROPERTY_FINAL_PROMPT))) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.messageId) && !this.messageId.equals(task.getProperty(Constants.TASK_PROPERTY_MESSAGE_ID))) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.messageHash) && !this.messageHash.equals(task.getProperty(Constants.TASK_PROPERTY_MESSAGE_HASH))) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.progressMessageId) && !this.progressMessageId.equals(task.getProperty(Constants.TASK_PROPERTY_PROGRESS_MESSAGE_ID))) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.nonce) && !this.nonce.equals(task.getProperty(Constants.TASK_PROPERTY_NONCE))) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.instanceId) && !this.instanceId.equals(task.getProperty(Constants.TASK_PROPERTY_DISCORD_INSTANCE_ID))) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.channelId) && !this.channelId.equals(task.getProperty(Constants.TASK_PROPERTY_DISCORD_CHANNEL_ID))) {
			return false;
		}
		return true;
	}

	@Override
	public Condition getSQLCondition() {
		List<Condition> conditions = new ArrayList<>();
		if (this.ids != null && !this.ids.isEmpty()) {
			if (this.ids.size() == 1) {
				conditions.add(field("id").eq(this.ids.iterator().next()));
			} else {
				conditions.add(field("id").in(this.ids));
			}
		}
		if (this.statusSet != null && !this.statusSet.isEmpty()) {
			if (this.statusSet.size() == 1) {
				conditions.add(field("status").eq(this.statusSet.iterator().next().name()));
			} else {
				conditions.add(field("status").in(this.statusSet.stream().map(Enum::name).toList()));
			}
		}
		if (this.actionSet != null && !this.actionSet.isEmpty()) {
			if (this.actionSet.size() == 1) {
				conditions.add(field("action").eq(this.actionSet.iterator().next().name()));
			} else {
				conditions.add(field("action").in(this.actionSet.stream().map(Enum::name).toList()));
			}
		}

		if (CharSequenceUtil.isNotBlank(this.state)) {
			conditions.add(field("state").eq(this.state));
		}

		if (CharSequenceUtil.isNotBlank(this.promptContains)) {
			conditions.add(field("prompt").like("%" + this.promptContains + "%"));
		}
		if (CharSequenceUtil.isNotBlank(this.promptEnContains)) {
			conditions.add(field("prompt_en").like("%" + this.promptEnContains + "%"));
		}
		if (CharSequenceUtil.isNotBlank(this.descriptionContains)) {
			conditions.add(field("description").like("%" + this.descriptionContains + "%"));
		}

		if (CharSequenceUtil.isNotBlank(this.finalPrompt)) {
			conditions.add(condition("JSON_EXTRACT(properties, '$.finalPrompt') = '" + this.finalPrompt + "'"));
		}
		if (CharSequenceUtil.isNotBlank(this.messageId)) {
			conditions.add(condition("JSON_EXTRACT(properties, '$.messageId') = '" + this.messageId + "'"));
		}
		if (CharSequenceUtil.isNotBlank(this.messageHash)) {
			conditions.add(condition("JSON_EXTRACT(properties, '$.messageHash') = '" + this.messageHash + "'"));
		}
		if (CharSequenceUtil.isNotBlank(this.progressMessageId)) {
			conditions.add(condition("JSON_EXTRACT(properties, '$.progressMessageId') = '" + this.progressMessageId + "'"));
		}
		if (CharSequenceUtil.isNotBlank(this.nonce)) {
			conditions.add(condition("JSON_EXTRACT(properties, '$.nonce') = '" + this.nonce + "'"));
		}
		if (CharSequenceUtil.isNotBlank(this.instanceId)) {
			conditions.add(condition("JSON_EXTRACT(properties, '$.discordInstanceId') = '" + this.instanceId + "'"));
		}
		if (CharSequenceUtil.isNotBlank(this.channelId)) {
			conditions.add(condition("JSON_EXTRACT(properties, '$.discordChannelId') = '" + this.channelId + "'"));
		}
		return DSL.and(conditions);
	}

}
