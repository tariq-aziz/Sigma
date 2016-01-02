package com.samczsun.sigma.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.samczsun.sigma.commons.Action;
import com.samczsun.sigma.commons.Callbacks;
import com.samczsun.sigma.commons.SocketConnection;

public class ServerHandler extends SocketConnection {
	public static final String DEFAULT_SERVER_IP = "199.204.186.9";
	private static final int DEFAULT_SERVER_PORT = 4600;

	private String username;

	private AtomicBoolean shutdown = new AtomicBoolean(false);

	private InetSocketAddress internalAddress;
	private InetSocketAddress externalAddress;

	private KeyPair keypair;

	private Cipher encryptCipher;
	private Cipher decryptCipher;

	private ClientMain instance;

	public ServerHandler(ClientMain main) throws UnknownHostException, IOException {
		this(main, DEFAULT_SERVER_IP, DEFAULT_SERVER_PORT);
	}

	public ServerHandler(ClientMain main, String ip, String port) throws UnknownHostException, IOException {
		this(main, ip, Integer.parseInt(port));
	}

	public ServerHandler(ClientMain main, String ip, int port) throws UnknownHostException, IOException {
		super(new Socket(ip, port));
		this.instance = main;
		loadDefaultActions();
		this.openStream();
	}

	private void loadDefaultActions() {
		on(Action.BROADCAST, (messages) -> {
			System.out.println("Received broadcast " + messages);
			String[] encrypted = messages.get(0).split("~");
			try {
				this.decryptCipher.init(Cipher.DECRYPT_MODE, this.keypair.getPrivate());
				byte[] aeskey = this.decryptCipher.doFinal(DatatypeConverter.parseBase64Binary(encrypted[0]));

				SecretKey key = new SecretKeySpec(aeskey, 0, aeskey.length, "AES");
				String[] data = Utils.decryptAES(encrypted[1], key).split(":");
				String requestType = data[0];
				if (requestType.equals("CONTACTREQUEST")) {
					String username = data[1];
					String message = new String(DatatypeConverter.parseBase64Binary(data[2]), "UTF-8");
					PublicKey otherKey = Utils.parsePublicKey(data[3]);
					this.instance.getMessagingController().receivedContactRequest(username, message, otherKey);
				} else if (requestType.equals("CONTACTRESPONSE")) {
					String username = data[1];
					boolean accepted = Boolean.parseBoolean(new String(DatatypeConverter.parseBase64Binary(data[2]), "UTF-8"));
					String ip = null;
					if (accepted) {
						ip = new String(DatatypeConverter.parseBase64Binary(data[3]), "UTF-8");
					}
					this.instance.getMessagingController().receivedContactResponse(username, accepted, ip);
				} else if (requestType.equals("CONTACTINFO")) {
					String username = data[1];
					String ip = new String(DatatypeConverter.parseBase64Binary(data[2]), "UTF-8");
					this.instance.getMessagingController().updateIpInfo(username, ip);
				} else if (requestType.equals("UDPINFO")) {
					String username = data[1];
					int port = Integer.parseInt(new String(DatatypeConverter.parseBase64Binary(data[2]), "UTF-8"));
					this.instance.getMessagingController().handleClientUDPInfo(username, port);
				} else if (requestType.equals("MESSAGE")) {
					String username = data[1];
					String message = new String(DatatypeConverter.parseBase64Binary(data[2]), "UTF-8");
					this.instance.getMessagingController().insertMessageNode(username, message, new Date());
				}
			} catch (Exception e) {
				e.printStackTrace();
				// Probably not a message for us
			}
		});

		on(Action.UDPINFO, (messages) -> {
			this.instance.getMessagingController().handleServerUDPInfo(messages.get(0), Integer.parseInt(messages.get(1)));
		});
	}

	public void login(String username, Callbacks.Consumer<Boolean, String> onComplete) throws NoSuchAlgorithmException, NoSuchPaddingException {
		this.username = username;
		this.keypair = generateSessionKeypair(2048);
		this.encryptCipher = Cipher.getInstance("RSA");
		this.decryptCipher = Cipher.getInstance("RSA");
		System.out.println("Username: " + username);
		System.out.println("Public key: " + DatatypeConverter.printBase64Binary(this.keypair.getPublic().getEncoded()));
		System.out.println("Private key: " + DatatypeConverter.printBase64Binary(this.keypair.getPrivate().getEncoded()));
		prepare(Action.LOGIN, username).on(Action.LOGIN_SUCCESSFUL, (message) -> {
			System.out.println("Login successful. Transmitting ip info");
			try {
				prepare(Action.INTERNAL_IP_INFO, InetAddress.getLocalHost().getHostAddress(), this.getSocket().getLocalPort()).on(Action.EXTERNAL_IP_INFO, (callback) -> {
					try {
						this.internalAddress = new InetSocketAddress(InetAddress.getLocalHost(), this.getSocket().getLocalPort());
						this.externalAddress = new InetSocketAddress(InetAddress.getByName(callback.get(0)), Integer.parseInt(callback.get(1)));
						System.out.println("External ip: " + this.externalAddress);
						System.out.println("Internal ip: " + this.internalAddress);
						System.out.println("Completed login procedure");
						onComplete.accept(true, null);
					} catch (Exception e) {
						e.printStackTrace();
						onComplete.accept(false, e.getMessage());
					}
				}).emit();
			} catch (Throwable t) {
				onComplete.accept(false, t.getMessage());
				t.printStackTrace();
			}
		}).emit();
	}

	public void logout() {
		this.shutdown.set(true);
	}

	private KeyPair generateSessionKeypair(int size) throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(size);
		return keyPairGenerator.genKeyPair();
	}

	public InetSocketAddress getExternalAddress() {
		return this.externalAddress;
	}

	public InetSocketAddress getInternalAddress() {
		return this.internalAddress;
	}

	public PublicKey getPublicKey() {
		return this.keypair.getPublic();
	}

	public String getUsername() {
		return this.username;
	}

	@Override
	public void onDisconnect() {
		System.out.println("Died");
	}
}
