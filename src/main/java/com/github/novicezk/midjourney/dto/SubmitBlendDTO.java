package com.github.novicezk.midjourney.dto;

import com.github.novicezk.midjourney.enums.BlendDimensions;
import com.github.novicezk.midjourney.enums.BotType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@ApiModel("Blend提交参数")
@EqualsAndHashCode(callSuper = true)
public class SubmitBlendDTO extends BaseSubmitDTO {

	@ApiModelProperty(value = "图片base64数组", required = true, example = "[\"data:image/png;base64,xxx1\", \"data:image/png;base64,xxx2\"]")
	private List<String> base64Array;

	@ApiModelProperty(value = "比例: PORTRAIT(2:3); SQUARE(1:1); LANDSCAPE(3:2)", example = "SQUARE", position = 1)
	private BlendDimensions dimensions = BlendDimensions.SQUARE;

	@ApiModelProperty(value = "bot类型，mj(默认)或niji", example = "MID_JOURNEY", allowableValues = "MID_JOURNEY,NIJI_JOURNEY", position = -1)
	private BotType botType;

	@ApiModelProperty(value = "筛选账号执行任务，不需要筛选时设置为null或移除该参数", allowEmptyValue = true, position = 3)
	private AccountFilter accountFilter;
}
