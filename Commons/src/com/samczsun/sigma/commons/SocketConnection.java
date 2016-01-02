package com.samczsun.sigma.commons;

import java.io.IOException;
import java.net.Socket;

public abstract class SocketConnection extends Connection {
	private Socket internalSocket;

	public SocketConnection(Socket socket) throws IOException {
		super(socket.getInputStream(), socket.getOutputStream());
		this.internalSocket = socket;
	}

	public Socket getSocket() {
		return this.internalSocket;
	}
}
