package com.samczsun.sigma.commons;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SigmaOutputStream extends DataOutputStream {
	
	public SigmaOutputStream(OutputStream in) {
		super(in);
	}
	
	public void writeString(String str) throws IOException {
		byte[] data = str.getBytes(StandardCharsets.UTF_8);
		writeDefinedBytes(data);
	}
	
	public void writeAction(Action a) throws IOException {
		writeString(a.name());
	}
	
	public void writeDefinedBytes(byte[] data) throws IOException {
		writeInt(data.length);
		write(data);
	}
}
