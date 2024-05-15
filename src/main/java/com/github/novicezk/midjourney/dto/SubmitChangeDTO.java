package com.github.novicezk.midjourney.dto;

import com.github.novicezk.midjourney.enums.TaskAction;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@ApiModel("变化任务提交参数")
@EqualsAndHashCode(callSuper = true)
public class SubmitChangeDTO extends BaseSubmitDTO {

	@ApiModelProperty(value = "任务ID", required = true, example = "\"14001934816969359\"")
	private String taskId;

	@ApiModelProperty(value = "UPSCALE(放大); VARIATION(变换)", required = true,
			allowableValues = "UPSCALE, VARIATION", example = "UPSCALE")
	private TaskAction action;

	@ApiModelProperty(value = "序号(1~4)", required = true, allowableValues = "range[1, 4]", example = "1")
	private Integer index;

}
