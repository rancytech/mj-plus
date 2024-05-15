package com.github.novicezk.midjourney.enums;


public enum VariationLevel {

	LOW("Low"),

	HIGH("High");

	private final String value;

	VariationLevel(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public static VariationLevel fromValue(String value) {
		for (VariationLevel level : VariationLevel.values()) {
			if (level.getValue().equals(value)) {
				return level;
			}
		}
		return null;
	}
}
