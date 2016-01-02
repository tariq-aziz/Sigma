package com.samczsun.sigma.commons;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SigmaInputStream extends DataInputStream {
	
	public SigmaInputStream(InputStream in) {
		super(in);
	}
	
	public String readString() throws IOException {
		return new String(readDefinedBytes(), StandardCharsets.UTF_8);
	}
	
	public Action readAction() throws IOException {
		return Action.valueOf(readString().toUpperCase());
	}
	
	public UDPAction readUDPAction() throws IOException {
		int action = read();
		if (action > 0 && action < UDPAction.values().length) {
			return UDPAction.values()[action];
		}
		throw new IllegalStateException("Unknown UDP Action " + action);
	}
	
	public byte[] readDefinedBytes() throws IOException {
		int length = readInt();
		byte[] data = new byte[length];
		read(data);
		return data;
	}
}
