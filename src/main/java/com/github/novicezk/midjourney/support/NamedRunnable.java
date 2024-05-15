package com.github.novicezk.midjourney.support;


import lombok.Getter;

@Getter
public abstract class NamedRunnable implements Runnable {

	private final String name;

	public NamedRunnable(String name) {
		this.name = name;
	}

}
