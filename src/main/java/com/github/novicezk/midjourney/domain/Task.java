package com.github.novicezk.midjourney.domain;

import com.github.novicezk.midjourney.annotation.DisplayDate;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.support.MessageButton;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("任务")
public class Task extends DomainObject {
	@Serial
	private static final long serialVersionUID = -674915748204390789L;

	@ApiModelProperty("任务类型")
	private TaskAction action;
	@ApiModelProperty("任务状态")
	private TaskStatus status = TaskStatus.NOT_START;

	@ApiModelProperty("提示词")
	private String prompt;
	@ApiModelProperty("提示词-英文")
	private String promptEn;
	@ApiModelProperty("任务描述")
	private String description;

	@DisplayDate(format = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty("提交时间")
	private Long submitTime;
	@DisplayDate(format = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty("开始执行时间")
	private Long startTime;
	@DisplayDate(format = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty("结束时间")
	private Long finishTime;

	@ApiModelProperty("任务进度")
	private String progress;
	@ApiModelProperty("图片url")
	private String imageUrl;

	@ApiModelProperty("失败原因")
	private String failReason;

	@ApiModelProperty("自定义参数")
	private String state;

	private List<MessageButton> buttons;

	public void success() {
		this.finishTime = System.currentTimeMillis();
		this.status = TaskStatus.SUCCESS;
		this.progress = "100%";
	}

	public void fail(String reason) {
		this.finishTime = System.currentTimeMillis();
		this.status = TaskStatus.FAILURE;
		this.failReason = reason;
		this.progress = "";
	}

	public void cancel() {
		this.finishTime = System.currentTimeMillis();
		this.status = TaskStatus.CANCEL;
		this.progress = "";
	}

	public List<MessageButton> getButtons() {
		if (this.buttons == null) {
			this.buttons = new ArrayList<>();
		}
		return this.buttons;
	}
}
