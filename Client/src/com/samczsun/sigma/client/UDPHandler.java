package com.samczsun.sigma.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import com.samczsun.sigma.commons.Action;
import com.samczsun.sigma.commons.Callbacks;
import com.samczsun.sigma.commons.UDPAction;

public class UDPHandler {
	private DatagramSocket internalSocket;
	private Thread readThread;
	private Thread writeThread;

	private LinkedBlockingQueue<Entry<InetSocketAddress, byte[]>> messagesToWrite = new LinkedBlockingQueue<>();
	private Object writeLock = new Object();

	private Map<Action, Callbacks.Consumer<InetSocketAddress, byte[]>> actions = new HashMap<>();

	public UDPHandler() throws IOException {
		this.internalSocket = new MulticastSocket();

		this.readThread = new Thread(() -> {
			while (true) {
				try {
					DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
					this.internalSocket.receive(packet);
					new Thread(() -> {
						int action = packet.getData()[0];
						if (action > 0 && action < UDPAction.values().length) {
							UDPAction a = UDPAction.values()[action];
							byte[] remaining = new byte[packet.getLength() - 1];
							System.arraycopy(packet.getData(), 1, remaining, 0, remaining.length);
							Callbacks.Consumer<InetSocketAddress, byte[]> consumer = actions.get(a);
							if (consumer != null) {
								consumer.accept((InetSocketAddress) packet.getSocketAddress(), remaining);
							}
						}
					}).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		this.writeThread = new Thread(() -> {
			while (true) {
				try {
					Entry<InetSocketAddress, byte[]> message = messagesToWrite.take();
					DatagramPacket packet = new DatagramPacket(message.getValue(), 0, message.getValue().length, message.getKey());
					internalSocket.send(packet);
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void openStream() {
		this.readThread.start();
		this.writeThread.start();
	}

	public Message prepare(Action a, Object... messages) {
		return new Message(a, messages);
	}

	public void emit(Action a, Object... messages) {
		new Message(a, messages).emit();
	}

	public void emit(Action a) {
		new Message(a).emit();
	}

	public void on(Action a, Callbacks.Consumer<InetSocketAddress, byte[]> handler) {
		this.actions.put(a, handler);
	}

	public class Message {
		private Action action;
		private byte[] message;
		private Map<Action, Callbacks.Consumer<InetSocketAddress, byte[]>> handlers = new HashMap<>();

		public Message(Action a, byte[] message) {
			this.action = a;
			this.message = message;
		}

		public Message on(Action a, Callbacks.Consumer<InetSocketAddress, byte[]> handler) {
			this.handlers.put(a, handler);
			return this;
		}

		public void emit() {
			actions.putAll(handlers);
			synchronized (writeLock) {
				messagesToWrite.add(new SimpleEntry<InetSocketAddress, byte[]>(address, message));
			}
		}
	}

}
