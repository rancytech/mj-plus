package com.github.novicezk.midjourney.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("Modal提交参数")
public class SubmitModalDTO {

	@ApiModelProperty(value = "任务ID", required = true, example = "\"14001934816969359\"")
	private String taskId;

	@ApiModelProperty(value = "提示词")
	private String prompt;

	@ApiModelProperty(value = "局部重绘的蒙版base64")
	private String maskBase64;

}
