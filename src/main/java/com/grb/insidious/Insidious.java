package com.grb.insidious;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import com.grb.insidious.recording.Recording;
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
		
	private static Session createSession(String client, Recording recording) {
		Session session = null;
		try {
			long sessionId = getNextSessionId();
			if (recording.protocol.equals(Protocol.TL1)) {
				TL1Session tl1Session = new TL1Session(String.valueOf(sessionId), recording.port);
				session = tl1Session;
				tl1Session.setClient(client);
				sessionMap.put(tl1Session.getId(), tl1Session);
				tl1Session.setRecording(recording);
			} else {
				// error
			}
			return session;
		} catch(Exception e) {
			return null;
		}
	}
	
	private static String encodeSessionJSON(Session session) {
		return String.format("{\"protocol\": \"%s\", \"id\": \"%s\", \"port\": \"%d\", \"client\": \"%s\", \"source\": \"%s\"}",
				session.getProtocol().toString().toLowerCase(), session.getId(), session.getPort(), session.getClient(), session.getSource());
	}
	
	public static void printSyntax() {
		System.out.println(";syntax: Insidious -f recording");
	}
	
	public static void main(String[] args) {
		post("/sessions", (request, response) -> {
			response.type("application/json");
			String body = request.body();
			Recording recording = Recording.parseString(body);
			Session session = createSession(request.ip(), recording);
			return encodeSessionJSON(session);
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
				Session session = sessionMap.get(request.params(":name"));
				if (session != null) {
					session.close();
					sessionMap.remove(request.params(":name"));
					return encodeSessionJSON(session);
				}
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
	    	Recording recording = Recording.parseString(ctx.toString());
	    	tl1Session.setRecording("", recording);
*/
//	    	Recording recording = Recording.parseFile(args[1]);
	        
            ExitLatch.await();
            System.exit(0);
	    } catch(Exception e ) {
	    	e.printStackTrace();
		} finally {
	    }
	}
}
