package com.github.novicezk.midjourney;

import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.enums.StoreType;
import com.github.novicezk.midjourney.enums.TranslateWay;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "mj")
public class ProxyProperties {
	/**
	 * 默认mj-bot类型.
	 */
	private BotType botType = BotType.MID_JOURNEY;
	/**
	 * task存储配置.
	 */
	private final TaskStore taskStore = new TaskStore();
	/**
	 * 账号存储方式.
	 */
	private StoreType accountStoreType = StoreType.IN_MEMORY;
	/**
	 * discord单账号配置.
	 */
	private final DiscordAccountConfig discord = new DiscordAccountConfig();
	/**
	 * discord账号池配置.
	 */
	private final List<DiscordAccountConfig> accounts = new ArrayList<>();
	/**
	 * discord账号信息同步cron.
	 */
	private String accountSyncCron = "0 30 20 * * ?";
	/**
	 * discord账号选择规则.
	 */
	private String accountChooseRule = "BestWaitIdleRule";
	/**
	 * 代理配置.
	 */
	private final ProxyConfig proxy = new ProxyConfig();
	/**
	 * 反代配置.
	 */
	private final NgDiscordConfig ngDiscord = new NgDiscordConfig();
	/**
	 * 百度翻译配置.
	 */
	private final BaiduTranslateConfig baiduTranslate = new BaiduTranslateConfig();
	/**
	 * openai配置.
	 */
	private final OpenaiConfig openai = new OpenaiConfig();
	/**
	 * DEEPL翻译配置.
	 */
	private final DeeplTranslateConfig deeplTranslate = new DeeplTranslateConfig();
	/**
	 * 中文翻译成英文的方式.
	 */
	private TranslateWay translateWay = TranslateWay.NULL;
	/**
	 * 英文翻译成中文的方式.
	 */
	private TranslateWay translateZhWay = TranslateWay.NULL;
	/**
	 * 登录的用户名.
	 */
	private String username = "admin";
	/**
	 * 登录的密码，默认为mj.api-secret(空默认admin).
	 */
	private String password;
	/**
	 * admin管理页图片前缀，可设置`https://wsrv.nl/?url=`反代discord图片.
	 */
	private String adminImagePrefix = "";
	/**
	 * 接口密钥或登录密码；配置后调用接口时需要加请求头 mj-api-secret.
	 */
	private String apiSecret;
	/**
	 * 任务状态变更回调地址.
	 */
	private String notifyHook;
	/**
	 * 通知回调线程池大小.
	 */
	private int notifyPoolSize = 10;
	/**
	 * mj错误描述转换.
	 */
	private final List<ErrorDesc> errorDesc = new ArrayList<>();

	@Data
	public static class ErrorDesc {
		private String en;
		private String zh;
	}

	@Data
	public static class BaiduTranslateConfig {
		/**
		 * 百度翻译的APP_ID.
		 */
		private String appid;
		/**
		 * 百度翻译的密钥.
		 */
		private String appSecret;
	}

	@Data
	public static class OpenaiConfig {
		/**
		 * 自定义gpt的api-url.
		 */
		private String gptApiUrl;
		/**
		 * gpt的api-key.
		 */
		private String gptApiKey;
		/**
		 * 超时时间.
		 */
		private Duration timeout = Duration.ofSeconds(30);
		/**
		 * 使用的模型.
		 */
		private String model = "gpt-3.5-turbo";
		/**
		 * 返回结果的最大分词数.
		 */
		private int maxTokens = 2048;
		/**
		 * 相似度，取值 0-2.
		 */
		private double temperature = 0;
	}

	@Data
	public static class DeeplTranslateConfig {
		/**
		 * DEEPL翻译的鉴权密钥.
		 */
		private String authKey;
	}

	@Data
	public static class TaskStore {
		/**
		 * 任务过期时间，默认30天.
		 */
		private Duration timeout = Duration.ofDays(30);
		/**
		 * 任务存储方式: redis(默认)、in_memory.
		 */
		private StoreType type = StoreType.IN_MEMORY;
	}

	@Data
	public static class ProxyConfig {
		/**
		 * 代理host.
		 */
		private String host;
		/**
		 * 代理端口.
		 */
		private Integer port;
	}

	@Data
	public static class NgDiscordConfig {
		/**
		 * https://discord.com 反代.
		 */
		private String server;
		/**
		 * https://cdn.discordapp.com 反代.
		 */
		private String cdn;
		/**
		 * wss://gateway.discord.gg 反代.
		 */
		private String wss;
		/**
		 * wss://gateway-us-east1-b.discord.gg 反代.
		 */
		private String resumeWss;
		/**
		 * https://936929561302675456.discordsays.com 反代，用于局部重绘.
		 */
		private String mjSaysServer;
		/**
		 * https://discord-attachments-uploads-prd.storage.googleapis.com 反代.
		 */
		private String uploadServer;
	}

	@Data
	public static class DiscordAccountConfig {
		/**
		 * 服务器ID.
		 */
		private String guildId;
		/**
		 * 频道ID.
		 */
		private String channelId;
		/**
		 * 用户Token.
		 */
		private String userToken;
		/**
		 * MJ私信ID.
		 */
		private String mjBotChannelId;
		/**
		 * 用户UserAgent.
		 */
		private String userAgent = Constants.DEFAULT_DISCORD_USER_AGENT;
		/**
		 * 是否可用.
		 */
		private boolean enable = true;
		/**
		 * 并发数.
		 */
		private int coreSize = 3;
		/**
		 * 等待队列长度.
		 */
		private int queueSize = 10;
		/**
		 * 任务超时时间(分钟).
		 */
		private int timeoutMinutes = 5;
		/**
		 * remix自动提交, 共享账号无法自主控制remix模式时使用，自动提交reroll、pan、variation的弹框.
		 */
		private boolean remixAutoSubmit = false;
		/**
		 * 备注说明.
		 */
		private String remark;
	}
}
