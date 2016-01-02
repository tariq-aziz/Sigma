package com.samczsun.sigma.client.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

import com.samczsun.sigma.client.ClientMain;
import com.samczsun.sigma.client.Contact;
import com.samczsun.sigma.client.Utils;
import com.samczsun.sigma.client.utils.Events;
import com.samczsun.sigma.commons.Action;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MessagingController {

	@FXML
	private Button addContactButton;
	@FXML
	private Button sendButton;
	@FXML
	private VBox contactsPane;
	@FXML
	private VBox messagesPane;
	@FXML
	private TextArea currentMessage;

	private ClientMain instance;
	private Stage stage;

	private Map<String, ContactNodeController> allContacts = new ConcurrentHashMap<>();

	private Map<String, String> pendingUDPInfo = new ConcurrentHashMap<>();

	private Map<String, PublicKey> pendingContactRequests = new ConcurrentHashMap<>();

	private String currentContact;

	private DatagramSocket mainSocket; // TODO: Move to own handler

	public void clickSend(ActionEvent e) {
		if (this.currentContact != null) {
			ContactNodeController contact = this.allContacts.get(currentContact);
			String message = currentMessage.getText();
			if (contact != null && message != null && !message.trim().isEmpty()) {
				this.insertMessageNode(instance.getUsername(), message, new Date());
				this.currentMessage.setText(null);

				try {
					KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
					keyGenerator.init(128);
					SecretKey secretKey = keyGenerator.generateKey();

					Cipher publicKeyCipher = Cipher.getInstance("RSA");
					publicKeyCipher.init(Cipher.ENCRYPT_MODE, contact.getKey());
					String send = DatatypeConverter.printBase64Binary(publicKeyCipher.doFinal(secretKey.getEncoded()));

					String toEncrypt = "MESSAGE:" + instance.getUsername() + ":" + DatatypeConverter.printBase64Binary(message.getBytes(StandardCharsets.UTF_8));
					String encoded = Utils.encryptAES(toEncrypt, secretKey);
					String result = send + "~" + encoded;
					instance.getConnection().emit(Action.BROADCAST, result);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public void setActiveContact(String name) {
		this.currentContact = name;
	}

	public void clickAddContact(ActionEvent e) {
		if (stage == null) {
			try {
				FXMLLoader loader = new FXMLLoader(ClientMain.class.getResource("/AddContactPopup.fxml"));
				Scene scene = new Scene(loader.load());
				AddContactPopupController controller = loader.getController();
				Events.on(controller.buttonAdd, ActionEvent.class).then((event) -> {
					if (controller.usernameField.getText().isEmpty()) {
						controller.messageBox.setText("Username not specified");
					} else if (controller.publicKey.getText().isEmpty()) {
						controller.messageBox.setText("Public key not specified");
					} else {
						controller.messageBox.setText(null);
						sendContactRequest(controller.usernameField.getText(), controller.publicKey.getText(), controller.message.getText());
					}
				});
				stage = new Stage();
				stage.initModality(Modality.APPLICATION_MODAL);
				stage.initOwner(instance.getStage());
				stage.setScene(scene);
				stage.setResizable(false);
				stage.setOnCloseRequest((event) -> {
					stage = null;
				});
				stage.show();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
			this.stage.requestFocus();
		}
	}

	public void setInstance(ClientMain main) {
		this.instance = main;
	}

	public void sendContactRequest(String username, String publickey, String message) {
		this.stage.close();
		this.stage = null;

		try {
			PublicKey key = Utils.parsePublicKey(publickey);
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(128);
			SecretKey secretKey = keyGenerator.generateKey();

			Cipher publicKeyCipher = Cipher.getInstance("RSA");
			publicKeyCipher.init(Cipher.ENCRYPT_MODE, key);
			String send = DatatypeConverter.printBase64Binary(publicKeyCipher.doFinal(secretKey.getEncoded()));

			String toEncrypt = "CONTACTREQUEST:" + instance.getUsername() + ":" + DatatypeConverter.printBase64Binary(message.getBytes(StandardCharsets.UTF_8)) + ":" + DatatypeConverter.printBase64Binary(instance.getConnection().getPublicKey().getEncoded());
			String encoded = Utils.encryptAES(toEncrypt, secretKey);
			String result = send + "~" + encoded;
			instance.getConnection().emit(Action.BROADCAST, result);

			this.pendingContactRequests.put(username, key);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void receivedContactRequest(String username, String message, PublicKey theirKey) {
		Platform.runLater(() -> {
			try {
				Contact contact = new Contact(username, theirKey);
				Stage stage = new Stage();
				FXMLLoader loader = new FXMLLoader(ClientMain.class.getResource("/ContactReceivedPopup.fxml"));
				Scene scene = new Scene(loader.load());
				ContactReceivedPopupController controller = loader.getController();
				controller.message.setText(message);
				controller.usernameField.setText(username + " says");
				Events.on(controller.accept, ActionEvent.class).then((event) -> {
					stage.close();
					this.respondToContactRequest(username, theirKey, true);
				});
				Events.on(controller.deny, ActionEvent.class).then((event) -> {
					stage.close();
					this.respondToContactRequest(username, theirKey, false);
				});
				stage.initModality(Modality.APPLICATION_MODAL);
				stage.initOwner(instance.getStage());
				stage.setScene(scene);
				stage.setResizable(false);
				stage.show();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public void receivedContactResponse(String username, boolean accepted, String ip) {
		if (accepted) {
			PublicKey theirKey = this.pendingContactRequests.remove(username);
			if (theirKey != null) {
				this.insertContactNode(username, theirKey);
				while (this.allContacts.get(username) == null)
					;
				this.updateIpInfo(username, ip);

				try {
					SecretKey secretKey = Utils.generateSecretKey();
					Cipher publicKeyCipher = Cipher.getInstance("RSA");
					publicKeyCipher.init(Cipher.ENCRYPT_MODE, theirKey);
					String send = DatatypeConverter.printBase64Binary(publicKeyCipher.doFinal(secretKey.getEncoded()));

					String toEncrypt = "CONTACTINFO:" + instance.getUsername() + ":" + DatatypeConverter.printBase64Binary(instance.getConnection().getExternalAddress().getAddress().getHostAddress().getBytes(StandardCharsets.UTF_8));
					String encoded = Utils.encryptAES(toEncrypt, secretKey);
					String result = send + "~" + encoded;
					instance.getConnection().emit(Action.BROADCAST, result);
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				System.out.println("Someone accepted your request but you didn't send one?");
			}
		} else {
			System.out.println("Oh noes :( " + username + " denied your contact request");
		}
	}

	public void respondToContactRequest(String username, PublicKey theirKey, boolean accept) {
		try {
			SecretKey secretKey = Utils.generateSecretKey();
			Cipher publicKeyCipher = Cipher.getInstance("RSA");
			publicKeyCipher.init(Cipher.ENCRYPT_MODE, theirKey);
			String send = DatatypeConverter.printBase64Binary(publicKeyCipher.doFinal(secretKey.getEncoded()));

			String message = Boolean.toString(accept);

			String toEncrypt = "CONTACTRESPONSE:" + instance.getUsername() + ":" + DatatypeConverter.printBase64Binary(message.getBytes(StandardCharsets.UTF_8));
			if (accept) {
				toEncrypt += (":" + DatatypeConverter.printBase64Binary(this.instance.getConnection().getExternalAddress().getAddress().getHostAddress().getBytes(StandardCharsets.UTF_8)));
			}
			String encoded = Utils.encryptAES(toEncrypt, secretKey);
			String result = send + "~" + encoded;
			instance.getConnection().emit(Action.BROADCAST, result);

			if (accept) {
				this.insertContactNode(username, theirKey);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateIpInfo(String username, String ip) {
		ContactNodeController controller = this.allContacts.get(username.toLowerCase());
		if (controller != null) {
			try {
				controller.setAddress(ip);
				String nonce = Utils.randomString(32);
				this.pendingUDPInfo.put(nonce, username);
				DatagramSocket clientSocket = new MulticastSocket();
				byte[] data = (nonce + "~" + instance.getUsername()).getBytes(StandardCharsets.UTF_8);
				DatagramPacket packet = new DatagramPacket(data, 0, data.length, InetAddress.getByName("199.204.186.9"), 4660);
				clientSocket.send(packet);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void handleServerUDPInfo(String nonce, int port) {
		String username = this.pendingUDPInfo.remove(nonce);
		System.out.println("Handling server UDP info with " + nonce + " and " + port + " and " + username);
		if (username != null) {
			try {
				ContactNodeController info = this.allContacts.get(username);
				SecretKey secretKey = Utils.generateSecretKey();
				Cipher publicKeyCipher = Cipher.getInstance("RSA");
				publicKeyCipher.init(Cipher.ENCRYPT_MODE, info.getKey());
				String send = DatatypeConverter.printBase64Binary(publicKeyCipher.doFinal(secretKey.getEncoded()));

				String message = Integer.toString(port);

				String toEncrypt = "UDPINFO:" + instance.getUsername() + ":" + DatatypeConverter.printBase64Binary(message.getBytes(StandardCharsets.UTF_8));
				String encoded = Utils.encryptAES(toEncrypt, secretKey);
				String result = send + "~" + encoded;
				instance.getConnection().emit(Action.BROADCAST, result);

				new Thread(() -> {
					try {
						System.out.println("Listening for UDP on " + port);
						while (true) {
							DatagramPacket receivePacket = new DatagramPacket(new byte[4096], 4096);
							serverSocket.receive(receivePacket);
							String data = new String(receivePacket.getData(), 0, receivePacket.getLength());
							System.out.println("Got data: " + data);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Random UDP info? nonce=" + nonce);
		}
	}

	public void handleClientUDPInfo(String username, int port) {
		System.out.println("Updating port of " + username + " to " + port);
		this.allContacts.get(username).setPort(port);
		try {
			System.out.println("Spamming UDP packets to " + username + "@" + allContacts.get(username).getAddress() + ":" + port);
			int packets = 0;
			while (packets < 10) {
				DatagramSocket clientSocket = this.allSockets.get(username);
				byte[] data = ("This is from the client").getBytes(StandardCharsets.UTF_8);
				DatagramPacket packet = new DatagramPacket(data, 0, data.length, this.allContacts.get(username).getAddress(), port);
				clientSocket.send(packet);
				packets++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertMessageNode(String username, String message, Date time) {
		if (message != null && !message.trim().isEmpty()) {
			Platform.runLater(() -> {
				try {
					FXMLLoader messageNodeLoader = new FXMLLoader(ClientMain.class.getResource("/MessageNode.fxml"));
					Parent p = messageNodeLoader.load();
					MessageNodeController controller = messageNodeLoader.getController();
					controller.setUsername(username);
					controller.setDate(time);
					controller.setMessage(message);
					this.messagesPane.getChildren().add(p);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	public void insertContactNode(String username, PublicKey key) {
		if (!this.allContacts.containsKey(username.toLowerCase())) {
			Platform.runLater(() -> {
				try {
					FXMLLoader messageNodeLoader = new FXMLLoader(ClientMain.class.getResource("/ContactNode.fxml"));
					Parent p = messageNodeLoader.load();
					ContactNodeController controller = messageNodeLoader.getController();
					controller.setUsername(username);
					controller.setParent(this);
					controller.setKey(key);
					this.contactsPane.getChildren().add(p);
					this.allContacts.put(username.toLowerCase(), controller);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	public void onKeyPressed(KeyEvent e) {
		if (e.getTarget().equals(this.currentMessage) && e.getCode() == KeyCode.ENTER) {
			if (!e.isShiftDown()) {
				e.consume();
				this.clickSend(null);
			} else {
				this.currentMessage.setText(this.currentMessage.getText() + "\n");
				this.currentMessage.positionCaret(this.currentMessage.getLength());
			}
		}
	}
}
