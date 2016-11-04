package com.grb.flirc2;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import com.ciena.logx.LogX;
import com.ciena.logx.logfile.ra.flirc2.Flirc2OutputContext;
import com.grb.flirc2.recording.Recording;
import com.grb.flirc2.ssh.SSHServer;

import com.grb.tl1.TL1AgentDecoder;
import com.grb.tl1.TL1Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import static spark.Spark.*;

public class Flirc2 {
	final Logger logger = LoggerFactory.getLogger(Flirc2.class);

    final static public CountDownLatch ExitLatch = new CountDownLatch(1);

	static private HashMap<Integer, SSHServer> serverMap = new HashMap<Integer, SSHServer>();

	static private long nextSessionsId = 1;	
	synchronized static private long getNextSessionId() {
		return nextSessionsId++;
	}
	
	public Flirc2() {
	}

	public void startREST() {
		post("/servers", (request, response) -> {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("%s %s", request.requestMethod(), request.url()));
			}
			response.type("application/json");
			String body = request.body();
			Recording recording = Recording.parseString(body);
			SSHServer server = createServerSession(request.ip(), recording);
			return server.toJSON();
		});
		get("/servers", (request, response) -> {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("%s %s", request.requestMethod(), request.url()));
			}
			response.type("application/json");
			return serversToJson();
		});
		delete("/servers", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("%s %s", request.requestMethod(), request.url()));
				}
				response.type("application/json");
				String retStr = serversToJson();
				for(SSHServer server : serverMap.values()) {
					server.close();
				}
				serverMap.clear();
				return retStr;
			}
		});
		get("/server/:name", (request, response) -> {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("%s %s", request.requestMethod(), request.url()));
			}
			response.type("application/json");
			String returnStr;
			try {
				response.type("application/json");
				Integer port = Integer.valueOf(request.params(":name"));
				SSHServer server = serverMap.get(port);
				if (server != null) {
					return server.toJSON();
				}
				returnStr = String.format("{\"error\": \"Server %d not found\"}", port);
				if (logger.isErrorEnabled()) {
					logger.error(String.format("Server %d not found", port));
				}
			} catch(NumberFormatException e) {
				returnStr = String.format("{\"error\": \"Unable to convert %s to a port number\"}", request.params(":name"));
				if (logger.isErrorEnabled()) {
					logger.error(String.format("Unable to convert %s to a port number", request.params(":name")));
				}
			}
			response.status(404);
			return returnStr;
		});
		delete("/server/:name", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("%s %s", request.requestMethod(), request.url()));
				}
				String returnStr;
				try {
					response.type("application/json");
					Integer port = Integer.valueOf(request.params(":name"));
					SSHServer server = serverMap.get(port);
					if (server != null) {
						serverMap.remove(port);
						server.close();
						return server.toJSON();
					}
					returnStr = String.format("{\"error\": \"Server %d not found\"}", port);
					if (logger.isErrorEnabled()) {
						logger.error(String.format("Server %d not found", port));
					}
				} catch(NumberFormatException e) {
					returnStr = String.format("{\"error\": \"Unable to convert %s to a port number\"}", request.params(":name"));
					if (logger.isErrorEnabled()) {
						logger.error(String.format("Unable to convert %s to a port number", request.params(":name")));
					}
				}
				response.status(404);
				return returnStr;
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
		Flirc2OutputContext ctx = new Flirc2OutputContext();
		TL1LogRecordParser parser = new TL1LogRecordParser(ctx);
		ctx.addParser(parser);
		LogX logx = new LogX(inputFileList, ctx);
		logx.run();
		System.out.println(ctx.toString());
		Recording recording = Recording.parseString(ctx.toString());
		*/
		return null;
	}

	private static SSHServer createServerSession(String restClient, Recording recording) throws Exception {
		SSHServer sshServer = null;
		int portToUse = 0;
		if ((recording.port != null) && (recording.port > 0)) {
			portToUse = recording.port;
			sshServer = serverMap.get(portToUse);
			// make sure no current client sessions on this port ...
		}
		if (sshServer == null) {
			try {
				sshServer = new SSHServer(restClient, portToUse);
				sshServer.setRecording(recording);
				int port = sshServer.start();
				serverMap.put(port, sshServer);
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		} else {
			if (sshServer.getProtocol().equals(recording.protocol)) {
				sshServer.setRecording(recording);
			} else {
				// error
			}
		}
		return sshServer;
	}

	private static String serversToJson() {
		StringBuilder bldr = new StringBuilder();
		bldr.append("{\"servers\": [");
		boolean firstTime = true;
		for(SSHServer server : serverMap.values()) {
			if (!firstTime) {
				bldr.append(", ");
			}
			bldr.append(server.toJSON());
			firstTime = false;
		}
		bldr.append("]}");
		return bldr.toString();
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
		Flirc2CommandLineProcessor clp = new Flirc2CommandLineProcessor();
		Flirc2Properties props = (Flirc2Properties)clp.parse(args);

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
			Flirc2 ins = new Flirc2();
			ins.startREST();
			try {
				ExitLatch.await();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
		}

		Flirc2OutputContext ctx = null;
		try {
			ctx = new Flirc2OutputContext(props);
			LogX logx = new LogX(props);
			logx.run();
			System.out.println(ctx.logItemsToString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
