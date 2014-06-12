package edu.vanderbilt.batman.util;

import java.util.Random;


public class RandomValueGenerator {
	
	private static final String CHARSLOWERCASE = "abcdefghijklmnopqrstuvwxyz";
	private static final String CHARSUPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String NUMBERS = "1234567890";
	public enum domains { com, net, org, edu};
	
	private static Random r = new Random();
	
	public static final int TYPE_STRING = 0;
	public static final int TYPE_NUMBER = 1;
	public static final int TYPE_EMAIL = 2;	
	
	public static String generate(int length, int type) {
		String characters = "";
		if (type == TYPE_STRING) {
			characters = CHARSLOWERCASE + CHARSUPPERCASE;
		} else if (type == TYPE_NUMBER) {
			characters = NUMBERS;
		} else if (type == TYPE_EMAIL) {
			characters = CHARSLOWERCASE + CHARSUPPERCASE + "._";
		}
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int index = new Random().nextInt(characters.length());
			buf.append(characters.substring(index, index + 1));
		}
		return buf.toString();
	}
	
	public static String generateRandomString(int length) {
		return generate(length, TYPE_STRING);
	}
	
	public static String generateRandomNumber(int length) {
		return generate(length, TYPE_NUMBER);
	}
	
	public static String generateRandomEmail() {
		int len = r.nextInt(5) + 5;
		String username = generateRandomString(len);
		return generateRandomEmail(username);
	}
	
	public static String generateRandomEmail(String username) {
		StringBuilder builder = new StringBuilder();
		builder.append(username);
		builder.append("@");
		int len = r.nextInt(4) + 1;
		builder.append(generateRandomString(len));
		builder.append(".");
		int d = r.nextInt(domains.values().length);
		builder.append(domains.values()[d]);
		return builder.toString();
	}
	
	public static String generateRandomPhoneNum() {
		return generateRandomNumber(10);
	}
}
