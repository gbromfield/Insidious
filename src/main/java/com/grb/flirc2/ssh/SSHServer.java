package com.grb.flirc2.ssh;

import com.grb.flirc2.Protocol;
import com.grb.flirc2.Session;
import com.grb.flirc2.SessionFactory;
import com.grb.flirc2.recording.Recording;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gbromfie on 12/1/15.
 */
public class SSHServer implements SSHServerClientListener {
	final Logger _logger = LoggerFactory.getLogger(SSHServer.class);

    final static private String KEYFILE = "keys/host.ser";
    final static private String KEYFILEPATH = "src/main/resources/" + KEYFILE;

    static public HashMap<String, Session> sessionMap = new HashMap<String, Session>();

    private String _restClient;
    private int _port;
    private Recording _recording;
    private SshServer _server;
    private long nextSessionsId = 1;

    public SSHServer(String restClient, int listenPort) {
        _restClient = restClient;
    	_port = listenPort;
        _recording = null;
    	_server = null;
    }

    public int start() throws IOException, URISyntaxException {
    	if (_server == null) {
    		_server = SshServer.setUpDefaultServer();
    		_server.setPort(_port);
//    		_server.getProperties().put(SshServer.IDLE_TIMEOUT, "0");
            File keyfile = new File(KEYFILEPATH);
            if (keyfile.exists()) {
                _server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(keyfile));
            } else {
                URL url = ClassLoader.getSystemResource(KEYFILE);
                if (url == null) {
                    throw new IOException(String.format("Unable to load key file at \"%s\"",KEYFILE));
                } else {
                    final Map<String, String> env = new HashMap<>();
                    final String[] array = url.toURI().toString().split("!");
                    final FileSystem fs = FileSystems.newFileSystem(URI.create(array[0]), env);
                    final Path path = fs.getPath(array[1]);
                    _server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(path));
                }
            }
    		_server.setShellFactory(new Factory<Command>() {
                @Override
                public Command create() {
                    SSHServerClient c = new SSHServerClient();
                    newSSHServerClient(c);
                    return c;
                }
            });
            List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>(1);
            userAuthFactories.add(new UserAuthNoneFactory());
            _server.setUserAuthFactories(userAuthFactories);
    	}
    	_server.start();
    	_port = _server.getPort();
    	if (_logger.isInfoEnabled()) {
    		_logger.info(String.format("Started listening on port %d", _port));
    	}
    	return _server.getPort();
    }

    public void stop() throws InterruptedException, IOException {
    	if (_server != null) {
            if (_logger.isInfoEnabled()) {
                _logger.info(String.format("Stopped listening on port %d", _port));
            }
        	_server.stop();
    	}
    }

    public void close() {
        try {
            stop();
        } catch (Exception e) {
        }
        if (_server != null) {
            _server.close(true);
        }
    }

    public int getPort() {
    	return _port;
    }

    public Protocol getProtocol() {
        return _recording.protocol;
    }

    public Recording getRecording() {
        return _recording;
    }

    public void setRecording(Recording recording) {
        _recording = recording;
    }

    public void removeSession(Session session) {
        sessionMap.remove(session.getId());
    }

    public String toJSON() {
        StringBuilder bldr = new StringBuilder("[");
        for(String id : sessionMap.keySet()) {
            if (bldr.length() > 1) {
                bldr.append(", ");
            }
            bldr.append("\"");
            bldr.append(id);
            bldr.append("\"");
        }
        bldr.append("]");
        return String.format("{\"protocol\": \"%s\", \"port\": \"%d\", \"sessions\": %s}",
                getProtocol().toString().toLowerCase(), getPort(), bldr.toString());
    }

    @Override
    public void newSSHServerClient(SSHServerClient client) {
        try {
            Session session = SessionFactory.createSession(_recording.protocol, getNextSessionId(), this, client);
            session.setRecording(_recording);
            sessionMap.put(session.getId(), session);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized private String getNextSessionId() {
        return String.valueOf(nextSessionsId++);
    }
}
