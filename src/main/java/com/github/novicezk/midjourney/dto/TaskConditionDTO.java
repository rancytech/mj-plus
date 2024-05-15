package com.github.novicezk.midjourney.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("任务查询参数")
public class TaskConditionDTO {

	@ApiModelProperty(value = "任务ID数组")
	private List<String> ids;

}
