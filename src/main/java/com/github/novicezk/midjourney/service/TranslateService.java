package com.github.novicezk.midjourney.service;

import java.util.regex.Pattern;

public interface TranslateService {

	default String translateToEnglish(String prompt) {
		if (!containsChinese(prompt)) {
			return prompt;
		}
		return translate(prompt, "EN");
	}

	default String translateToChinese(String prompt) {
		return translate(prompt, "ZH");
	}

	String translate(String prompt, String targetLanguage);

	default boolean containsChinese(String prompt) {
		return Pattern.compile("[\u4e00-\u9fa5]").matcher(prompt).find();
	}

}
