package com.github.novicezk.midjourney;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Constants {
	// 任务扩展属性 start
	public static final String TASK_PROPERTY_NOTIFY_HOOK = "notifyHook";
	public static final String TASK_PROPERTY_FINAL_PROMPT = "finalPrompt";
	public static final String TASK_PROPERTY_FINAL_ZH_PROMPT = "finalZhPrompt";
	public static final String TASK_PROPERTY_MESSAGE_ID = "messageId";
	public static final String TASK_PROPERTY_MESSAGE_HASH = "messageHash";
	public static final String TASK_PROPERTY_PROGRESS_MESSAGE_ID = "progressMessageId";
	public static final String TASK_PROPERTY_FLAGS = "flags";
	public static final String TASK_PROPERTY_NONCE = "nonce";
	public static final String TASK_PROPERTY_CUSTOM_ID = "customId";
	public static final String TASK_PROPERTY_NEED_MODAL = "needModel";
	public static final String TASK_PROPERTY_MODAL_MESSAGE_ID = "modalMessageId";
	public static final String TASK_PROPERTY_MODAL_CUSTOM_ID = "modalCustomId";
	public static final String TASK_PROPERTY_MODAL_PROMPT_CUSTOM_ID = "modalPromptCustomId";
	public static final String TASK_PROPERTY_DISCORD_INSTANCE_ID = "discordInstanceId";
	public static final String TASK_PROPERTY_DISCORD_BOT_TYPE = "botType";
	public static final String TASK_PROPERTY_MESSAGE_CONTENT = "messageContent";
	public static final String TASK_PROPERTY_IMAGE_SEED = "imageSeed";
	public static final String TASK_PROPERTY_DISCORD_CHANNEL_ID = "discordChannelId";
	// 任务扩展属性 end

	public static final String API_SECRET_HEADER_NAME = "mj-api-secret";

	public static final String DEFAULT_DISCORD_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";

	public static final String MJ_MESSAGE_HANDLED = "mj_proxy_handled";

	public static final String ACCOUNT_PROPERTY_SUPPORT_APPLICATION_IDS = "supportApplicationIds";

}
