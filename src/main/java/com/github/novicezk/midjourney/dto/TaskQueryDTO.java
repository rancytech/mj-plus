package com.github.novicezk.midjourney.dto;

import com.github.novicezk.midjourney.condition.TaskCondition;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskQueryDTO extends BaseSearchDTO {

	private TaskStatus status;
	private Set<TaskStatus> statusSet;
	private TaskAction action;
	private Set<String> ids;
	private String prompt;
	private String promptEn;
	private String description;
	private String instanceId;
	private String state;

	public TaskCondition getCondition() {
		TaskCondition condition = new TaskCondition();
		if (this.status != null) {
			condition.setStatusSet(Set.of(this.status));
		}
		if (this.statusSet != null) {
			condition.setStatusSet(this.statusSet);
		}
		if (this.action != null) {
			condition.setActionSet(Set.of(this.action));
		}
		condition.setIds(this.ids);
		condition.setPromptContains(this.prompt);
		condition.setPromptEnContains(this.promptEn);
		condition.setDescriptionContains(this.description);
		condition.setInstanceId(this.instanceId);
		condition.setState(this.state);
		return condition;
	}
}
