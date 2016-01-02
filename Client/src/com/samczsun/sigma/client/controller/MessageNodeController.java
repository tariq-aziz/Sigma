package com.samczsun.sigma.client.controller;

import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class MessageNodeController {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("hh:mm a");

	@FXML
	private Label usernameAndTime;

	@FXML
	private TextArea message;

	private String username = "";
	private Date time = new Date();

	public void setUsername(String username) {
		if (!this.username.equals(username)) {
			this.username = username;
			updateUsernameAndTime();
		}
	}

	public void setDate(Date time) {
		if (!this.time.equals(time)) {
			this.time = time;
			updateUsernameAndTime();
		}
	}

	public void setMessage(String message) {
		Platform.runLater(() -> {
			this.message.setText(message);
		});
	}

	private void updateUsernameAndTime() {
		Platform.runLater(() -> {
			usernameAndTime.setText(new StringBuilder(username).append(" @ ").append(FORMAT.format(time)).toString());
		});
	}
}
