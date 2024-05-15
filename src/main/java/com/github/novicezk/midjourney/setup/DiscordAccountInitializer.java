package com.github.novicezk.midjourney.setup;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.store.AccountStoreService;
import com.github.novicezk.midjourney.support.DiscordAccountHelper;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import com.github.novicezk.midjourney.util.SnowFlake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordAccountInitializer implements ApplicationRunner {
	private final DiscordLoadBalancer discordLoadBalancer;
	private final DiscordAccountHelper discordAccountHelper;
	private final ProxyProperties properties;
	private final AccountStoreService accountStoreService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		ProxyProperties.ProxyConfig proxy = this.properties.getProxy();
		if (Strings.isNotBlank(proxy.getHost())) {
			System.setProperty("http.proxyHost", proxy.getHost());
			System.setProperty("http.proxyPort", String.valueOf(proxy.getPort()));
			System.setProperty("https.proxyHost", proxy.getHost());
			System.setProperty("https.proxyPort", String.valueOf(proxy.getPort()));
		}

		List<ProxyProperties.DiscordAccountConfig> configAccounts = this.properties.getAccounts();
		if (CharSequenceUtil.isNotBlank(this.properties.getDiscord().getChannelId())) {
			configAccounts.add(this.properties.getDiscord());
		}
		List<DiscordAccount> existsAccounts = new ArrayList<>(this.accountStoreService.listAll());
		existsAccounts.stream().collect(Collectors.groupingBy(DiscordAccount::getUserToken)).forEach((userToken, accouts) -> {
			if (accouts.size() > 1) {
				throw new ValidateException("存在重复的用户Token: " + userToken);
			}
		});
		Set<String> existsUserTokens = existsAccounts.stream().map(DiscordAccount::getUserToken).collect(Collectors.toSet());
		for (ProxyProperties.DiscordAccountConfig configAccount : configAccounts) {
			if (!CharSequenceUtil.isAllNotBlank(configAccount.getChannelId(), configAccount.getUserToken())) {
				continue;
			}
			if (!existsUserTokens.contains(configAccount.getUserToken())) {
				existsUserTokens.add(configAccount.getUserToken());
				DiscordAccount account = new DiscordAccount();
				BeanUtil.copyProperties(configAccount, account);
				account.setId(SnowFlake.INSTANCE.nextId());
				account.setDateCreated(new Date());
				this.accountStoreService.save(account);
				existsAccounts.add(account);
			}
		}
		Set<String> enableInstanceIds = new HashSet<>();
		for (DiscordAccount account : existsAccounts) {
			if (!account.isEnable()) {
				continue;
			}
			try {
				DiscordInstance instance = this.discordAccountHelper.createDiscordInstance(account);
				instance.start();
				this.discordLoadBalancer.getAllInstances().add(instance);
				syncAccountInfo(instance);
				enableInstanceIds.add(instance.getInstanceId());
			} catch (Exception e) {
				log.error("[wss-{}] start fail, account disabled: {}", account.getDisplay(), e.getMessage());
				account.setEnable(false);
				this.accountStoreService.save(account);
			}
		}
		log.info("当前可用账号数 [{}] - {}", enableInstanceIds.size(), String.join(", ", enableInstanceIds));
	}

	private void syncAccountInfo(DiscordInstance instance) {
		if (CharSequenceUtil.isNotBlank(instance.account().getFastTimeRemaining())) {
			return;
		}

		ThreadUtil.execute(() -> {
			try {
				Message<Void> message = instance.info(BotType.MID_JOURNEY, SnowFlake.INSTANCE.nextId());
				if (message.getCode() != ReturnCode.SUCCESS) {
					return;
				}
				AsyncLockUtils.waitForLock("info:" + instance.getInstanceId(), Duration.ofMinutes(1L));
				this.accountStoreService.save(instance.account());
				message = instance.settings(BotType.MID_JOURNEY, SnowFlake.INSTANCE.nextId());
				if (message.getCode() != ReturnCode.SUCCESS) {
					return;
				}
				AsyncLockUtils.waitForLock("settings-" + BotType.MID_JOURNEY.getValue() + ":" + instance.getInstanceId(), Duration.ofMinutes(1L));
				this.accountStoreService.save(instance.account());
				try {
					message = instance.settings(BotType.NIJI_JOURNEY, SnowFlake.INSTANCE.nextId());
					if (message.getCode() != ReturnCode.SUCCESS) {
						return;
					}
					AsyncLockUtils.waitForLock("settings-" + BotType.NIJI_JOURNEY.getValue() + ":" + instance.getInstanceId(), Duration.ofMinutes(1));
					this.accountStoreService.save(instance.account());
				} catch (UnsupportedOperationException e) {
					// do nothing
				}
			} catch (TimeoutException e) {
				log.warn("sync account info timeout: {}", instance.account().getDisplay());
			}
		});
	}
}
