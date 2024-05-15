package com.github.novicezk.midjourney.service.translate;


import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.service.TranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DeepLTranslateServiceImpl implements TranslateService {
	private static final String TRANSLATE_API = "https://api-free.deepl.com/v2/translate";

	private final String authKey;

	public DeepLTranslateServiceImpl(ProxyProperties.DeeplTranslateConfig translateConfig) {
		this.authKey = translateConfig.getAuthKey();
		if (!CharSequenceUtil.isAllNotBlank(this.authKey)) {
			throw new BeanDefinitionValidationException("mj.deepl-translate.auth-key未配置");
		}
	}

	@Override
	public String translate(String prompt, String targetLanguage) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Authorization", "DeepL-Auth-Key " + this.authKey);
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, Object> body = new HashMap<>();
			body.put("text", new String[]{prompt});
			body.put("target_lang", targetLanguage);
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
			RestTemplate restTemplate = new RestTemplate();

			ResponseEntity<String> response = restTemplate.exchange(
					TRANSLATE_API,
					HttpMethod.POST,
					entity,
					String.class);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(response.getBody());
			this._assertNotNull("response body", rootNode);
			JsonNode translationsNode = rootNode.get("translations");
			this._assertNotNull("translations", translationsNode);
			JsonNode firstTranslationNode = translationsNode.get(0);
			JsonNode translatedTextNode = firstTranslationNode.get("text");
			this._assertNotNull("translatedTextNode", translatedTextNode);
			return translatedTextNode.asText();
		} catch (Exception e) {
			log.warn("调用deepl翻译失败: {}", e.getMessage());
		}
		return prompt;
	}

	protected final void _assertNotNull(String paramName, Object src) {
		if (src == null) {
			throw new IllegalArgumentException(String.format("\"%s\" is null", paramName));
		}
	}
}
