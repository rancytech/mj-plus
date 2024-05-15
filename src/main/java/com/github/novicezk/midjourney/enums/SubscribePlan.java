package com.github.novicezk.midjourney.enums;


public enum SubscribePlan {

	BASIC("Basic"),

	STANDARD("Standard"),

	PRO("Pro"),

	MEGA("Mega");

	private final String value;

	SubscribePlan(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public static SubscribePlan fromValue(String value) {
		for (SubscribePlan plan : SubscribePlan.values()) {
			if (plan.getValue().equals(value)) {
				return plan;
			}
		}
		return null;
	}

}
