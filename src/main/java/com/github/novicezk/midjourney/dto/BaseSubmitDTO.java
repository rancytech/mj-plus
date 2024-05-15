package com.github.novicezk.midjourney.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseSubmitDTO {

	@ApiModelProperty(value = "自定义参数", allowEmptyValue = true, position = 10)
	protected String state;

	@ApiModelProperty(value = "回调地址, 为空时使用全局notifyHook", allowEmptyValue = true, position = 10)
	protected String notifyHook;
}
