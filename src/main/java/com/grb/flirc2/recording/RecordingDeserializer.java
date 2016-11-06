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

public class RecordingDeserializer implements JsonDeserializer<RecordingJson> {
	final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

	static public RecordingJson deserializeRecording(JsonObject parentJsonObject) {
		RecordingJson recordingJson = new RecordingJson();
		recordingJson.elements = null;
		JsonObject jsonObject = null;
		final JsonElement recordingElements = parentJsonObject.get("recording");
		if (recordingElements != null) {
			final JsonArray captureElementsArray = recordingElements.getAsJsonArray();
			recordingJson.elements = new RecordingElement[captureElementsArray.size()];
			for (int i = 0; i < recordingJson.elements.length; i++) {
				recordingJson.elements[i] = new RecordingElement();
				final JsonElement captureElement = captureElementsArray.get(i);
				jsonObject = captureElement.getAsJsonObject();
				final JsonElement protocol = jsonObject.get("protocol");
				if (protocol == null) {
					throw new IllegalArgumentException("Protocol is mandatory in recording element");
				}
				recordingJson.elements[i].protocol = Protocol.valueOfIgnoreCase(protocol.getAsString());
				final JsonElement timestamp = jsonObject.get("timestamp");
				if (timestamp != null) {
					try {
						recordingJson.elements[i].timestampStr = timestamp.getAsString();
						recordingJson.elements[i].timestamp = dateFormatter.parse(timestamp.getAsString());
					} catch (ParseException e) {
						throw new IllegalArgumentException(String.format("Error parsing timestamp \"%s\"", timestamp.getAsString()), e);
					}
				}
				final JsonElement input = jsonObject.get("input");
				if (input != null) {
					recordingJson.elements[i].input = input.getAsString();
				}
				final JsonElement output = jsonObject.get("output");
				if (output != null) {
					recordingJson.elements[i].output = output.getAsString();
				}
				final JsonElement multiplicity = jsonObject.get("multiplicity");
				if (multiplicity != null) {
					recordingJson.elements[i].multiplicity = multiplicity.getAsInt();
				}
				final JsonElement tcpserver = jsonObject.get("tcpserver");
				if (tcpserver != null) {
					recordingJson.elements[i].tcpserver = TCPServer.valueOfIgnoreCase(tcpserver.getAsString());
				}
			}
		}
		return recordingJson;
	}

	@Override
	public RecordingJson deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
		RecordingJson recordingJson = new RecordingJson();
		JsonObject jsonObject = json.getAsJsonObject();

		final JsonElement protocol = jsonObject.get("protocol");
		if (protocol != null) {
			recordingJson.protocol = Protocol.valueOfIgnoreCase(protocol.getAsString());
		}

		final JsonElement port = jsonObject.get("port");
		if (port != null) {
			recordingJson.port = port.getAsInt();
		}

		final JsonElement recordingURL = jsonObject.get("recordingURL");
		if (recordingURL != null) {
			throw new IllegalArgumentException("Using deprecated recordingURL json property");
		}
		final JsonElement recordingURLs = jsonObject.get("recordingURLs");
		if (recordingURLs != null) {
			final JsonArray recordingURLsArray = recordingURLs.getAsJsonArray();
			recordingJson.recordingURLs = new String[recordingURLsArray.size()];
			for(int i = 0; i < recordingJson.recordingURLs.length; i++) {
				recordingJson.recordingURLs[i] = recordingURLsArray.get(i).getAsString();
			}
		}

		final JsonElement captureElements = jsonObject.get("recording");
	    if (captureElements != null) {
		    final JsonArray captureElementsArray = captureElements.getAsJsonArray();
			recordingJson.elements = new RecordingElement[captureElementsArray.size()];
		    for (int i = 0; i < recordingJson.elements.length; i++) {
				recordingJson.elements[i] = new RecordingElement();
		      final JsonElement captureElement = captureElementsArray.get(i);
		      jsonObject = captureElement.getAsJsonObject();
	          final JsonElement elementProtocol = jsonObject.get("protocol");
				recordingJson.elements[i].protocol = Protocol.valueOfIgnoreCase(elementProtocol.getAsString());
			  final JsonElement timestamp = jsonObject.get("timestamp");
			  if (timestamp != null) {
				  try {
					  recordingJson.elements[i].timestampStr = timestamp.getAsString();
					  recordingJson.elements[i].timestamp = dateFormatter.parse(timestamp.getAsString());
				  } catch (ParseException e) {
					  throw new IllegalArgumentException(String.format("Error parsing timestamp \"%s\"", timestamp.getAsString()), e);
				  }
			  }
			  final JsonElement input = jsonObject.get("input");
			  if (input != null) {
				  recordingJson.elements[i].input = input.getAsString();
			  }
			  final JsonElement output = jsonObject.get("output");
			  if (output != null) {
				  recordingJson.elements[i].output = output.getAsString();
			  }
			  final JsonElement multiplicity = jsonObject.get("multiplicity");
			  if (multiplicity != null) {
				  recordingJson.elements[i].multiplicity = multiplicity.getAsInt();
			  }
			  final JsonElement tcpserver = jsonObject.get("tcpserver");
			  if (tcpserver != null) {
				  recordingJson.elements[i].tcpserver = TCPServer.valueOfIgnoreCase(tcpserver.getAsString());
			  }
		   }
	    }
	    return recordingJson;
	}
}
