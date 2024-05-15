package com.github.novicezk.midjourney.condition;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.enums.AccountMode;
import com.github.novicezk.midjourney.enums.SubscribePlan;

import lombok.Data;
import lombok.experimental.Accessors;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jooq.impl.DSL.field;


@Data
@Accessors(chain = true)
public class AccountCondition implements DomainCondition<DiscordAccount> {
	private Set<AccountMode> modes;

	// 模糊匹配
	private String nameContains;

	// 精确匹配
	private Boolean enable;
	private String channelId;

	private String remarkContains;
	private String id;
	private Set<SubscribePlan> subscribePlans;
	private String userToken;

	@Override
	public boolean test(DiscordAccount account) {
		if (account == null) {
			return false;
		}
		if (this.modes != null && !this.modes.isEmpty() && !this.modes.contains(account.getMode())) {
			return false;
		}
		if (this.subscribePlans != null && !this.subscribePlans.isEmpty() && !this.subscribePlans.contains(account.getSubscribePlan())) {
			return false;
		}

		if (CharSequenceUtil.isNotBlank(this.nameContains) && !CharSequenceUtil.contains(account.getName(), this.nameContains)) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.remarkContains) && !CharSequenceUtil.contains(account.getRemark(), this.remarkContains)) {
			return false;
		}

		if (CharSequenceUtil.isNotBlank(this.id) && !this.id.equals(account.getId())) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.channelId) && !this.channelId.equals(account.getChannelId())) {
			return false;
		}
		if (CharSequenceUtil.isNotBlank(this.userToken) && !this.userToken.equals(account.getUserToken())) {
			return false;
		}
		if (this.enable != null && !this.enable.equals(account.isEnable())) {
			return false;
		}
		return true;
	}

	@Override
	public Condition getSQLCondition() {
		List<Condition> conditions = new ArrayList<>();
		if (this.modes != null && !this.modes.isEmpty()) {
			if (this.modes.size() == 1) {
				conditions.add(field("mode").eq(this.modes.iterator().next().name()));
			} else {
				conditions.add(field("mode").in(this.modes.stream().map(Enum::name).toList()));
			}
		}

		if (this.subscribePlans != null && !this.subscribePlans.isEmpty()) {
			if (this.subscribePlans.size() == 1) {
				conditions.add(field("subscribe_plan").eq(this.subscribePlans.iterator().next().name()));
			} else {
				conditions.add(field("subscribe_plan").in(this.subscribePlans.stream().map(Enum::name).toList()));
			}
		}

		if (CharSequenceUtil.isNotBlank(this.nameContains)) {
			conditions.add(field("name").like("%" + this.nameContains + "%"));
		}
		if (CharSequenceUtil.isNotBlank(this.remarkContains)) {
			conditions.add(field("remark").like("%" + this.remarkContains + "%"));
		}

		if (this.enable != null) {
			conditions.add(field("enable").eq(this.enable));
		}
		if (CharSequenceUtil.isNotBlank(this.id)) {
			conditions.add(field("id").eq(this.id));
		}
		if (CharSequenceUtil.isNotBlank(this.channelId)) {
			conditions.add(field("channel_id").eq(this.channelId));
		}
		if (CharSequenceUtil.isNotBlank(this.userToken)) {
			conditions.add(field("user_token").eq(this.userToken));
		}
		return DSL.and(conditions);
	}

}
