package com.github.novicezk.midjourney.support;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReflectUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.novicezk.midjourney.annotation.DisplayDate;
import com.github.novicezk.midjourney.domain.DomainObject;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DomainHelper {
	private final MessageSourceHelper messageSourceHelper;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public Map<String, Object> convertDomainVO(DomainObject domain) {
		String jsonStr;
		try {
			jsonStr = this.objectMapper.writeValueAsString(domain);
		} catch (JsonProcessingException e) {
			return new HashMap<>();
		}
		Map<String, Object> vo = new JSONObject(jsonStr).toMap();
		Map<String, String> displays = new HashMap<>();
		vo.put("displays", displays);
		Field[] fields = ReflectUtil.getFields(domain.getClass());
		for (Field field : fields) {
			if (field.getType().isEnum()) {
				Enum<?> anEnum = (Enum<?>) ReflectUtil.getFieldValue(domain, field);
				displays.put(field.getName(), anEnum == null ? "" : this.messageSourceHelper.getLocaleMessage(anEnum));
			} else if (field.isAnnotationPresent(DisplayDate.class)) {
				Object dateObj = ReflectUtil.getFieldValue(domain, field);
				Date date;
				if (dateObj instanceof Date date1) {
					date = date1;
				} else if (dateObj instanceof Long long1) {
					date = new Date(long1);
				} else {
					continue;
				}
				DisplayDate annotation = field.getAnnotation(DisplayDate.class);
				displays.put(field.getName(), DateUtil.format(date, annotation.format()));
			}
		}
		return vo;
	}
}
