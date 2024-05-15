package com.github.novicezk.midjourney.enums;


public enum StylizeLevel {

	LOW("low"),

	MED("med"),

	HIGH("high"),

	VERY_HIGH("very high");

	private final String value;

	StylizeLevel(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public static StylizeLevel fromValue(String value) {
		for (StylizeLevel stylizeLevel : StylizeLevel.values()) {
			if (stylizeLevel.getValue().equals(value)) {
				return stylizeLevel;
			}
		}
		return null;
	}
}
