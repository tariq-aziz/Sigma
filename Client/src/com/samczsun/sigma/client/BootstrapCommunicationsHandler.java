package com.samczsun.sigma.client;
import java.io.EOFException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.samczsun.sigma.commons.Action;
import com.samczsun.sigma.commons.SigmaInputStream;
import com.samczsun.sigma.commons.SigmaOutputStream;

public class BootstrapCommunicationsHandler extends Thread {
	public static final BlockingQueue<String> messages = new ArrayBlockingQueue<String>(10);

	private SigmaInputStream input;
	private SigmaOutputStream output;
	private ClientMain main;

	public BootstrapCommunicationsHandler(ClientMain main, SigmaInputStream input, SigmaOutputStream output) {
		this.main = main;
		this.input = input;
		this.output = output;
	}

	/*public void run() {
		while (true) {
			try {
				int query = input.readInt();
				if (query == Action.CHAT_REQUESTED) {
					// final String username = input.readString();
					// Main.instance.todo.add(new Runnable() {
					// public void run() {
					// try {
					// VBox dialogVbox = new VBox(20);
					// final Stage dialog = new Stage();
					// dialog.initModality(Modality.APPLICATION_MODAL);
					// dialog.initOwner(Main.instance.getStage());
					// Label enterKey = new Label("Contact request from: " +
					// username);
					// Button btn = new Button("Accept");
					// Button btn2 = new Button("Decline");
					// btn.setOnAction(new EventHandler<ActionEvent>() {
					// public void handle(ActionEvent arg0) {
					// dialog.close();
					//
					// }
					// });
					//
					// btn2.setOnAction(new EventHandler<ActionEvent>() {
					// public void handle(ActionEvent arg0) {
					// dialog.close();
					//
					// }
					// });
					// dialogVbox.getChildren().addAll(enterKey, btn, btn2);
					// dialogVbox.setPadding(new Insets(10.0));
					// Scene dialogScene = new Scene(dialogVbox, 300, 300);
					// dialog.setScene(dialogScene);
					// dialog.show();

					String ipinfo = input.readString();
					System.out.println("Chat requested! Other user is " + ipinfo);
					String ip = ipinfo.split(":")[0];
					int port = Integer.parseInt(ipinfo.split(":")[1]);
					String user = ipinfo.split(":")[2];

					main.todo.add(new Runnable() {
						public void run() {
							main.addContact(new Contact(user, null));
						}
					});

					Thread readThread = new Thread() {
						public void run() {
							try {
								DatagramSocket serverSocket = new DatagramSocket(main.bootstrapConnection.getLocalPort() + 50);
								System.out.printf("Listening on udp %s\n", serverSocket.getLocalPort());
								while (true) {
									byte[] receiveData = new byte[1024];

									DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
									serverSocket.receive(receivePacket);
									main.todo.add(new Runnable() {
										public void run() {
											main.newMessage(new String(receiveData, 0, receivePacket.getLength()), user);
										}
									});
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};

					Thread writeThread = new Thread() {
						public void run() {
							try {
								DatagramSocket clientSocket = new DatagramSocket();
								while (true) {
									String message = messages.take();
									byte[] data = message.getBytes();
									DatagramPacket packet = new DatagramPacket(data, 0, data.length, InetAddress.getByName(ip), port + 50);
									clientSocket.send(packet);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};

					readThread.start();
					writeThread.start();
					// } catch (Exception e) {
					// e.printStackTrace();
					// }
					// }
					// });
				} else if (query == Action.ChatResponse.USER_FOUND || query == Action.ChatResponse.USER_NOT_FOUND) {
					Contact targetContact = main.contacts.get(input.readString());
					if (query == Action.ChatResponse.USER_FOUND) {
						String ipinfo = input.readString();
						System.out.println("Found user! Other user is " + ipinfo);
						String ip = ipinfo.split(":")[0];
						int port = Integer.parseInt(ipinfo.split(":")[1]);

						Thread readThread = new Thread() {
							public void run() {
								try {
									DatagramSocket serverSocket = new DatagramSocket(Main.instance.bootstrapConnection.getLocalPort() + 50);
									System.out.printf("Listening on udp %s\n", serverSocket.getLocalPort());
									while (true) {
										byte[] receiveData = new byte[1024];

										DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
										serverSocket.receive(receivePacket);
										main.todo.add(new Runnable() {
											public void run() {
												main.newMessage(new String(receiveData, 0, receivePacket.getLength()), targetContact.name);
											}
										});
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						};

						Thread writeThread = new Thread() {
							public void run() {
								try {
									DatagramSocket clientSocket = new DatagramSocket();
									while (true) {
										String message = messages.take();
										System.out.println("Sending: " + message);
										byte[] data = message.getBytes();
										DatagramPacket packet = new DatagramPacket(data, 0, data.length, InetAddress.getByName(ip), port + 50);
										clientSocket.send(packet);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						};

						readThread.start();
						writeThread.start();

						Main.instance.todo.add(new Runnable() {
							public void run() {
								Main.instance.addContact(targetContact);
							}
						});
					} else {
						System.out.println("User not found");
						Main.instance.todo.add(new Runnable() {
							public void run() {
								Main.instance.removeContact(targetContact);
							}
						});
					}
				}
			} catch (EOFException e) {
				break;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}*/
}
