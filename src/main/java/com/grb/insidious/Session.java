package com.grb.insidious;

public interface Session {
	public Protocol getProtocol();
	public String getId();
	public int getPort();
	public String getClient();
	public String getSource();
	public void close();
}
