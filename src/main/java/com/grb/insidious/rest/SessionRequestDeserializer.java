package com.grb.insidious.rest;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.grb.insidious.Protocol;

public class SessionRequestDeserializer implements JsonDeserializer<SessionRequest> {

	@Override
	public SessionRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		SessionRequest req = new SessionRequest();
		JsonObject jsonObject = json.getAsJsonObject();

	    final JsonElement sessionElements = jsonObject.get("sessions");
	    if (sessionElements != null) {
		    final JsonArray sessionElementsArray = sessionElements.getAsJsonArray();
		    req.elements = new SessionElement[sessionElementsArray.size()];
		    for (int i = 0; i < req.elements.length; i++) {
		    	req.elements[i] = new SessionElement();
			    final JsonElement sessionElement = sessionElementsArray.get(i);
			    jsonObject = sessionElement.getAsJsonObject();
			    final JsonElement protocol = jsonObject.get("protocol");
			    if (protocol != null) {
			    	req.elements[i].protocol = Protocol.valueOfIgnoreCase(protocol.getAsString());
			    }
			    final JsonElement captureURL = jsonObject.get("captureURL");
			    if (captureURL != null) {
					req.elements[i].captureURL = captureURL.getAsString();
				}
				final JsonElement port = jsonObject.get("port");
				if (port != null) {
					req.elements[i].port = port.getAsInt();
				}
		    }
	    }
		return req;
	}
}
