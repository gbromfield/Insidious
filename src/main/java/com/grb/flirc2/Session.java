package com.grb.flirc2;

import com.grb.flirc2.recording.Recording;

public interface Session {
	public Protocol getProtocol();
	public String getId();
	public int getPort();
	public void close();
	public void setRecording(Recording recording) throws Exception;
	public String toJsonString();
}
