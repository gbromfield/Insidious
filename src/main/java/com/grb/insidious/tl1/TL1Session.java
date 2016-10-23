package com.grb.insidious.tl1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import com.grb.insidious.Protocol;
import com.grb.insidious.Session;
import com.grb.insidious.capture.Capture;
import com.grb.insidious.ssh.SSHServer;
import com.grb.insidious.ssh.SSHServerClient;
import com.grb.insidious.ssh.SSHServerClientListener;
import com.grb.tl1.TL1MessageMaxSizeExceededException;
import com.grb.tl1.TL1OutputMessage;

public class TL1Session implements Session, TL1CaptureListener, SSHServerClientListener {
    private enum ReaderThreadOperation {
        START,
        STOP,
        CLOSE
    };

    private ArrayBlockingQueue<ReaderThreadOperation> _clientReadQ = new ArrayBlockingQueue<ReaderThreadOperation>(100);
	static private HashMap<String, TL1Session> _sessions = new HashMap<String, TL1Session>();
	
	static public TL1Session getSession(String name) {
		return _sessions.get(name);
	}
	
	private String _id;
	private int _port;
	private String _clientSource;
	private TL1CaptureManager _captureMgr;
	private String _source;
	private SSHServer _sshServer;
	private SSHServerClient _client;
	
	public TL1Session(String id, int port) throws IOException {
		_id = id;
		_port = port;
		_clientSource = "";
		_captureMgr = new TL1CaptureManager(_id, this);
		_source = "";
		_sshServer = new SSHServer(_id, _port, "/Users/Graham/Documents/workspace/insidious/keys/host.ser", this);
		_sshServer.start();
		_client = null;
	}

	@Override
	public String getId() {
		return _id;
	}

	@Override
	public int getPort() {
		return _sshServer.getPort();
	}	

	@Override
	public Protocol getProtocol() {
		return Protocol.TL1;
	}

	public void setClient(String client) {
		_clientSource = client;
	}
	
	@Override
	public String getClient() {
		return _clientSource;
	}

	@Override
	public String getSource() {
		return _source;
	}

    public void setCapture(String source, Capture capture) throws TL1MessageMaxSizeExceededException, ParseException {
    	_source = source;
    	_captureMgr.setCapture(capture);
    }

	@Override
	public void onTL1Output(TL1OutputMessage outputMsg) {
		try {
			_client.getOut().write(outputMsg.toString().getBytes());
			_client.getOut().flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void newSSHServerClient(SSHServerClient client) {
		_client = client;
		client.setRunnable(new Runnable() {
            @Override
            public void run() {
                char[] data = new char[65535];
                ByteBuffer buffer = ByteBuffer.allocate(data.length);
                BufferedReader r = new BufferedReader(new InputStreamReader(client.getIn()));
                boolean enabled = true;
                try {
                    while (true) {
                        while ((!enabled) || (_clientReadQ.size() > 0)) {
                            ReaderThreadOperation oper = _clientReadQ.take();
                            if (oper.equals(ReaderThreadOperation.START)) {
                                enabled = true;
                            } else if (oper.equals(ReaderThreadOperation.STOP)) {
                                enabled = false;
                            } else {
                                return;
                            }
                        }
                        int numRead = r.read(data);
                        if (numRead == -1) {
//                            close();
                            return;
                        }
                        while (_clientReadQ.size() > 0) {
                            ReaderThreadOperation oper = _clientReadQ.take();
                            if (oper.equals(ReaderThreadOperation.START)) {
                                enabled = true;
                            } else if (oper.equals(ReaderThreadOperation.STOP)) {
                                enabled = false;
                            } else {
                                return;
                            }
                        }
                        for(int i = 0; i < numRead; i++) {
                            buffer.put((byte) data[i]);
                        }
                        if (enabled) {
                            buffer.flip();
                            _captureMgr.processInput(buffer);
                            buffer.clear();
                        }
                    }
                } catch(Exception e) {
                } finally {
                }
            }
        }, "SSHServerClient_" + _id); // TODO:
 	}
}
