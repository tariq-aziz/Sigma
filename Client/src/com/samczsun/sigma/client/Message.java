package com.samczsun.sigma.client;

import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.geometry.Insets;

public class Message extends GridPane {
	TextArea content;
	Label user;
	Label timestamp;
	final double PADDING = 5.0;

	public Message(String textContent, String source, String time) {
		// this.setPadding(new Insets(PADDING));
		content = new TextArea(textContent);
		content.setWrapText(true);
		content.setEditable(false);
		content.setBackground(null);
		content.setPrefHeight(24);
		user = new Label(source);

		timestamp = new Label(time);
		this.add(content, 1, 1);
		this.add(user, 1, 0);
		this.add(timestamp, 0, 1);
		this.setHgap(PADDING);
		this.setVgap(PADDING);
	}

	public String getText() {
		return content.getText();
	}
}
