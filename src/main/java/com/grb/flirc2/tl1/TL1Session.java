package com.grb.flirc2.tl1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import com.grb.flirc2.Protocol;
import com.grb.flirc2.Session;
import com.grb.flirc2.recording.Recording;
import com.grb.flirc2.ssh.SSHServer;
import com.grb.flirc2.ssh.SSHServerClient;
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
			logger.info(String.format("closing session %s on port %d", _id, _sshServer.getPort()));
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
		_recordingMgr.setRecording(recording);
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

	@Override
	public String toJsonString() {
		return _recordingMgr.toJsonString();
	}
}
