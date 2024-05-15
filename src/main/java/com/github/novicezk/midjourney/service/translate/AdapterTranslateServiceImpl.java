package com.github.novicezk.midjourney.service.translate;


import com.github.novicezk.midjourney.service.TranslateService;

public class AdapterTranslateServiceImpl implements TranslateService {
	private final TranslateService translateZh2EnService;
	private final TranslateService translateEn2ZhService;

	public AdapterTranslateServiceImpl(TranslateService translateZh2EnService, TranslateService translateEn2ZhService) {
		this.translateZh2EnService = translateZh2EnService;
		this.translateEn2ZhService = translateEn2ZhService;
	}

	@Override
	public String translate(String prompt, String targetLanguage) {
		if (targetLanguage.equals("EN")) {
			return translateZh2EnService.translateToEnglish(prompt);
		} else {
			return translateEn2ZhService.translateToChinese(prompt);
		}
	}

}
