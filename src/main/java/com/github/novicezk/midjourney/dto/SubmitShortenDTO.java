package com.github.novicezk.midjourney.dto;

import com.github.novicezk.midjourney.enums.BotType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@ApiModel("Shorten提交参数")
@EqualsAndHashCode(callSuper = true)
public class SubmitShortenDTO extends BaseSubmitDTO {

	@ApiModelProperty(value = "提示词", required = true, example = "Cat")
	private String prompt;

	@ApiModelProperty(value = "bot类型，mj(默认)或niji", example = "MID_JOURNEY", allowableValues = "MID_JOURNEY,NIJI_JOURNEY", position = -1)
	private BotType botType;

	@ApiModelProperty(value = "筛选账号执行任务，不需要筛选时设置为null或移除该参数", allowEmptyValue = true, position = 1)
	private AccountFilter accountFilter;

}
