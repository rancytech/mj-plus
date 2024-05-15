package com.github.novicezk.midjourney.service;

import com.github.novicezk.midjourney.enums.BlendDimensions;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.result.Message;
import eu.maxschuster.dataurl.DataUrl;

import java.util.List;

public interface DiscordService {

	Message<Void> imagine(BotType botType, String prompt, String nonce);

	Message<Void> action(BotType botType, String messageId, int messageFlags, int componentType, String customId, String nonce);

	Message<Void> modal(BotType botType, String messageId, String customId, String promptCustomId, String prompt, String nonce);

	Message<Void> regionModal(String customId, String prompt, String maskBase64);

	Message<Void> describe(BotType botType, String finalFileName, String nonce);

	Message<Void> blend(BotType botType, List<String> finalFileNames, BlendDimensions dimensions, String nonce);

	Message<Void> shorten(BotType botType, String prompt, String nonce);

	Message<Void> cancel(BotType botType, String messageId, String nonce);

	Message<Void> info(BotType botType, String nonce);

	Message<Void> settings(BotType botType, String nonce);

	Message<Void> changeVersion(String version, String nonce);

	Message<Void> seed(String messageId, int messageFlags);

	Message<String> upload(String fileName, DataUrl dataUrl);

	default Message<String> sendImageMessage(String content, String finalFileName) {
		Message<List<String>> list = this.sendImageMessages(content, List.of(finalFileName));
		if (list.getCode() != 0) {
			return Message.of(list.getCode(), list.getDescription());
		}

		return Message.success(list.getResult().get(0));
	}

	Message<List<String>> sendImageMessages(String content, List<String> finalFileNames);

	Message<Void> saveInsightFace(String name, String finalFileName, String nonce);

	Message<Void> swapInsightFace(String name, String finalFileName, String nonce);

	Message<Void> delInsightFace(String name, String nonce);
}
