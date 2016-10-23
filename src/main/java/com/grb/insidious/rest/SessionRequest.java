package com.grb.insidious.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SessionRequest {
	public SessionElement[] elements;
	
	final static GsonBuilder gsonBuilder = new GsonBuilder();
	final static Gson gson;

	static {
	    gsonBuilder.registerTypeAdapter(SessionRequest.class, new SessionRequestDeserializer());
	    gson = gsonBuilder.create();
	}

	static public SessionRequest parseString(String jsonString) {
	    return gson.fromJson(jsonString, SessionRequest.class);
	}
}
