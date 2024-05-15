package com.github.novicezk.midjourney.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BaseSearchDTO {
	@ApiModelProperty(value = "每页条数", example = "10", position = -1)
	private int pageSize = 10;
	@ApiModelProperty(value = "当前页, 从0开始", example = "0", position = -1)
	private int pageNumber = 0;
	@ApiModelProperty(value = "排序", example = "id,desc", position = -1)
	private String sort;
}
