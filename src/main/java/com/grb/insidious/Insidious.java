package com.grb.insidious;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import com.ciena.logx.LogX;
import com.ciena.logx.logfile.ra.insidious.InsidiousOutputContext;
import com.grb.insidious.recording.Recording;
import com.grb.insidious.tl1.TL1Session;

import com.grb.tl1.TL1AgentDecoder;
import com.grb.tl1.TL1Message;
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

	static public void startInteractive() {
		final int BufferSize = 100000;
		byte[] indata = new byte[BufferSize];
		int ch;
		int index = 0;
		boolean found = false;
		boolean backslash = false;
		Boolean serialize = true;
		System.out.println("Entering interctive mode\n");
		TL1AgentDecoder decoder = new TL1AgentDecoder();
		try {
			while(true) {
				ch = System.in.read();
				if (index == 0) {
					serialize = (ch != 92); // \
				}
				if (ch == -1) {
					System.exit(1);
				} else if (ch == 10) {	// LF
					backslash = false;
					if ((index == 0) || ((index > 0) && (indata[index - 1] != 13) && (indata[index - 1] != 10))) {
						indata[index++] = 13;
					}
					indata[index++] = 10;
				} else if (ch == 59) {	// ;
					backslash = false;
					if ((index > 1) && (indata[index - 2] == 13) && (indata[index - 1] == 10)) {
						found = true;
					}
					indata[index++] = (byte)ch;
				} else if (ch == 60) {	// <
					backslash = false;
					if ((index > 1) && (indata[index - 2] == 13) && (indata[index - 1] == 10)) {
						found = true;
					}
					indata[index++] = (byte)ch;
				} else if (ch == 62) {	// >
					backslash = false;
					if ((index > 1) && (indata[index - 2] == 13) && (indata[index - 1] == 10)) {
						found = true;
					}
					indata[index++] = (byte)ch;
				} else if ((ch == 114) && (!serialize)) {	// r
					backslash = false;
					if ((index > 0) && (indata[index - 1] == 92)) {
						indata[index - 1] = 13;
					} else {
						indata[index++] = (byte)ch;
					}
				} else if ((ch == 110) && (!serialize)) {	// n
					backslash = false;
					if ((index > 0) && (indata[index - 1] == 92)) {
						indata[index - 1] = 10;
					} else {
						indata[index++] = (byte)ch;
					}
				} else if ((ch == 34) && (!serialize)) {	// "
					backslash = false;
					if ((index > 0) && (indata[index - 1] == 92)) {
						indata[index - 1] = 34;
					} else {
						indata[index++] = (byte)ch;
					}
				} else if ((ch == 92) && (!serialize)) {	// \
					if ((index > 0) && (indata[index - 1] == 92)) {
						if (backslash) {
							indata[index++] = (byte)ch;
							backslash = false;
						} else {
							backslash = true;
						}
					} else {
						indata[index++] = (byte)ch;
						backslash = false;
					}
				} else {
					backslash = false;
					indata[index++] = (byte)ch;
				}
				if (found) {
					if (serialize) {
						ByteBuffer buffer = ByteBuffer.wrap(indata, 0, index);
						buffer.flip();
						TL1Message tl1Msg = decoder.decodeTL1Message(buffer);
						if (tl1Msg != null) {
							System.out.println("NOT FOUND");
						} else {
							System.out.println(transliterateCRLF(new String(indata, 0, index)));
						}
					} else {
						System.out.println(new String(indata, 0, index));
					}
					found = false;
					index = 0;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
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
			e.printStackTrace();
			return null;
		}
	}
	
	private static String encodeSessionJSON(Session session) {
		return String.format("{\"protocol\": \"%s\", \"id\": \"%s\", \"port\": \"%d\", \"client\": \"%s\", \"source\": \"%s\"}",
				session.getProtocol().toString().toLowerCase(), session.getId(), session.getPort(), session.getClient(), session.getSource());
	}

	public static String transliterateCRLF(String input) {
		char[] inputChars = input.toCharArray();
		StringBuilder bldr = new StringBuilder();
		for(int i = 0; i < inputChars.length; i++) {
			if (inputChars[i] == '\r') {
				bldr.append("\\r");
			} else if (inputChars[i] == '\n') {
				if (i > 0) {
					bldr.append("\\n");
				}
			} else if (inputChars[i] == '"') {
				bldr.append("\\\"");
			} else if (inputChars[i] == '\\') {
				bldr.append("\\\\");
			} else {
				bldr.append(inputChars[i]);
			}
		}
		return bldr.toString();
	}

	public static void main(String[] args) {
		InsidiousCommandLineProcessor clp = new InsidiousCommandLineProcessor();
		InsidiousProperties props = (InsidiousProperties)clp.parse(args);

		if (props.getUnknownArg() != null) {
			System.out.println(String.format("Unknown argument: \"%s\"", props.getUnknownArg()));
			System.out.println("Syntax:");
			System.out.println(clp.getSyntax());
			System.exit(0);
		}

		if (props.printHelp()) {
			System.out.println("Syntax:");
			System.out.println(clp.getSyntax());
			System.exit(0);
		}

		if (props.getInteractive()) {
			startInteractive();
			System.exit(0);
		}

		if ((props.getInputFilenames() == null) || (props.getInputFilenames().size() == 0)) {
			Insidious ins = new Insidious();
			ins.startREST();
			try {
				ExitLatch.await();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
		}

		InsidiousOutputContext ctx = null;
		try {
			ctx = new InsidiousOutputContext(props);
			LogX logx = new LogX(props);
			logx.run();
			System.out.println(ctx.logItemsToString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
