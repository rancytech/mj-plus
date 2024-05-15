package com.github.novicezk.midjourney.support;


import lombok.Data;

@Data
public class ApplicationCommand {
	private String id;
	private String name;
	private String version;
	private String applicationId;
	private String description;

	public ApplicationCommand(String id, String name, String version, String applicationId, String description) {
		this.id = id;
		this.name = name;
		this.version = version;
		this.applicationId = applicationId;
		this.description = description;
	}
}
