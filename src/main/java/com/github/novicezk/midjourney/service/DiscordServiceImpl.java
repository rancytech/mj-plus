package com.github.novicezk.midjourney.service;


import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.enums.BlendDimensions;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.support.ApplicationCommand;
import com.github.novicezk.midjourney.support.DiscordHelper;
import com.github.novicezk.midjourney.support.MessageSelector;
import com.github.novicezk.midjourney.support.NamedRunnable;
import com.github.novicezk.midjourney.support.SpringContextHolder;
import com.github.novicezk.midjourney.util.AsyncLockUtils;

import eu.maxschuster.dataurl.DataUrl;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class DiscordServiceImpl implements DiscordService {
	private static final String DEFAULT_SESSION_ID = "f1a313a09ce079ce252459dc70231f30";

	private final DiscordAccount account;
	private final RestTemplate restTemplate;
	private final DiscordHelper discordHelper;
	private final Map<String, ApplicationCommand> commandMap = new HashMap<>();

	private final String discordServer;
	private final String discordInteractionUrl;
	private final String discordAttachmentUrl;
	private final String discordMessageUrl;
	private final String discordInpaintUrl;
	private final List<NamedRunnable> interactionQueues = Collections.synchronizedList(new ArrayList<>());
	private Future<?> interactionInterval;

	public DiscordServiceImpl(DiscordAccount account, RestTemplate restTemplate) {
		this.account = account;
		this.restTemplate = restTemplate;
		this.discordHelper = SpringContextHolder.getApplicationContext().getBean(DiscordHelper.class);
		this.discordServer = this.discordHelper.getServer();
		this.discordInteractionUrl = discordServer + "/api/v9/interactions";
		this.discordAttachmentUrl = discordServer + "/api/v9/channels/" + account.getChannelId() + "/attachments";
		this.discordMessageUrl = discordServer + "/api/v9/channels/" + account.getChannelId() + "/messages";
		this.discordInpaintUrl = this.discordHelper.getMjSaysServer() + "/inpaint/api/submit-job";
		// initApplicationCommands(discordServer);
	}

	//

	@Deprecated
	@SuppressWarnings("unused")
	private void initApplicationCommands(String discordServer) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", this.account.getUserToken());
		String applicationCommandsUrl = discordServer + "/api/v9/channels/" + this.account.getChannelId() + "/application-commands/search";
		ResponseEntity<String> response = this.restTemplate.exchange(applicationCommandsUrl + "?type=3&limit=25&include_applications=true", HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);
		JSONObject json = new JSONObject(response.getBody());
		Set<String> botApplicationIds = Arrays.stream(BotType.values()).map(BotType::getValue).collect(Collectors.toSet());
		JSONArray commands = json.getJSONArray("application_commands");
		for (int i = 0; i < commands.length(); i++) {
			JSONObject command = commands.getJSONObject(i);
			String applicationId = command.getString("application_id");
			if (botApplicationIds.contains(applicationId)) {
				String name = command.getString("name");
				String id = command.getString("id");
				String version = command.getString("version");
				String description = command.optString("description");
				this.commandMap.put(applicationId + ":" + name, new ApplicationCommand(id, name, version, applicationId, description));
			}
		}
		List<String> applicationIds = new ArrayList<>();
		JSONArray applications = json.getJSONArray("applications");
		for (int i = 0; i < applications.length(); i++) {
			String applicationId = applications.getJSONObject(i).getString("id");
			if (botApplicationIds.contains(applicationId)) {
				initApplicationCommands(applicationCommandsUrl, applicationId, headers);
				applicationIds.add(applicationId);
			}
		}
		this.account.setProperty(Constants.ACCOUNT_PROPERTY_SUPPORT_APPLICATION_IDS, applicationIds);
	}

	public void initApplicationCommands() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", this.account.getUserToken());
		String applicationCommandsUrl = this.discordServer + "/api/v9/guilds/" + this.account.getGuildId() + "/application-command-index";

		ResponseEntity<String> response = this.restTemplate.exchange(applicationCommandsUrl + "?type=3&limit=25&include_applications=true", HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);

		JSONObject json = new JSONObject(response.getBody());
		Set<String> botApplicationIds = Arrays.stream(BotType.values()).map(BotType::getValue).collect(Collectors.toSet());
		JSONArray commands = json.getJSONArray("application_commands");

		Set<String> applicationIds = new HashSet<>();
		for (int i = 0; i < commands.length(); i++) {
			JSONObject command = commands.getJSONObject(i);
			String applicationId = command.getString("application_id");
			if (botApplicationIds.contains(applicationId)) {
				applicationIds.add(applicationId);
				String name = command.getString("name");
				String id = command.getString("id");
				String version = command.getString("version");
				String description = command.optString("description");
				this.commandMap.put(applicationId + ":" + name, new ApplicationCommand(id, name, version, applicationId, description));
			}
		}

		this.account.setProperty(Constants.ACCOUNT_PROPERTY_SUPPORT_APPLICATION_IDS, new ArrayList<>(applicationIds));
	}

	@Deprecated
	private void initApplicationCommands(String applicationCommandsUrl, String applicationId, HttpHeaders headers) {
		ResponseEntity<String> response = this.restTemplate.exchange(applicationCommandsUrl + "?type=1&application_id=" + applicationId, HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);
		JSONObject json = new JSONObject(response.getBody());
		JSONArray commands = json.getJSONArray("application_commands");
		for (int i = 0; i < commands.length(); i++) {
			JSONObject command = commands.getJSONObject(i);
			String name = command.getString("name");
			String id = command.getString("id");
			String version = command.getString("version");
			String description = command.optString("description");
			this.commandMap.put(applicationId + ":" + name, new ApplicationCommand(id, name, version, applicationId, description));
		}
	}

	public void startInteractionInterval() {
		if (this.interactionInterval != null) {
			this.interactionInterval.cancel(true);
		}
		this.interactionInterval = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
			try {
				NamedRunnable waitRunnable = null;
				int remainingSize = 0;
				synchronized (this.interactionQueues) {
					if (!this.interactionQueues.isEmpty()) {
						waitRunnable = this.interactionQueues.remove(0);
						remainingSize = this.interactionQueues.size();
					}
				}
				if (waitRunnable != null) {
					log.debug("{} - Deferred submit interaction: {}, remaining: {}", this.account.getDisplay(), waitRunnable.getName(), remainingSize);
					waitRunnable.run();
				}
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
			}
		}, 1000, 2500, TimeUnit.MILLISECONDS);
	}

	public void clearInteractionInterval() {
		if (this.interactionInterval != null) {
			this.interactionInterval.cancel(true);
			this.interactionInterval = null;
		}
	}

	@Override
	public Message<Void> imagine(BotType botType, String prompt, String nonce) {
		JSONObject params = getInteractionParams(botType, nonce);
		JSONObject commandData = getCommandData(botType, "imagine");
		commandData.put("options", new JSONArray().put(new JSONObject().put("type", 3).put("name", "prompt").put("value", prompt)));
		params.put("data", commandData);
		return postInteractionAndCheckStatus(params, "imagine");
	}

	@Override
	public Message<Void> action(BotType botType, String messageId, int messageFlags, int componentType, String customId, String nonce) {
		JSONObject params = getInteractionParams(botType, nonce);
		params.put("message_flags", messageFlags).put("message_id", messageId);
		JSONObject data = new JSONObject().put("component_type", componentType).put("custom_id", customId);
		params.put("data", data).put("type", 3);
		return postInteractionAndCheckStatus(params, "action");
	}

	@Override
	public Message<Void> modal(BotType botType, String messageId, String customId, String promptCustomId, String prompt, String nonce) {
		JSONObject params = getInteractionParams(botType, nonce);
		JSONObject data = new JSONObject().put("id", messageId).put("custom_id", customId);
		JSONArray components = new JSONArray().put(new JSONObject().put("type", 4).put("custom_id", promptCustomId).put("value", prompt));
		data.put("components", new JSONArray().put(new JSONObject().put("type", 1).put("components", components)));
		params.put("data", data).put("type", 5);
		return postInteractionAndCheckStatus(params, "modal");
	}

	@Override
	public Message<Void> regionModal(String customId, String prompt, String maskBase64) {
		JSONObject params = new JSONObject()
				.put("customId", customId)
				.put("prompt", prompt)
				.put("mask", maskBase64)
				.put("userId", "0")
				.put("username", "0");
		return postJsonAndCheckStatus(this.discordInpaintUrl, params.toString());
	}

	@Override
	public Message<Void> describe(BotType botType, String finalFileName, String nonce) {
		String fileName = CharSequenceUtil.subAfter(finalFileName, "/", true);
		JSONObject params = getInteractionParams(botType, nonce);
		JSONObject commandData = getCommandData(botType, "describe");
		commandData.put("options", new JSONArray().put(new JSONObject().put("type", 11).put("name", "image").put("value", 0)));
		commandData.put("attachments", new JSONArray().put(new JSONObject().put("id", "0").put("filename", fileName).put("uploaded_filename", finalFileName)));
		params.put("data", commandData);
		return postInteractionAndCheckStatus(params, "describe");
	}

	@Override
	public Message<Void> blend(BotType botType, List<String> finalFileNames, BlendDimensions dimensions, String nonce) {
		JSONObject params = getInteractionParams(botType, nonce);
		JSONObject commandData = getCommandData(botType, "blend");
		JSONArray options = new JSONArray();
		JSONArray attachments = new JSONArray();
		for (int i = 0; i < finalFileNames.size(); i++) {
			String finalFileName = finalFileNames.get(i);
			String fileName = CharSequenceUtil.subAfter(finalFileName, "/", true);
			JSONObject attachment = new JSONObject().put("id", String.valueOf(i))
					.put("filename", fileName)
					.put("uploaded_filename", finalFileName);
			attachments.put(attachment);
			JSONObject option = new JSONObject().put("type", 11)
					.put("name", "image" + (i + 1))
					.put("value", i);
			options.put(option);
		}
		options.put(new JSONObject().put("type", 3)
				.put("name", "dimensions")
				.put("value", "--ar " + dimensions.getValue()));
		commandData.put("options", options).put("attachments", attachments);
		params.put("data", commandData);
		return postInteractionAndCheckStatus(params, "blend");
	}

	@Override
	public Message<Void> shorten(BotType botType, String prompt, String nonce) {
		JSONObject params = getInteractionParams(botType, nonce);
		JSONObject commandData = getCommandData(botType, "shorten");
		commandData.put("options", new JSONArray().put(new JSONObject().put("type", 3).put("name", "prompt").put("value", prompt)));
		params.put("data", commandData);
		return postInteractionAndCheckStatus(params, "shorten");
	}

	@Override
	public Message<Void> cancel(BotType botType, String messageId, String nonce) {
		JSONObject params = getInteractionParams(botType, nonce);
		JSONObject commandData = getCommandData(botType, "Cancel Job")
				.put("type", 3)
				.put("options", new JSONArray())
				.put("attachments", new JSONArray())
				.put("target_id", messageId);
		params.put("data", commandData);
		return postInteractionAndCheckStatus(params, "cancel");
	}

	@Override
	public Message<Void> info(BotType botType, String nonce) {
		JSONObject params = getInteractionParams(botType, nonce);
		JSONObject commandData = getCommandData(botType, "info");
		commandData.put("options", new JSONArray()).put("attachments", new JSONArray());
		params.put("data", commandData);
		return postInteractionAndCheckStatus(params, "info");
	}

	@Override
	public Message<Void> settings(BotType botType, String nonce) {
		JSONObject params = getInteractionParams(botType, nonce);
		JSONObject commandData = getCommandData(botType, "settings");
		commandData.put("options", new JSONArray()).put("attachments", new JSONArray());
		params.put("data", commandData);
		return postInteractionAndCheckStatus(params, "settings");
	}

	@Override
	public Message<Void> changeVersion(String version, String nonce) {
		if (this.account.getVersionSelector() == null) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "未找到账号设置信息");
		}
		Optional<MessageSelector.Option> optional = this.account.getVersionSelector().getOptions().stream().filter(v -> CharSequenceUtil.equals(v.getValue(), version)).findFirst();
		if (optional.isEmpty()) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "错误的版本号");
		}
		BotType botType = BotType.MID_JOURNEY;
		JSONObject params = getInteractionParams(botType, nonce);
		String messageId = account.getProperty(botType.getValue() + ":messageId", String.class, account.getPropertyGeneric("messageId"));
		params.put("message_flags", this.account.getProperty(botType.getValue() + ":flags", Integer.class, 64))
				.put("message_id", messageId);
		JSONObject data = new JSONObject().put("component_type", this.account.getVersionSelector().getType())
				.put("custom_id", this.account.getVersionSelector().getCustomId())
				.put("type", this.account.getVersionSelector().getType())
				.put("values", new JSONArray().put(version));
		params.put("data", data).put("type", 3);
		return postInteractionAndCheckStatus(params, "change-version");
	}

	@Override
	public Message<Void> seed(String messageId, int messageFlags) {
		String url = this.discordMessageUrl + "/" + messageId + "/reactions/%E2%9C%89%EF%B8%8F/%40me?location=Message&type=" + messageFlags;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentLength(0);
			headers.set("Authorization", this.account.getUserToken());
			headers.set("User-Agent", this.account.getUserAgent());
			MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
			RequestEntity<MultiValueMap<String, String>> requestEntity = new RequestEntity<>(body, headers, HttpMethod.PUT, new URI(url));
			this.restTemplate.exchange(requestEntity, Void.class);
			return Message.success();
		} catch (HttpStatusCodeException e) {
			return convertHttpStatusCodeException(e);
		} catch (Exception e) {
			return Message.failure(e.getMessage());
		}
	}

	@Override
	public Message<String> upload(String fileName, DataUrl dataUrl) {
		try {
			JSONObject fileObj = new JSONObject();
			fileObj.put("filename", fileName);
			fileObj.put("file_size", dataUrl.getData().length);
			fileObj.put("id", "0");
			JSONObject params = new JSONObject().put("files", new JSONArray().put(fileObj));
			ResponseEntity<String> responseEntity = postJson(this.discordAttachmentUrl, params.toString());
			if (responseEntity.getStatusCode() != HttpStatus.OK) {
				log.warn("上传图片到discord失败, status: {}, msg: {}", responseEntity.getStatusCodeValue(), responseEntity.getBody());
				return Message.of(ReturnCode.VALIDATION_ERROR, "上传图片到discord失败");
			}
			JSONArray array = new JSONObject(responseEntity.getBody()).getJSONArray("attachments");
			if (array.length() == 0) {
				return Message.of(ReturnCode.VALIDATION_ERROR, "上传图片到discord失败");
			}
			String uploadUrl = array.getJSONObject(0).getString("upload_url");
			String uploadFilename = array.getJSONObject(0).getString("upload_filename");
			putFile(uploadUrl, dataUrl);
			return Message.success(uploadFilename);
		} catch (Exception e) {
			log.error("上传图片到discord失败", e);
			return Message.of(ReturnCode.FAILURE, "上传图片到discord失败");
		}
	}

	@Override
	public Message<List<String>> sendImageMessages(String content, List<String> finalFileNames) {
		JSONArray attach = new JSONArray();
		for (String finalFileName : finalFileNames) {
			String fileName = CharSequenceUtil.subAfter(finalFileName, "/", true);
			attach.put((new JSONObject()).put("id", "0").put("filename", fileName).put("uploaded_filename", finalFileName));
		}

		try {
			JSONObject params = (new JSONObject())
					.put("content", content)
					.put("channel_id", this.account.getChannelId())
					.put("type", 0)
					.put("sticker_ids", new JSONArray())
					.put("attachments", attach);
			ResponseEntity<String> responseEntity = this.postJson(this.discordMessageUrl, params.toString());

			if (responseEntity.getStatusCode() != HttpStatus.OK) {
				log.warn("发送图片消息到discord失败, status: {}, msg: {}", responseEntity.getStatusCodeValue(), responseEntity.getBody());
				return Message.of(ReturnCode.VALIDATION_ERROR, "发送图片消息到discord失败");
			}

			JSONArray attachments = new JSONObject(responseEntity.getBody()).optJSONArray("attachments");
			if (attachments.isEmpty()) {
				return Message.failure("发送图片消息到discord失败: 图片不存在");
			}

			List<String> result = new ArrayList<>(attachments.length());
			for (int i = 0; i < attachments.length(); i++) {
				result.add(attachments.getJSONObject(i).getString("url"));
			}

			ThreadUtil.sleep(5000L);
			return Message.success(result);
		} catch (Exception e) {
			log.error("发送图片信息到discord失败", e);
			return Message.of(ReturnCode.FAILURE, "发送图片信息到discord失败");
		}
	}

	@Deprecated
	public Message<String> _sendImageMessage(String content, String finalFileName) {
		try {
			String fileName = CharSequenceUtil.subAfter(finalFileName, "/", true);
			JSONObject params = new JSONObject().put("content", content)
					.put("channel_id", this.account.getChannelId())
					.put("type", 0)
					.put("sticker_ids", new JSONArray())
					.put("attachments", new JSONArray().put(new JSONObject().put("id", "0").put("filename", fileName).put("uploaded_filename", finalFileName)));
			ResponseEntity<String> responseEntity = postJson(this.discordMessageUrl, params.toString());
			if (responseEntity.getStatusCode() != HttpStatus.OK) {
				log.warn("发送图片消息到discord失败, status: {}, msg: {}", responseEntity.getStatusCodeValue(), responseEntity.getBody());
				return Message.of(ReturnCode.VALIDATION_ERROR, "发送图片消息到discord失败");
			}
			JSONObject result = new JSONObject(responseEntity.getBody());
			JSONArray attachments = result.optJSONArray("attachments");
			if (!attachments.isEmpty()) {
				return Message.success(attachments.getJSONObject(0).optString("url"));
			}
			return Message.failure("发送图片消息到discord失败: 图片不存在");
		} catch (Exception e) {
			log.error("发送图片信息到discord失败", e);
			return Message.of(ReturnCode.FAILURE, "发送图片信息到discord失败");
		}
	}

	@Override
	public Message<Void> saveInsightFace(String name, String finalFileName, String nonce) {
		return saveOrSwapInsightFace("saveid", name, finalFileName, nonce);
	}

	@Override
	public Message<Void> swapInsightFace(String name, String finalFileName, String nonce) {
		return saveOrSwapInsightFace("swapid", name, finalFileName, nonce);
	}

	@Override
	public Message<Void> delInsightFace(String name, String nonce) {
		JSONObject params = getInteractionParams(BotType.INSIGHT_FACE, nonce);
		JSONObject commandData = getCommandData(BotType.INSIGHT_FACE, "delid");
		commandData.put("options", new JSONArray().put(new JSONObject().put("type", 3).put("name", "idname").put("value", name)));
		commandData.put("attachments", new JSONArray());
		params.put("data", commandData).put("analytics_location", "slash_ui");
		return postInteractionAndCheckStatus(params, "delid");
	}

	private Message<Void> saveOrSwapInsightFace(String action, String name, String finalFileName, String nonce) {
		String fileName = CharSequenceUtil.subAfter(finalFileName, "/", true);
		JSONObject params = getInteractionParams(BotType.INSIGHT_FACE, nonce);
		JSONObject commandData = getCommandData(BotType.INSIGHT_FACE, action);
		commandData.put("options", new JSONArray()
				.put(new JSONObject().put("type", 11).put("name", "image").put("value", 0))
				.put(new JSONObject().put("type", 3).put("name", "idname").put("value", name))
		);
		commandData.put("attachments", new JSONArray().put(new JSONObject().put("id", "0").put("filename", fileName).put("uploaded_filename", finalFileName)));
		params.put("data", commandData).put("analytics_location", "slash_ui");
		return postInteractionAndCheckStatus(params, action);
	}

	private JSONObject getInteractionParams(BotType botType, String nonce) {
		String sessionId = this.account.getSessionId();
		if (CharSequenceUtil.isBlank(sessionId)) {
			sessionId = DEFAULT_SESSION_ID;
		}
		return new JSONObject().put("type", 2)
				.put("guild_id", this.account.getGuildId())
				.put("channel_id", this.account.getChannelId())
				.put("application_id", botType.getValue())
				.put("session_id", sessionId)
				.put("nonce", nonce);
	}

	private JSONObject getCommandData(BotType botType, String name) {
		String key = botType.getValue() + ":" + name;
		if (!this.commandMap.containsKey(key)) {
			throw new UnsupportedOperationException("Unsupported Command: " + botType.name() + "-" + name);
		}
		ApplicationCommand command = this.commandMap.get(key);
		return new JSONObject().put("type", 1)
				.put("id", command.getId())
				.put("name", command.getName())
				.put("version", command.getVersion());
	}

	private void putFile(String uploadUrl, DataUrl dataUrl) {
		uploadUrl = this.discordHelper.getDiscordUploadUrl(uploadUrl);
		HttpHeaders headers = new HttpHeaders();
		headers.add("User-Agent", this.account.getUserAgent());
		headers.setContentType(MediaType.valueOf(dataUrl.getMimeType()));
		headers.setContentLength(dataUrl.getData().length);
		HttpEntity<byte[]> requestEntity = new HttpEntity<>(dataUrl.getData(), headers);
		this.restTemplate.put(uploadUrl, requestEntity);
	}

	private ResponseEntity<String> postJson(String url, String paramsStr) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", this.account.getUserToken());
		headers.set("User-Agent", this.account.getUserAgent());
		HttpEntity<String> httpEntity = new HttpEntity<>(paramsStr, headers);
		return this.restTemplate.postForEntity(url, httpEntity, String.class);
	}

	private Message<Void> postInteractionAndCheckStatus(JSONObject params, String name) {
		String key = IdUtil.randomUUID();
		this.interactionQueues.add(new NamedRunnable(name) {
			@Override
			public void run() {
				Message<Void> result = postJsonAndCheckStatus(DiscordServiceImpl.this.discordInteractionUrl, params.toString());
				AsyncLockUtils.LockObject lock = AsyncLockUtils.getLock(key);
				if (lock != null) {
					lock.setProperty("result", result);
					lock.awake();
				}
			}
		});
		try {
			AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock(key, Duration.ofMinutes(2));
			return lock.getPropertyGeneric("result");
		} catch (TimeoutException e) {
			return Message.failure("请求discord超时");
		}
	}

	private Message<Void> postJsonAndCheckStatus(String url, String paramsStr) {
		try {
			ResponseEntity<String> responseEntity = postJson(url, paramsStr);
			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return Message.success();
			}
			return Message.of(responseEntity.getStatusCodeValue(), CharSequenceUtil.sub(responseEntity.getBody(), 0, 100));
		} catch (HttpStatusCodeException e) {
			return convertHttpStatusCodeException(e);
		} catch (Exception e) {
			return Message.failure(e.getMessage());
		}
	}

	private Message<Void> convertHttpStatusCodeException(HttpStatusCodeException e) {
		try {
			JSONObject error = new JSONObject(e.getResponseBodyAsString());
			return Message.of(error.optInt("code", e.getRawStatusCode()), error.optString("message"));
		} catch (Exception je) {
			return Message.of(e.getRawStatusCode(), CharSequenceUtil.sub(e.getMessage(), 0, 100));
		}
	}
}
