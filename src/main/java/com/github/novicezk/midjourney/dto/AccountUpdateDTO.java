package com.github.novicezk.midjourney.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("账号修改参数")
public class AccountUpdateDTO {
	@ApiModelProperty(value = "服务器ID", required = true)
	private String guildId;
	@ApiModelProperty(value = "频道ID", required = true)
	private String channelId;
	@ApiModelProperty(value = "用户Token", required = true)
	private String userToken;
	@ApiModelProperty("MJ私信ID")
	private String mjBotChannelId;
	@ApiModelProperty("niji私信ID")
	private String nijiBotChannelId;
	@ApiModelProperty("用户UserAgent")
	private String userAgent;

	@ApiModelProperty(value = "是否可用", example = "true")
	private Boolean enable;
	@ApiModelProperty(value = "remix自动提交", example = "false")
	private boolean remixAutoSubmit = false;

	@ApiModelProperty(value = "并发数", example = "3")
	private int coreSize = 3;
	@ApiModelProperty(value = "等待队列长度", example = "10")
	private int queueSize = 10;
	@ApiModelProperty(value = "任务超时时间(分钟)", example = "5")
	private int timeoutMinutes = 5;

	@ApiModelProperty("备注说明")
	private String remark;
}
