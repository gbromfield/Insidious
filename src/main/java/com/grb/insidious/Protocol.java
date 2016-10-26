package com.grb.insidious;

public enum Protocol {
	TL1;
	
	public static Protocol valueOfIgnoreCase(String value) {
		Protocol[] values = Protocol.values();
		for(int i = 0; i < values.length; i++) {
			if (values[i].toString().equalsIgnoreCase(value)) {
				return values[i];
			}
		}
		throw new IllegalArgumentException("No enum constant com.grb.insidious.recording.Protocol." + value);
	}
}
