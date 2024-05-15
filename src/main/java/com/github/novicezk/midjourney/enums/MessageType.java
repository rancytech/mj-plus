package com.github.novicezk.midjourney.enums;


public enum MessageType {
	/**
	 * 创建.
	 */
	CREATE,
	/**
	 * 修改.
	 */
	UPDATE,
	/**
	 * 删除.
	 */
	DELETE,
	/**
	 * 创建 Modal.
	 */
	MODAL_CREATE,
	/**
	 * 创建 Iframe Modal.
	 */
	IFRAME_MODAL_CREATE;

	public static MessageType of(String type) {
		return switch (type) {
			case "MESSAGE_CREATE" -> CREATE;
			case "MESSAGE_UPDATE" -> UPDATE;
			case "MESSAGE_DELETE" -> DELETE;
			case "INTERACTION_MODAL_CREATE" -> MODAL_CREATE;
			case "INTERACTION_IFRAME_MODAL_CREATE" -> IFRAME_MODAL_CREATE;
			default -> null;
		};
	}
}
