package com.samczsun.sigma.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.samczsun.sigma.client.controller.LoginController;
import com.samczsun.sigma.client.controller.MessagingController;
import com.samczsun.sigma.client.controller.RootController;
import com.samczsun.sigma.client.utils.Events;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ClientMain extends Application {
	private Stage mainStage;
	private VBox mainScreen;

	private RootController rootController;
	private LoginController loginController;
	private MessagingController messagingController;
	private ServerHandler connection;
	private UDPHandler udp;

	private Properties rootProperties;

	public static void main(String[] args) throws Exception {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		String dir = System.getProperty("user.home");
		if (dir == null) {
			// What to do?
		}
		File dataDir = new File(dir + File.separator + ".Sigma");
		if (!dataDir.exists()) {
			dataDir.mkdirs();
		}
		File propertiesFile = new File(dataDir, "sigma.properties");
		if (!propertiesFile.exists()) {
			propertiesFile.createNewFile();
		}
		this.rootProperties = new Properties();
		rootProperties.load(new FileInputStream(propertiesFile));
		this.mainStage = primaryStage;
		this.mainStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent e) {
				mainStage.setIconified(true);
				e.consume();
			}
		});
		FXMLLoader mainLoader = new FXMLLoader(ClientMain.class.getResource("/MainScreen.fxml"));
		Scene mainScene = new Scene(mainLoader.load());
		primaryStage.setScene(mainScene);
		this.mainScreen = (VBox) ((AnchorPane) mainScene.getRoot()).getChildren().get(0);
		this.rootController = mainLoader.getController();
		this.rootController.setInstance(this);

		FXMLLoader loginLoader = new FXMLLoader(ClientMain.class.getResource("/Login.fxml"));
		Parent p = loginLoader.load();
		showScreen(p);
		primaryStage.setResizable(false);
		this.loginController = loginLoader.getController();
		Events.on(this.loginController.buttonSignIn, ActionEvent.class).then((event) -> {
			if (this.loginController.usernameField.getText().trim().isEmpty()) {
				this.loginController.messageField.setText("Username is empty");
			} else {
				login(this.loginController.usernameField.getText());
			}
		});
		if (rootProperties.containsKey("username")) {
			this.login(rootProperties.getProperty("username"));
		}
		// loginScene.getStylesheets().add(Main.class.getResource("application.css").toExternalForm());
		primaryStage.show();
	}

	public void showScreen(Parent p) {
		if (this.mainScreen.getChildren().size() == 1) {
			this.mainScreen.getChildren().add(p);
		} else {
			this.mainScreen.getChildren().set(1, p);
		}
	}

	public void login(String username) {
		if (this.connection == null) {
			try {
				this.connection = new ServerHandler(this);
				this.udp = new UDPHandler();
			} catch (IOException e) {
				e.printStackTrace();
				this.loginController.messageField.setText(e.getMessage());
			}
			if (this.connection != null) {
				this.loginController.messageField.setText(null);
				new Thread(() -> {
					try {
						connection.login(username, (result, message) -> {
							if (!result) {
								loginController.messageField.setText(message);
							} else {
								this.rootProperties.setProperty("username", username);
								Platform.runLater(() -> {
									startMessages();
								});
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				} , "Login Thread").start();
			}
		}
	}

	public void startMessages() {
		try {
			FXMLLoader loader = new FXMLLoader(ClientMain.class.getResource("/MessagingScreen.fxml"));
			Parent p = loader.load();
			this.showScreen(p);
			this.messagingController = loader.getController();
			this.messagingController.setInstance(this);
			// loginScene.getStylesheets().add(Main.class.getResource("application.css").toExternalForm());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void logout() {
		if (this.connection != null) {
			this.connection.logout();
		}
		System.exit(0);
	}

	public Stage getStage() {
		return this.mainStage;
	}

	public String getUsername() {
		return this.connection.getUsername();
	}

	public ServerHandler getConnection() {
		return this.connection;
	}

	public MessagingController getMessagingController() {
		return this.messagingController;
	}
}