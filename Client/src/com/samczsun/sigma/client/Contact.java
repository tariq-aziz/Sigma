package com.samczsun.sigma.client;

import java.security.PublicKey;

public class Contact {
	private String displayName;
	private PublicKey key;
	
	public Contact(String displayName, PublicKey theirKey) {
		this.displayName = displayName;
		this.key = theirKey;
	}
}
