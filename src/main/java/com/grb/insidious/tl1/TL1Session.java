package com.grb.insidious.tl1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TL1Session implements Session, TL1RecordingListener, Runnable {
	final Logger logger = LoggerFactory.getLogger(TL1Session.class);
    private enum ReaderThreadOperation {
        START,
        STOP,
        CLOSE
    };

    private ArrayBlockingQueue<ReaderThreadOperation> _clientReadQ = new ArrayBlockingQueue<ReaderThreadOperation>(100);

	private String _id;
	private SSHServer _sshServer;
	private SSHServerClient _client;
	private TL1RecordingManager _recordingMgr;

	public TL1Session(String id, SSHServer sshServer, SSHServerClient client) {
		_id = id;
		_sshServer = sshServer;
		_client = client;
		_client.setRunnable(this, String.format("%s_%d_%s", TL1Session.class.getSimpleName(),
				sshServer.getPort(), _id));
		_recordingMgr = new TL1RecordingManager(String.format("%d_%s", sshServer.getPort(), _id), this);
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

	@Override
	public void close() {
		if (logger.isInfoEnabled()) {
			logger.info(String.format("closing client on port %d", _sshServer.getPort()));
		}
		_sshServer.removeSession(this);
		_client.close();
		_recordingMgr.close();

		if (_clientReadQ != null) {
			try {
				_clientReadQ.put(ReaderThreadOperation.CLOSE);
			} catch (InterruptedException e) {}
		}
	}

	@Override
    public void setRecording(Recording recording) throws Exception {
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
					_recordingMgr.setRecording(recordingFromURL);
				}
			} else 	if (recording.elements != null) {
				_recordingMgr.setRecording(recording);
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
	public void run() {
		char[] data = new char[65535];
		ByteBuffer buffer = ByteBuffer.allocate(data.length);
		BufferedReader r = new BufferedReader(new InputStreamReader(_client.getIn()));
		boolean enabled = true;
		while (true) {
			try {
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
				int numRead;
				try {
					numRead = r.read(data);
					if ((numRead == -1) || (data[0] == 4)) {
						close();
						return;
					}
				} catch(IOException e) {
					close();
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
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
			}
		}
	}
}
