package com.samczsun.sigma.client.controller;

import com.samczsun.sigma.client.ClientMain;

import javafx.application.Platform;
import javafx.event.ActionEvent;

public class RootController {
	private ClientMain instance;

	public void onExitClick(ActionEvent e) {
		instance.logout();
		Platform.exit();
	}

	public void setInstance(ClientMain main) {
		this.instance = main;
	}
}
