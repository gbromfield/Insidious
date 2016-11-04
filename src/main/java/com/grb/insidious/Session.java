package com.grb.insidious;

import com.grb.insidious.recording.Recording;

import java.io.IOException;

public interface Session {
	public Protocol getProtocol();
	public String getId();
	public int getPort();
	public void close();
	public void setRecording(Recording recording) throws Exception;
}
