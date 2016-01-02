package com.samczsun.sigma.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.samczsun.sigma.commons.Action;
import com.samczsun.sigma.commons.SocketConnection;

public class ClientHandler extends SocketConnection {

	private String username;

	private InetSocketAddress internalAddress;
	private InetSocketAddress externalAddress;

	public ClientHandler(Socket internalSocket) throws IOException {
		super(internalSocket);
		on(Action.LOGIN, this::onLogin);
		on(Action.BROADCAST, this::onBroadcast);

		System.out.println(String.format("New connection from %s:%s", internalSocket.getInetAddress(), internalSocket.getPort()));
		this.openStream();
	}

	public void onLogin(List<String> messages) {
		this.username = messages.get(0);
		log("Login request");
		prepare(Action.LOGIN_SUCCESSFUL).on(Action.INTERNAL_IP_INFO, (ipinfo) -> {
			try {
				internalAddress = new InetSocketAddress(InetAddress.getByName(ipinfo.get(0)), Integer.parseInt(ipinfo.get(1)));
				externalAddress = new InetSocketAddress(getSocket().getInetAddress(), getSocket().getPort());
				log("External IP: %s", externalAddress);
				log("Internal IP: %s", internalAddress);
				if (!ServerMain.clients.containsKey(username)) {
					ServerMain.clients.put(username, new CopyOnWriteArrayList<ClientHandler>());
				}
				ServerMain.clients.get(username).add(this);
				emit(Action.EXTERNAL_IP_INFO, this.externalAddress.getAddress().getHostAddress(), this.externalAddress.getPort());
			} catch (UnknownHostException e) {
				log("Could not parse internal IP: %s", ipinfo);
			}
		}).emit();
	}

	public void onBroadcast(List<String> messages) {
		log("Broadcast length: " + messages.get(0).getBytes(StandardCharsets.UTF_8).length);
		for (List<ClientHandler> list : ServerMain.clients.values()) {
			for (ClientHandler connection : list) {
				if (connection != this) {
					connection.emit(Action.BROADCAST, messages.get(0));
				}
			}
		}
	}

	public void onDisconnect() {
		if (isLoggedIn()) {
			log("Connection closed");
			ServerMain.clients.get(username).remove(this);
		}
	}

	public void log(String message, Object... data) {
		System.out.println(String.format("[%s] ", username) + String.format(message, data));
	}

	public InetSocketAddress getInternalAddress() {
		return this.internalAddress;
	}

	public InetSocketAddress getExternalAddress() {
		return this.externalAddress;
	}

	public String getUsername() {
		return this.username;
	}

	public boolean isLoggedIn() {
		return this.username != null;
	}
}
