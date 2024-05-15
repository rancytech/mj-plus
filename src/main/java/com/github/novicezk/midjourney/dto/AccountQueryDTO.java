package com.github.novicezk.midjourney.dto;

import com.github.novicezk.midjourney.condition.AccountCondition;
import com.github.novicezk.midjourney.enums.AccountMode;
import com.github.novicezk.midjourney.enums.SubscribePlan;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("账号查询参数")
public class AccountQueryDTO extends BaseSearchDTO {
	@ApiModelProperty("ID")
	private String id;
	@ApiModelProperty("频道ID")
	private String channelId;
	@ApiModelProperty("账号名")
	private String name;
	@ApiModelProperty("账号名")
	private String remark;
	@ApiModelProperty(value = "账号模式", example = "RELAX")
	private AccountMode mode;
	@ApiModelProperty(value = "订阅计划", example = "STANDARD")
	private SubscribePlan subscribePlan;
	@ApiModelProperty(value = "是否可用", example = "true")
	private Boolean enable;

	public AccountCondition getCondition() {
		AccountCondition condition = new AccountCondition();
		if (this.mode != null) {
			condition.setModes(Set.of(this.mode));
		}

		if (this.subscribePlan != null) {
			condition.setSubscribePlans(Set.of(this.subscribePlan));
		}

		condition.setId(this.id);
		condition.setChannelId(this.channelId);
		condition.setNameContains(this.name);
		condition.setRemarkContains(this.remark);
		condition.setEnable(this.enable);
		return condition;
	}
}
