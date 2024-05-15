package com.github.novicezk.midjourney.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.dto.AccountFilter;
import com.github.novicezk.midjourney.enums.AccountMode;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.function.Predicate;


@Data
@Setter
@Accessors(chain = true)
public class DiscordInstanceCondition implements Predicate<DiscordInstance> {
	private String channelId;
	private String remark;
	private String instanceId;
	private List<AccountMode> modes;
	private Boolean remix;
	private Boolean remixAutoConsidered;
	private final BotType botType;

	public DiscordInstanceCondition(AccountFilter filter, BotType botType) {
		if (filter != null) {
			this.channelId = filter.getChannelId();
			this.remark = filter.getRemark();
			this.instanceId = filter.getInstanceId();
			this.modes = filter.getModes();
			this.remix = filter.getRemix();
			this.remixAutoConsidered = filter.getRemixAutoConsidered();
		}
		this.botType = botType;
	}

	public DiscordInstanceCondition(BotType botType) {
		this.botType = botType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean test(DiscordInstance instance) {
		if (CharSequenceUtil.isNotBlank(this.instanceId) && !this.instanceId.equals(instance.getInstanceId())) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.channelId) && !this.channelId.equals(instance.getChannelId())) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.remark) && !instance.account().getRemark().contains(this.remark)) {
			return false;
		}

		boolean accountRemix = instance.account().customRemix(this.botType);
		if (Boolean.TRUE.equals(this.remixAutoConsidered) && instance.account().isRemixAutoSubmit()) {
			accountRemix = false;
		}
		if (this.remix != null && !this.remix.equals(accountRemix)) {
			return false;
		}

		List<String> applicationIds = instance.account().getProperty(Constants.ACCOUNT_PROPERTY_SUPPORT_APPLICATION_IDS, List.class);
		if (applicationIds == null || !applicationIds.contains(this.botType.getValue())) {
			return false;
		}
		AccountMode accountMode = instance.account().customMode(this.botType);
		return this.modes == null || this.modes.isEmpty() || this.modes.contains(accountMode);
	}
}
