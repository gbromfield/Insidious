package com.grb.flirc2.recording;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.grb.flirc2.Protocol;

import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by gbromfie on 11/4/16.
 */
public class RecordingJson {
    public Protocol protocol;
    public Integer port = null;
    public String[] recordingURLs;
    public RecordingElement[] elements;

    final static GsonBuilder gsonBuilder = new GsonBuilder();
    final static Gson gson;

    static {
        gsonBuilder.registerTypeAdapter(RecordingJson.class, new RecordingDeserializer());
        gson = gsonBuilder.create();
    }

    static public RecordingJson parseString(String captureJsonString) {
        return gson.fromJson(captureJsonString, RecordingJson.class);
    }

    static public RecordingJson parseFile(String captureJsonFilename) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
        return gson.fromJson(new FileReader(captureJsonFilename), RecordingJson.class);
    }
}
