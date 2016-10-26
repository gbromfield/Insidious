package com.grb.insidious.recording;

public enum TCPServer {
	DISCONNECT,
	START,
	STOP;
	
	public static TCPServer valueOfIgnoreCase(String value) {
		TCPServer[] values = TCPServer.values();
		for(int i = 0; i < values.length; i++) {
			if (values[i].toString().equalsIgnoreCase(value)) {
				return values[i];
			}
		}
		throw new IllegalArgumentException("No enum constant com.grb.insidious.recording.TCPServer." + value);
	}
}
