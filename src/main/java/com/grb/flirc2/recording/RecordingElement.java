package com.grb.flirc2.recording;

import java.util.Date;

import com.grb.flirc2.Protocol;

public class RecordingElement {
	public Protocol protocol;
	public Date timestamp;
	public String timestampStr;
	public String input;
	public String output;
	public Integer multiplicity;
	public TCPServer tcpserver;
	private String buffer = null;
	
	@Override
	public String toString() {
		if (buffer == null) {
			StringBuilder bldr = new StringBuilder();
			bldr.append("protocol=");
			bldr.append(protocol.toString());
			if (timestamp != null) {
				bldr.append(", timestamp=");
				bldr.append(timestampStr);
			}
			if (input != null) {
				bldr.append(", input=");
				bldr.append(input);
			}
			if (output != null) {
				bldr.append(", output=");
				bldr.append(output);
			}
			if (multiplicity != null) {
				bldr.append(", multiplicity=");
				bldr.append(multiplicity);
			}
			if (tcpserver != null) {
				bldr.append(", tcpserver=");
				bldr.append(tcpserver.toString());
			}
			buffer = bldr.toString();
		}
		return buffer;
	}
}
