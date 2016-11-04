package com.grb.flirc2.recording;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.grb.flirc2.Protocol;

public class RecordingDeserializer implements JsonDeserializer<Recording> {
	final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

	static public Recording deserializeRecording(JsonObject parentJsonObject) {
		Recording recording = new Recording();
		recording.elements = null;
		JsonObject jsonObject = null;
		final JsonElement recordingElements = parentJsonObject.get("recording");
		if (recordingElements != null) {
			final JsonArray captureElementsArray = recordingElements.getAsJsonArray();
			recording.elements = new RecordingElement[captureElementsArray.size()];
			for (int i = 0; i < recording.elements.length; i++) {
				recording.elements[i] = new RecordingElement();
				final JsonElement captureElement = captureElementsArray.get(i);
				jsonObject = captureElement.getAsJsonObject();
				final JsonElement protocol = jsonObject.get("protocol");
				if (protocol == null) {
					throw new IllegalArgumentException("Protocol is mandatory in recording element");
				}
				recording.elements[i].protocol = Protocol.valueOfIgnoreCase(protocol.getAsString());
				final JsonElement timestamp = jsonObject.get("timestamp");
				if (timestamp != null) {
					try {
						recording.elements[i].timestampStr = timestamp.getAsString();
						recording.elements[i].timestamp = dateFormatter.parse(timestamp.getAsString());
					} catch (ParseException e) {
						throw new IllegalArgumentException(String.format("Error parsing timestamp \"%s\"", timestamp.getAsString()), e);
					}
				}
				final JsonElement input = jsonObject.get("input");
				if (input != null) {
					recording.elements[i].input = input.getAsString();
				}
				final JsonElement output = jsonObject.get("output");
				if (output != null) {
					recording.elements[i].output = output.getAsString();
				}
				final JsonElement multiplicity = jsonObject.get("multiplicity");
				if (multiplicity != null) {
					recording.elements[i].multiplicity = multiplicity.getAsInt();
				}
				final JsonElement tcpserver = jsonObject.get("tcpserver");
				if (tcpserver != null) {
					recording.elements[i].tcpserver = TCPServer.valueOfIgnoreCase(tcpserver.getAsString());
				}
			}
		}
		return recording;
	}

	@Override
	public Recording deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
		Recording recording = new Recording();
		JsonObject jsonObject = json.getAsJsonObject();

		final JsonElement protocol = jsonObject.get("protocol");
		if (protocol != null) {
		    recording.protocol = Protocol.valueOfIgnoreCase(protocol.getAsString());
		}

		final JsonElement port = jsonObject.get("port");
		if (port != null) {
		    recording.port = port.getAsInt();
		}

		final JsonElement recordingURL = jsonObject.get("recordingURL");
		if (recordingURL != null) {
			recording.recordingURL = recordingURL.getAsString();
		}

		final JsonElement captureElements = jsonObject.get("recording");
	    if (captureElements != null) {
		    final JsonArray captureElementsArray = captureElements.getAsJsonArray();
		    recording.elements = new RecordingElement[captureElementsArray.size()];
		    for (int i = 0; i < recording.elements.length; i++) {
		      recording.elements[i] = new RecordingElement();
		      final JsonElement captureElement = captureElementsArray.get(i);
		      jsonObject = captureElement.getAsJsonObject();
	          final JsonElement elementProtocol = jsonObject.get("protocol");
	          recording.elements[i].protocol = Protocol.valueOfIgnoreCase(elementProtocol.getAsString());
			  final JsonElement timestamp = jsonObject.get("timestamp");
			  if (timestamp != null) {
				  try {
					recording.elements[i].timestampStr = timestamp.getAsString();
					recording.elements[i].timestamp = dateFormatter.parse(timestamp.getAsString());
				} catch (ParseException e) {
					throw new IllegalArgumentException(String.format("Error parsing timestamp \"%s\"", timestamp.getAsString()), e);
				}
			  }
			  final JsonElement input = jsonObject.get("input");
			  if (input != null) {
				  recording.elements[i].input = input.getAsString();
			  }
			  final JsonElement output = jsonObject.get("output");
			  if (output != null) {
				  recording.elements[i].output = output.getAsString();
			  }
			  final JsonElement multiplicity = jsonObject.get("multiplicity");
			  if (multiplicity != null) {
				  recording.elements[i].multiplicity = multiplicity.getAsInt();
			  }
			  final JsonElement tcpserver = jsonObject.get("tcpserver");
			  if (tcpserver != null) {
				  recording.elements[i].tcpserver = TCPServer.valueOfIgnoreCase(tcpserver.getAsString());
			  }
		   }
	    }
	    return recording;
	}
}
