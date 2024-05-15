package com.github.novicezk.midjourney.enums;


public enum BilledWay {
	/**
	 * 年付.
	 */
	YEARLY("yearly"),
	/**
	 * 月付.
	 */
	MONTHLY("monthly");

	private final String value;

	BilledWay(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public static BilledWay fromValue(String value) {
		for (BilledWay billedWay : BilledWay.values()) {
			if (billedWay.getValue().equals(value)) {
				return billedWay;
			}
		}
		return null;
	}
}
