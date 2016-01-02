package com.samczsun.sigma.client.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ContactNodeController {
	@FXML
	private Label nameLabel;
	
	private MessagingController parent;
	
	private PublicKey theirKey;
	private InetAddress theirAddress;

	private int port;
	
	public void setUsername(String username) {
		Platform.runLater(() -> {
			nameLabel.setText(username);
		});
	}
	
	public void clickContact() {
		parent.setActiveContact(nameLabel.getText());
	}
	
	public void setParent(MessagingController parent) {
		this.parent = parent;
	}
	
	public void setKey(PublicKey key) {
		this.theirKey = key;
	}
	
	public PublicKey getKey() {
		return this.theirKey;
	}
	
	public void setAddress(String ip) throws UnknownHostException {
		this.theirAddress = InetAddress.getByName(ip);
	}
	
	public InetAddress getAddress() {
		return this.theirAddress;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public int getPort() {
		return this.port;
	}
}
