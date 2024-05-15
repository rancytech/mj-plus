package com.github.novicezk.midjourney.util;


import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JsonUtils {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static String convertObject2JSON(Object object) {
		if (object == null) {
			return null;
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			log.error("parse object to json error", e);
			return null;
		}
	}

	public static <T> T convertJSON2Object(String json, Class<T> clazz) {
		if (CharSequenceUtil.isBlank(json)) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(json, clazz);
		} catch (Exception e) {
			log.error("parse json to object error", e);
			return null;
		}
	}

	public static <T> T convertJSON2Object(String json, TypeReference<T> typeReference) {
		if (CharSequenceUtil.isBlank(json)) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(json, typeReference);
		} catch (Exception e) {
			log.error("parse json to object error", e);
			return null;
		}
	}
}
