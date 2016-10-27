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
import com.ciena.logx.util.ExtensionFilter;
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

	public void startREST() {
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
	}

	public Recording createRecording() {
		/*
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
		*/
		return null;
	}

	private static Session createSession(String client, Recording recording) {
		Session session = null;
		try {
			long sessionId = getNextSessionId();
			if (recording.protocol.equals(Protocol.TL1)) {
				TL1Session tl1Session = new TL1Session(String.valueOf(sessionId));
				session = tl1Session;
				tl1Session.setClient(client);
				sessionMap.put(tl1Session.getId(), tl1Session);
				tl1Session.setRecording(recording);
				tl1Session.start();
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
	    try {
			ArrayList<String> inputFiles = new ArrayList<String>();
			boolean processingInputFiles = false;
			String extension = null;
			ExtensionFilter filter = null;
			String recordingFilename = null;

			int i = 0;
			while(i < args.length) {
				if (args[i].equalsIgnoreCase("-i")) {
					processingInputFiles = true;
				} else if (args[i].equalsIgnoreCase("-e")) {
					processingInputFiles = false;
					i++;
					filter = new ExtensionFilter(args[i]);
				} else if (args[i].equalsIgnoreCase("-c")) {
					i++;
					recordingFilename = args[i];
				} else if (args[i].startsWith("-")) {
					processingInputFiles = false;
				} else if (processingInputFiles) {
					inputFiles.add(args[i]);
				} else {
					// ignore might be a parser argument
				}
				i++;
			}

			if (inputFiles.size() == 0) {
				Insidious ins = new Insidious();
				ins.startREST();
				ExitLatch.await();
				System.exit(1);
			}

//	    	TL1Session tl1Session = new TL1Session("session1", 0);
            ArrayList<File>  inputFileList = LogX.processFilenames(inputFiles, null);
	        InsidiousOutputContext ctx = new InsidiousOutputContext(new String[] {"-tl1", "-cap", recordingFilename});
            LogX logx = new LogX(inputFileList, ctx);
            logx.run();
            System.out.println(ctx.toString());
//	    	Recording recording = Recording.parseString(ctx.toString());
//	    	tl1Session.setRecording("", recording);
//			Recording recording = Recording.parseFile(args[1]);

            System.exit(0);
	    } catch(Exception e ) {
	    	e.printStackTrace();
		} finally {
	    }
	}
}
