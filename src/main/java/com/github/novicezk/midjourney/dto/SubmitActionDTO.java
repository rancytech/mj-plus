package com.github.novicezk.midjourney.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@ApiModel("执行动作参数")
@EqualsAndHashCode(callSuper = true)
public class SubmitActionDTO extends BaseSubmitDTO {

	@ApiModelProperty(value = "任务ID", required = true, example = "\"14001934816969359\"")
	private String taskId;

	@ApiModelProperty(value = "动作标识", required = true, example = "MJ::JOB::upsample::2::3dbbd469-36af-4a0f-8f02-df6c579e7011")
	private String customId;

	@ApiModelProperty(value = "是否选择同一频道下的账号，默认只使用任务关联的账号", allowEmptyValue = true)
	private Boolean chooseSameChannel;

	@ApiModelProperty(value = "筛选账号条件(限同一频道)，不需要筛选时设置为null或移除该参数", allowEmptyValue = true, position = 2)
	private AccountFilter accountFilter;
}
