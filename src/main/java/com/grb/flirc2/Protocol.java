package com.grb.flirc2;

public enum Protocol {
	TL1;
	
	public static Protocol valueOfIgnoreCase(String value) {
		Protocol[] values = Protocol.values();
		for(int i = 0; i < values.length; i++) {
			if (values[i].toString().equalsIgnoreCase(value)) {
				return values[i];
			}
		}
		throw new IllegalArgumentException(
				String.format("No enum constant %s.%s exists", Protocol.class.getName(), value));
	}
}
