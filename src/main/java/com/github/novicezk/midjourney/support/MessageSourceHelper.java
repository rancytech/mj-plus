package com.github.novicezk.midjourney.support;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageSourceHelper {
	private final MessageSource messageSource;

	public String getLocaleMessage(Enum<?> obj) {
		return getLocaleMessage(obj, obj.name());
	}

	public String getLocaleMessage(Enum<?> obj, String defaultMsg) {
		return messageSource.getMessage(obj.getClass().getName() + "." + obj.name(), null, defaultMsg, LocaleContextHolder.getLocale());
	}

}
