package com.github.novicezk.midjourney.support;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("可执行按钮")
public class MessageSelector {
	@ApiModelProperty("动作标识")
	private String customId;
	@ApiModelProperty("文本")
	private String placeholder;
	@ApiModelProperty("类型，系统内部使用")
	private int type = 3;

	private List<Option> options;

	public List<Option> getOptions() {
		if (this.options == null) {
			this.options = new ArrayList<>();
		}
		return this.options;
	}

	@Data
	public static class Option {
		@ApiModelProperty("图标")
		private String emoji = "";
		@ApiModelProperty("文本")
		private String label;
		@ApiModelProperty("选项值")
		private String value;
		@ApiModelProperty("是否选中")
		private boolean selected = false;
		@ApiModelProperty("说明")
		private String description;
	}

}
