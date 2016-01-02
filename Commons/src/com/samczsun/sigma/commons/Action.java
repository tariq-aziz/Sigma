package com.samczsun.sigma.commons;

public enum Action {
	LOGIN(1), LOGIN_SUCCESSFUL,
	
	LOGOUT,
	
	INTERNAL_IP_INFO(2),
	EXTERNAL_IP_INFO(2),
	
	BROADCAST(1),
	UDPINFO(2),
	
	CONTACT_REQUEST, CHAT_REQUEST,
	
	USER_FOUND, USER_NOT_FOUND, CHAT_ACCEPTED, CHAT_DENIED;

	private int datalen;

	Action() {
		this(0);
	}

	Action(int datalen) {
		this.datalen = datalen;
	}

	public int getDataLength() {
		return this.datalen;
	}
}
