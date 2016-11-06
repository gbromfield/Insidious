package com.grb.flirc2.recording;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;

import com.grb.flirc2.Protocol;

public class Recording {
	private Protocol _protocol;
	private Integer _port;
	private ArrayList<String> _recordingURLs;
	private ArrayList<RecordingElement> _elements;

	public Recording() {
		_protocol = null;
		_port = null;
		_recordingURLs = null;
		_elements = null;
	}

	public Protocol getProtocol() {
		return _protocol;
	}

	public Integer getPort() {
		return _port;
	}

	public ArrayList<String> getRecordingURLs() {
		return _recordingURLs;
	}

	public ArrayList<RecordingElement> getRecordingElements() {
		return _elements;
	}

	public void addRecordingJson(RecordingJson recordingJson) {
		if (recordingJson.protocol != null) {
			if (_protocol == null) {
				_protocol = recordingJson.protocol;
			} else {
				if (!recordingJson.protocol.equals(_protocol)) {
					throw new IllegalArgumentException(
							String.format("Mismatched protocol, expected %s, got %s",
									_protocol.toString(), recordingJson.protocol.toString()));
				}
			}
		}
		if (recordingJson.port != null) {
			if (_port == null) {
				_port = recordingJson.port;
			} else {
				if (!recordingJson.port.equals(_port)) {
					throw new IllegalArgumentException(
							String.format("Mismatched port, expected %d, got %d",
									_port, recordingJson.port));
				}
			}
		}
		if (recordingJson.recordingURLs != null) {
			if (_recordingURLs == null) {
				_recordingURLs = new ArrayList<String>();
			}
			for(int i = 0; i < recordingJson.recordingURLs.length; i++) {
				_recordingURLs.add(recordingJson.recordingURLs[i]);
			}
		}
		if (recordingJson.elements != null) {
			if (_elements == null) {
				_elements = new ArrayList<RecordingElement>();
			}
			for(int i = 0; i < recordingJson.elements.length; i++) {
				_elements.add(recordingJson.elements[i]);
			}
		}
	}

	static public Recording parseString(String jsonStr) throws IOException {
		Recording recording = new Recording();
		parseString(recording, jsonStr);
		if (recording.getProtocol() == null) {
			throw new IllegalArgumentException("Recording has no protocol specified");
		}
		return recording;
	}

	static public void parseString(Recording recording, String jsonStr) throws IOException {
		RecordingJson recordingJson = RecordingJson.parseString(jsonStr);
		recording.addRecordingJson(recordingJson);
		if (recordingJson.recordingURLs != null) {
			for(int i = 0; i < recordingJson.recordingURLs.length; i++) {
				parseURL(recording, recordingJson.recordingURLs[i]);
			}
		}
	}

	static public void parseURL(Recording recording, String urlStr) throws IOException {
		BufferedReader in = null;
		try {
			URL recordingURL = new URL(urlStr);
			if (recordingURL != null) {
				in = new BufferedReader(
						new InputStreamReader(recordingURL.openStream()));

				String inputLine;
				StringBuilder bldr = new StringBuilder();
				while ((inputLine = in.readLine()) != null) {
					bldr.append(inputLine);
				}
				parseString(recording, bldr.toString());
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
}
