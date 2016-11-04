package com.grb.flirc2.recording;

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
		throw new IllegalArgumentException(String.format("No enum constant %s.%s", TCPServer.class.getName(), value));
	}
}
