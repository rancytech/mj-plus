package com.github.novicezk.midjourney.util;


import cn.hutool.core.text.CharSequenceUtil;

import java.util.Locale;


public final class NamingStrategyUtils {

	private NamingStrategyUtils() {
	}


	public static String convert(String name) {
		StringBuilder builder = new StringBuilder(CharSequenceUtil.replaceChars(name, new char[]{'.', '#', '-'}, "_"));
		for (int i = 1; i < builder.length() - 1; i++) {
			if (isUnderscoreRequired(builder.charAt(i - 1), builder.charAt(i), builder.charAt(i + 1))) {
				builder.insert(i++, '_');
			}
		}
		return builder.toString().toLowerCase(Locale.ROOT);
	}

	private static boolean isUnderscoreRequired(char before, char current, char after) {
		return Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after);
	}
}
