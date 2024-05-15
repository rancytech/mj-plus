package com.github.novicezk.midjourney.jsr;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("激活服务参数")
public class ActivationDTO {
	@ApiModelProperty(value = "激活码", required = true)
	private String code;
}
