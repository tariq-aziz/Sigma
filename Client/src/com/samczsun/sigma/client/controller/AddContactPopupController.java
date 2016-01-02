package com.samczsun.sigma.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class AddContactPopupController {
	@FXML
	public TextField usernameField;
	@FXML
	public TextArea publicKey;
	@FXML
	public TextArea message;
	@FXML
	public Text messageBox;
	@FXML
	public Button buttonAdd;
}
