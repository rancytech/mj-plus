package com.github.novicezk.midjourney.support;


import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstanceImpl;
import com.github.novicezk.midjourney.service.NotifyService;
import com.github.novicezk.midjourney.store.AccountStoreService;
import com.github.novicezk.midjourney.store.TaskStoreService;
import com.github.novicezk.midjourney.wss.handle.MessageHandler;
import com.github.novicezk.midjourney.wss.user.UserMessageListener;
import com.github.novicezk.midjourney.wss.user.UserWebSocketStarter;
import lombok.RequiredArgsConstructor;

import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DiscordAccountHelper {
	private final DiscordHelper discordHelper;
	private final ProxyProperties properties;
	private final RestTemplate restTemplate;
	private final TaskStoreService taskStoreService;
	private final AccountStoreService accountStoreService;
	private final NotifyService notifyService;
	private final List<MessageHandler> messageHandlers;

	public JSONObject getDiscordUser(String userToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", userToken);
		String url = this.discordHelper.getServer() + "/api/v9/users/@me";
		ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
		if (response.getStatusCode().is2xxSuccessful()) {
			return new JSONObject(response.getBody());
		} else {
			throw new ValidateException("获取用户信息失败: " + response.getStatusCodeValue());
		}
	}

	public DiscordInstance createDiscordInstance(DiscordAccount account) {
		if (!CharSequenceUtil.isAllNotBlank(account.getGuildId(), account.getChannelId(), account.getUserToken())) {
			throw new IllegalArgumentException("guildId, channelId, userToken must not be blank");
		}
		if (CharSequenceUtil.isBlank(account.getUserAgent())) {
			account.setUserAgent(Constants.DEFAULT_DISCORD_USER_AGENT);
		}
		var messageListener = new UserMessageListener(this.messageHandlers);
		var webSocketStarter = new UserWebSocketStarter(this.discordHelper.getWss(), this.discordHelper.getResumeWss(), account, messageListener,
				this.accountStoreService, this.properties.getProxy());
		DiscordInstanceImpl impl = new DiscordInstanceImpl(account, webSocketStarter, this.restTemplate, this.taskStoreService, this.notifyService);
		messageListener.setInstance(impl);
		return impl;
	}
}
