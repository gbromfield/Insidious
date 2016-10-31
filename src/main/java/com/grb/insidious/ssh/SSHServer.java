package com.grb.insidious.ssh;

import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gbromfie on 12/1/15.
 */
public class SSHServer {
	final Logger _logger = LoggerFactory.getLogger(SSHServer.class);

    final static private String KEYFILE = "keys/host.ser";
    final static private String KEYFILEPATH = "src/main/resources/" + KEYFILE;

	private String _name;
    private int _port;
    private SshServer _server;
    private SSHServerClientListener _listener;

    public SSHServer(String name, int listenPort, SSHServerClientListener listener) {
    	_name = name;
    	_port = listenPort;
    	_server = null;
    	_listener = listener;
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
                    System.out.println("COULDN'T FIND URL");
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
                    if (_listener != null) {
                        _listener.newSSHServerClient(c);
                    }
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
    		_logger.info(String.format("%s - started listening on port %d", _name, _server.getPort()));
    	}
    	return _server.getPort();
    }

    public void stop() throws InterruptedException, IOException {
    	if (_server != null) {
        	_server.stop();
        	if (_logger.isInfoEnabled()) {
        		_logger.info(String.format("%s - stopped listening on port %d", _name, _port));
        	}
    	}
    }

    public void close() {
        if (_server != null) {
            _server.close(true);
        }
    }

    public int getPort() {
    	return _port;
    }
}
