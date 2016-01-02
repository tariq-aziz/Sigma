package com.samczsun.sigma.commons;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public abstract class Connection {
	private SigmaInputStream input;
	private SigmaOutputStream output;

	private Thread readThread;
	private Thread writeThread;

	private LinkedBlockingQueue<String> messagesToWrite = new LinkedBlockingQueue<>();
	private Object writeLock = new Object();

	private Map<Action, Consumer<List<String>>> actions = new HashMap<>();

	public Connection(InputStream inputStream, OutputStream outputStream) throws IOException {
		this.input = new SigmaInputStream(inputStream);
		this.output = new SigmaOutputStream(outputStream);

		this.readThread = new Thread(() -> {
			while (true) {
				try {
					Action a = input.readAction();
					int datalen = a.getDataLength();
					List<String> messages = null;
					if (datalen == 0) {
						messages = Collections.emptyList();
					} else {
						messages = new ArrayList<>();
						for (int i = 0; i < datalen; i++) {
							messages.add(input.readString());
						}
					}
					Consumer<List<String>> consumer = actions.get(a);
					if (consumer != null) {
						consumer.accept(messages);
					}
				} catch (SocketException e) {
					onDisconnect();
					break;
				} catch (EOFException e) {
					onDisconnect();
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		this.writeThread = new Thread(() -> {
			while (true) {
				try {
					String message = messagesToWrite.take();
					output.writeString(message);
				} catch (SocketException e) {
					onDisconnect();
					break;
				} catch (EOFException e) {
					onDisconnect();
					break;
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

	public void on(Action a, Consumer<List<String>> handler) {
		this.actions.put(a, handler);
	}

	public class Message {
		private Action action;
		private List<String> args = new ArrayList<String>();
		private Map<Action, Consumer<List<String>>> handlers = new HashMap<>();

		public Message(Action a, Object... messages) {
			this.action = a;
			for (Object msg : messages) {
				this.args.add(msg.toString());
			}
		}

		public Message on(Action a, Consumer<List<String>> handler) { //TODO: Maybe this is a one-time action? Could be exploitable though (spam actions)
			this.handlers.put(a, handler);
			return this;
		}

		public void emit() {
			actions.putAll(handlers);
			synchronized (writeLock) {
				messagesToWrite.add(action.name());
				for (String message : args) {
					messagesToWrite.add(message.toString());
				}
			}
		}
	}

	public abstract void onDisconnect();
}
