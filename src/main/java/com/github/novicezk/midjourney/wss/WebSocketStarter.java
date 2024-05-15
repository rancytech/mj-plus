package com.github.novicezk.midjourney.wss;

import com.github.novicezk.midjourney.ProxyProperties;
import com.neovisionaries.ws.client.ProxySettings;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.apache.logging.log4j.util.Strings;

public interface WebSocketStarter {

	void setTrying(boolean trying);

	void start() throws Exception;

	void stop();

	default WebSocketFactory createWebSocketFactory(ProxyProperties.ProxyConfig proxyConfig) {
		WebSocketFactory webSocketFactory = new WebSocketFactory().setConnectionTimeout(10000);
		if (Strings.isNotBlank(proxyConfig.getHost())) {
			ProxySettings proxySettings = webSocketFactory.getProxySettings();
			proxySettings.setHost(proxyConfig.getHost());
			proxySettings.setPort(proxyConfig.getPort());
		}
		return webSocketFactory;
	}
}
