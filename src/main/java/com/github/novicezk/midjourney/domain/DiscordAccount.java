package com.github.novicezk.midjourney.domain;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.annotation.DisplayDate;
import com.github.novicezk.midjourney.enums.AccountMode;
import com.github.novicezk.midjourney.enums.BilledWay;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.StylizeLevel;
import com.github.novicezk.midjourney.enums.SubscribePlan;
import com.github.novicezk.midjourney.enums.VariationLevel;
import com.github.novicezk.midjourney.support.MessageButton;
import com.github.novicezk.midjourney.support.MessageSelector;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("Discord账号")
public class DiscordAccount extends DomainObject {

	@ApiModelProperty("服务器ID")
	private String guildId;
	@ApiModelProperty("频道ID")
	private String channelId;
	@ApiModelProperty("用户Token")
	private String userToken;
	@ApiModelProperty("用户ID")
	private String userId;
	@ApiModelProperty("MJ私信ID")
	private String mjBotChannelId;
	@ApiModelProperty("NIJI私信ID")
	private String nijiBotChannelId;
	@ApiModelProperty(value = "用户SessionId", hidden = true)
	private String sessionId;
	@ApiModelProperty("用户UserAgent")
	private String userAgent = Constants.DEFAULT_DISCORD_USER_AGENT;

	@ApiModelProperty("是否可用")
	private boolean enable = true;
	@DisplayDate(format = "yyyy-MM-dd HH:mm")
	@ApiModelProperty("创建时间")
	private Date dateCreated;

	@ApiModelProperty("并发数")
	private int coreSize = 3;
	@ApiModelProperty("等待队列长度")
	private int queueSize = 10;
	@ApiModelProperty("任务超时时间(分钟)")
	private int timeoutMinutes = 5;

	@ApiModelProperty(value = "remix自动提交", notes = "共享账号无法自主控制remix模式时使用，自动提交reroll、variation、pan的弹框")
	private boolean remixAutoSubmit = false;

	@ApiModelProperty("备注说明")
	private String remark;

	@ApiModelProperty("账号名")
	private String name;
	@ApiModelProperty("邮箱")
	private String email;
	@ApiModelProperty("启用remix")
	private boolean remix = false;
	@ApiModelProperty("niji启用remix")
	private boolean nijiRemix = false;
	@ApiModelProperty("启用raw")
	private boolean raw = false;
	@ApiModelProperty("是否公开")
	private boolean publicMode = true;
	@ApiModelProperty("stylize级别")
	private StylizeLevel stylize = StylizeLevel.MED;
	@ApiModelProperty("variation级别")
	private VariationLevel variation = VariationLevel.LOW;
	@ApiModelProperty("mj版本")
	private String version;

	@ApiModelProperty("账号模式")
	private AccountMode mode = AccountMode.RELAX;
	@ApiModelProperty("niji模式")
	private AccountMode nijiMode;

	@ApiModelProperty("快速时间剩余")
	private String fastTimeRemaining;
	@ApiModelProperty("总用量")
	private String lifetimeUsage;
	@ApiModelProperty("relax用量")
	private String relaxedUsage;

	@ApiModelProperty("订阅计划")
	private SubscribePlan subscribePlan = SubscribePlan.STANDARD;
	@ApiModelProperty("计费方式")
	private BilledWay billedWay = BilledWay.MONTHLY;
	@DisplayDate(format = "yyyy-MM-dd HH:mm")
	@ApiModelProperty("续订时间")
	private Date renewDate;

	@ApiModelProperty("动作按钮")
	private List<MessageButton> buttons;
	@ApiModelProperty("版本选择")
	private MessageSelector versionSelector;
	@ApiModelProperty("niji动作按钮")
	private List<MessageButton> nijiButtons;

	@JsonIgnore
	public String getDisplay() {
		return CharSequenceUtil.isBlank(this.name) ? this.channelId : this.name;
	}

	@JsonIgnore
	public boolean customRemix(BotType botType) {
		return botType == BotType.MID_JOURNEY ? this.remix : this.nijiRemix;
	}

	@JsonIgnore
	public AccountMode customMode(BotType botType) {
		return botType == BotType.MID_JOURNEY ? this.mode : this.nijiMode;
	}

	public List<MessageButton> getButtons() {
		if (this.buttons == null) {
			this.buttons = new ArrayList<>();
		}
		return this.buttons;
	}

	public List<MessageButton> getNijiButtons() {
		if (this.nijiButtons == null) {
			this.nijiButtons = new ArrayList<>();
		}
		return this.nijiButtons;
	}

}
