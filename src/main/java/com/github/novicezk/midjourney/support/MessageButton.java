package com.github.novicezk.midjourney.support;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("可执行按钮")
public class MessageButton {
	@ApiModelProperty("动作标识")
	private String customId;
	@ApiModelProperty("图标")
	private String emoji = "";
	@ApiModelProperty("文本")
	private String label;
	@ApiModelProperty("类型，系统内部使用")
	private int type = 2;
	@ApiModelProperty("样式: 2（Primary）、3（Green）")
	private int style = 2;

}
