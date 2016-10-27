package com.grb.insidious.tl1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import com.grb.insidious.Protocol;
import com.grb.insidious.Session;
import com.grb.insidious.recording.Recording;
import com.grb.insidious.ssh.SSHServer;
import com.grb.insidious.ssh.SSHServerClient;
import com.grb.insidious.ssh.SSHServerClientListener;
import com.grb.tl1.TL1MessageMaxSizeExceededException;
import com.grb.tl1.TL1OutputMessage;

public class TL1Session implements Session, TL1RecordingListener, SSHServerClientListener {
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
	private TL1RecordingManager _recordingMgr;
	private String _source;
	private SSHServer _sshServer;
	private SSHServerClient _client;
	
	public TL1Session(String id) {
		_id = id;
		_port = 0;
		_clientSource = "";
		_recordingMgr = new TL1RecordingManager(_id, this);
		_source = "";
		_sshServer = null;
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

	@Override
	public void start() throws IOException {
		_sshServer = new SSHServer(_id, _port, "keys/host.ser", this);
		_sshServer.start();
	}

	@Override
	public void close() {
		if (_client != null) {
			_client.close();
		}
		if (_sshServer != null) {
			_sshServer.close();
		}
		if (_clientReadQ != null) {
			try {
				_clientReadQ.put(ReaderThreadOperation.CLOSE);
			} catch (InterruptedException e) {}
		}
		if (_recordingMgr != null) {
			_recordingMgr.close();
		}
	}

    public void setRecording(Recording recording) throws TL1MessageMaxSizeExceededException, ParseException, MalformedURLException, IOException {
		BufferedReader in = null;
		try {
			if (recording.recordingURL != null) {
				URL recordingURL = new URL(recording.recordingURL);
				if (recordingURL != null) {
					in = new BufferedReader(
							new InputStreamReader(recordingURL.openStream()));

					String inputLine;
					StringBuilder bldr = new StringBuilder();
					while ((inputLine = in.readLine()) != null) {
						bldr.append(inputLine);
					}
					Recording recordingFromURL = Recording.parseString(bldr.toString());
					recordingFromURL.protocol = recording.protocol;
					if (recording.port != null) {
						recordingFromURL.port = recording.port;
					}
					if (recordingFromURL.port == null) {
						_port = 0;
					} else {
						_port = recordingFromURL.port;
					}
					_recordingMgr.setRecording(recordingFromURL);
					_source = recording.recordingURL;
				}
			} else 	if (recording.elements != null) {
				if (recording.port == null) {
					_port = 0;
				} else {
					_port = recording.port;
				}
				_recordingMgr.setRecording(recording);
				_source = "inline recording";
			} else {
				// error nothing specified
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
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
                            _recordingMgr.processInput(buffer);
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
