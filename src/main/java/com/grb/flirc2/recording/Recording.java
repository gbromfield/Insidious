package com.grb.flirc2.recording;

import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.grb.flirc2.Protocol;

public class Recording {
	public Protocol protocol;
	public Integer port = null;
	public String recordingURL;
	public RecordingElement[] elements;

	final static GsonBuilder gsonBuilder = new GsonBuilder();
	final static Gson gson;
	
	static {
	    gsonBuilder.registerTypeAdapter(Recording.class, new RecordingDeserializer());
	    gson = gsonBuilder.create();
	}
	
	static public Recording parseString(String captureJsonString) {
	    return gson.fromJson(captureJsonString, Recording.class);
	}
	
	static public Recording parseFile(String captureJsonFilename) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		return gson.fromJson(new FileReader(captureJsonFilename), Recording.class);
	}
	
	static public void main(String[] args) {
		parseString("{\"tl1port\": 12346, \"recording\": [{\"protocol\": \"tl1\", \"timestamp\": \"1992-01-01 12:12:11.000\", \"tcpServer\": \"start\"},{\"protocol\": \"tl1\",\"timestamp\": \"1992-01-01 12:12:12.000\",\"input\": \"ACT-USER...\"}]}");
		parseString("{\"tl1port\": 12346}");
		parseString("{\"tl1port\": 12346, \"recording\": []}");
		try {
			Recording c = parseFile("./samples/sample1.json");
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
