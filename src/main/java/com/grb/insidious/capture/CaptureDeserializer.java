package com.grb.insidious.capture;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.grb.insidious.Protocol;

public class CaptureDeserializer implements JsonDeserializer<Capture> {
	final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

	@Override
	public Capture deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
		Capture capture = new Capture();
		JsonObject jsonObject = json.getAsJsonObject();

		final JsonElement name = jsonObject.get("name");
		if (name != null) {
		    capture.name = name.getAsString();
		}

		final JsonElement tl1port = jsonObject.get("tl1port");
		if (tl1port != null) {
		    capture.tl1Port = tl1port.getAsInt();
		}

	    final JsonElement captureElements = jsonObject.get("capture");
	    if (captureElements != null) {
		    final JsonArray captureElementsArray = captureElements.getAsJsonArray();
		    capture.elements = new CaptureElement[captureElementsArray.size()];
		    for (int i = 0; i < capture.elements.length; i++) {
		      capture.elements[i] = new CaptureElement();
		      final JsonElement captureElement = captureElementsArray.get(i);
		      jsonObject = captureElement.getAsJsonObject();
	          final JsonElement protocol = jsonObject.get("protocol");
	          capture.elements[i].protocol = Protocol.valueOfIgnoreCase(protocol.getAsString());
			  final JsonElement timestamp = jsonObject.get("timestamp");
			  if (timestamp != null) {
				  try {
					capture.elements[i].timestampStr = timestamp.getAsString();
					capture.elements[i].timestamp = dateFormatter.parse(timestamp.getAsString());
				} catch (ParseException e) {
					throw new IllegalArgumentException(String.format("Error parsing timestamp \"%s\"", timestamp.getAsString()), e);
				}
			  }
			  final JsonElement input = jsonObject.get("input");
			  if (input != null) {
				  capture.elements[i].input = input.getAsString();
			  }
			  final JsonElement output = jsonObject.get("output");
			  if (output != null) {
				  capture.elements[i].output = output.getAsString();
			  }
			  final JsonElement multiplicity = jsonObject.get("multiplicity");
			  if (multiplicity != null) {
				  capture.elements[i].multiplicity = multiplicity.getAsInt();
			  }
			  final JsonElement tcpserver = jsonObject.get("tcpserver");
			  if (tcpserver != null) {
				  capture.elements[i].tcpserver = TCPServer.valueOfIgnoreCase(tcpserver.getAsString());
			  }
		   }
	    }
	    return capture;
	}	
}
