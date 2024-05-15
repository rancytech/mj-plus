package com.github.novicezk.midjourney.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@ApiModel("SwapFace提交参数")
@EqualsAndHashCode(callSuper = true)
public class SwapFaceDTO extends BaseSubmitDTO {
	@ApiModelProperty(value = "人脸源图片base64", required = true, example = "data:image/png;base64,xxx1")
	private String sourceBase64;
	@ApiModelProperty(value = "目标图片base64", required = true, example = "data:image/png;base64,xxx2")
	private String targetBase64;

	@ApiModelProperty(value = "筛选账号执行任务，不需要筛选时设置为null或移除该参数", allowEmptyValue = true, position = 3)
	private Filter accountFilter;

	@Data
	public static class Filter {
		@ApiModelProperty(value = "账号实例ID", allowEmptyValue = true)
		private String instanceId;
	}
}
