package com.samczsun.sigma.commons;

import java.io.ByteArrayOutputStream;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.DatatypeConverter;

public class KeyTest {
	public static void main(String[] args) throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		RSAPublicKey k = (RSAPublicKey) keyPairGenerator.generateKeyPair().getPublic();


		System.out.println(k.getEncoded().length);
		System.out.println(k.getModulus().toByteArray().length);
		System.out.println(DatatypeConverter.printBase64Binary(k.getEncoded()).length());
		System.out.println(DatatypeConverter.printBase64Binary(k.getModulus().toByteArray()).length());
		System.out.println(DatatypeConverter.printBase64Binary(k.getModulus().toByteArray()));
		
		System.out.println(k.getModulus().toString());
		System.out.println(DatatypeConverter.printBase64Binary(k.getModulus().toByteArray()));
	}
}
