package com.grb.insidious;

import java.io.IOException;

public interface Session {
	public Protocol getProtocol();
	public String getId();
	public int getPort();
	public String getClient();
	public String getSource();
	public void start() throws IOException;
	public void close();
}
