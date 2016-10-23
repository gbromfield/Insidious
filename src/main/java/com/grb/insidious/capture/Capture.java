package com.grb.insidious.capture;

import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class Capture {
	public String name;
	public Integer tl1Port;
	public CaptureElement[] elements;

	final static GsonBuilder gsonBuilder = new GsonBuilder();
	final static Gson gson;
	
	static {
	    gsonBuilder.registerTypeAdapter(Capture.class, new CaptureDeserializer());
	    gson = gsonBuilder.create();
	}
	
	static public Capture parseString(String captureJsonString) {
	    return gson.fromJson(captureJsonString, Capture.class);
	}
	
	static public Capture parseFile(String captureJsonFilename) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		return gson.fromJson(new FileReader(captureJsonFilename), Capture.class);
	}
	
	static public void main(String[] args) {
		parseString("{\"tl1port\": 12346, \"capture\": [{\"protocol\": \"tl1\", \"timestamp\": \"1992-01-01 12:12:11.000\", \"tcpServer\": \"start\"},{\"protocol\": \"tl1\",\"timestamp\": \"1992-01-01 12:12:12.000\",\"input\": \"ACT-USER...\"}]}");
		parseString("{\"tl1port\": 12346}");
		parseString("{\"tl1port\": 12346, \"capture\": []}");
		try {
			Capture c = parseFile("./samples/sample1.json");
			System.out.println(c);
		} catch (JsonSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
