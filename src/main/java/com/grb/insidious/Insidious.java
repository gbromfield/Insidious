package com.grb.insidious;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import com.ciena.logx.LogX;
import com.ciena.logx.logfile.ra.insidious.InsidiousOutputContext;
import com.ciena.logx.logfile.ra.insidious.logrecord.TL1LogRecordParser;
import com.grb.insidious.capture.Capture;
import com.grb.insidious.rest.SessionRequest;
import com.grb.insidious.tl1.TL1Session;

import spark.Request;
import spark.Response;
import spark.Route;

import static spark.Spark.*;

public class Insidious {

    final static public CountDownLatch ExitLatch = new CountDownLatch(1);
	static private HashMap<String, Session> sessionMap = new HashMap<String, Session>();
	
	static private long nextSessionsId = 1;	
	synchronized static private long getNextSessionId() {
		return nextSessionsId++;
	}
	
	public Insidious() {
	}
		
	private static Session[] createSessions(String client, SessionRequest req) {
		BufferedReader in = null;
		try {
			Session[] sessions = new Session[req.elements.length];
			for(int i = 0; i < req.elements.length; i++) {
				long sessionId = getNextSessionId();
				if (req.elements[i].protocol.equals(Protocol.TL1)) {
			    	TL1Session tl1Session = new TL1Session(String.valueOf(sessionId), req.elements[i].port);
			    	tl1Session.setClient(client);
			    	sessions[i] = tl1Session;
			    	sessionMap.put(tl1Session.getId(), tl1Session);
			    	URL captureURL = new URL(req.elements[i].captureURL);
			        in = new BufferedReader(
			        new InputStreamReader(captureURL.openStream()));

			        String inputLine;
			        StringBuilder bldr = new StringBuilder();
			        while ((inputLine = in.readLine()) != null) {
			        	bldr.append(inputLine);
			        }
			        tl1Session.setCapture(req.elements[i].captureURL, Capture.parseString(bldr.toString()));
				} else {
					// error
				}
			}
			return sessions;
		} catch(Exception e) {
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {}
			}
		}
	}
	
	private static String encodeSessionJSON(Session session) {
		return String.format("{\"protocol\": \"%s\", \"id\": \"%s\", \"port\": \"%d\", \"client\": \"%s\", \"source\": \"%s\"}",
				session.getProtocol().toString().toLowerCase(), session.getId(), session.getPort(), session.getClient(), session.getSource());
	}
	
	public static void printSyntax() {
		System.out.println(";syntax: Insidious -f capture");
	}
	
	public static void main(String[] args) {
		post("/sessions", (request, response) -> {
			response.type("application/json");
			String body = request.body();
			SessionRequest req = SessionRequest.parseString(body);
			Session[] sessions = createSessions(request.ip(), req);
			StringBuilder bldr = new StringBuilder();
			bldr.append("{\"sessions\": [");
			for(int i = 0; i < sessions.length; i++) {
				if (i > 0) {
					bldr.append(",");
				}
				bldr.append(encodeSessionJSON(sessions[i]));
			}
			bldr.append("]}");
		    return bldr.toString();
		});
		get("/sessions", (request, response) -> {
			response.type("application/json");
			StringBuilder bldr = new StringBuilder();
			bldr.append("{\"sessions\": [");
			Iterator<Session> it = sessionMap.values().iterator();
			int i = 0;
			while(it.hasNext()) {
				if (i > 0) {
					bldr.append(",");
				}
				bldr.append(encodeSessionJSON(it.next()));
				i++;
			}
			bldr.append("]}");
		    return bldr.toString();
		});
		delete("/sessions", new Route() {	
			@Override
			public Object handle(Request request, Response response) throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
		});
		get("/session/:name", (request, response) -> {
			response.type("application/json");
		    return encodeSessionJSON(sessionMap.get(request.params(":name")));
		});
		delete("/session/:name", new Route() {	
			@Override
			public Object handle(Request request, Response response) throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
		});
		
	    try {
/*	    	
	    	TL1Session tl1Session = new TL1Session("session1", 0);	    	
	        ArrayList<String> inputFiles = new ArrayList<String>();
	        inputFiles.add("samples/bpprov.2.log");
            ArrayList<File>  inputFileList = LogX.processFilenames(inputFiles, null);
	        InsidiousOutputContext ctx = new InsidiousOutputContext();
            TL1LogRecordParser parser = new TL1LogRecordParser(ctx);
            ctx.addParser(parser);
            LogX logx = new LogX(inputFileList, ctx);
            logx.run();
            System.out.println(ctx.toString());
	    	Capture capture = Capture.parseString(ctx.toString());
	    	tl1Session.setCapture("", capture);
*/
//	    	Capture capture = Capture.parseFile(args[1]);
	        
            ExitLatch.await();
            System.exit(0);
	    } catch(Exception e ) {
	    	e.printStackTrace();
		} finally {
	    }
	}
}
