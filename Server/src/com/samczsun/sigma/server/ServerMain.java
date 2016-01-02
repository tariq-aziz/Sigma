package com.samczsun.sigma.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.samczsun.sigma.commons.Action;

public class ServerMain {

	public static final Map<String, List<ClientHandler>> clients = new ConcurrentHashMap<>();

	public static void main(String[] args) throws Exception {
		new Thread(() -> {
			try {
				System.out.println("Starting TCP server on port 4600");
				ServerSocket socket = new ServerSocket(4600);
				while (true) {
					Socket clientSocket = socket.accept();
					new Thread(() -> {
						try {
							new ClientHandler(clientSocket);
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}).start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
		new Thread(() -> {
			try {
				System.out.println("Starting UDP server on port 4660");
				DatagramSocket serverSocket = new DatagramSocket(4660);
				while (true) {
					DatagramPacket receivePacket = new DatagramPacket(new byte[4096], 4096);
					serverSocket.receive(receivePacket);
					new Thread(() -> {
						String[] data = new String(receivePacket.getData(), 0, receivePacket.getLength()).split("~");
						System.out.println("Received UDP data from " + receivePacket.getAddress() + "/" + receivePacket.getPort() + ": " + Arrays.toString(data));
						int port = receivePacket.getPort();
						String nonce = data[0];
						String username = data[1];
						List<ClientHandler> connections = clients.get(username);
						for (ClientHandler con : connections) {
							if (con.getExternalAddress().getAddress().equals(receivePacket.getAddress())) {
								con.emit(Action.UDPINFO, nonce, String.valueOf(port));
								System.out.println("Emitting for " + Arrays.toString(data));
							}
						}
					}).start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
}
