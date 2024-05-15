package com.github.novicezk.midjourney.dto;

import com.github.novicezk.midjourney.enums.AccountMode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("账号筛选条件")
public class AccountFilter {

	@ApiModelProperty(value = "账号实例ID", allowEmptyValue = true)
	private String instanceId;

	@ApiModelProperty(value = "频道ID", allowEmptyValue = true)
	private String channelId;

	@ApiModelProperty(value = "备注包含", allowEmptyValue = true)
	private String remark;

	@ApiModelProperty(value = "账号模式", allowEmptyValue = true)
	private List<AccountMode> modes;

	@ApiModelProperty(value = "账号是否remix", allowEmptyValue = true)
	private Boolean remix;

	@ApiModelProperty(value = "账号过滤时，`remix自动提交` 视为 `账号的remix为false`", allowEmptyValue = true)
	private Boolean remixAutoConsidered;
}
