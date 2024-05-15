package com.github.novicezk.midjourney.enums;


public enum BotType {

	MID_JOURNEY("936929561302675456"),

	NIJI_JOURNEY("1022952195194359889"),

	INSIGHT_FACE("1090660574196674713");

	private final String value;

	BotType(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

}
