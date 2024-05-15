package com.github.novicezk.midjourney.enums;


public enum AccountMode {

	RELAX("Relax"),

	FAST("Fast"),

	TURBO("Turbo");

	private final String value;

	AccountMode(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public static AccountMode fromValue(String value) {
		for (AccountMode mode : AccountMode.values()) {
			if (mode.getValue().equals(value)) {
				return mode;
			}
		}
		return null;
	}
}
