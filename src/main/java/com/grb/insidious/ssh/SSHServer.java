package com.grb.insidious.ssh;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gbromfie on 12/1/15.
 */
public class SSHServer {
	final Logger _logger = LoggerFactory.getLogger(SSHServer.class);
	   
	private String _name;
    private int _port;
    private String _hostKeyFilename;
    private SshServer _server;
    private SSHServerClientListener _listener;

    public SSHServer(String name, int listenPort, String hostKeyFilename, SSHServerClientListener listener) {
    	_name = name;
    	_port = listenPort;
        if ((hostKeyFilename == null) || (hostKeyFilename.trim().length() == 0)) {
        	_hostKeyFilename = null;
        } else {
        	_hostKeyFilename = hostKeyFilename.trim();
        	File hostKeyFile = new File(_hostKeyFilename);
            if (!hostKeyFile.exists()) {
                throw new IllegalArgumentException(String.format("Host key file %s does not exist", hostKeyFilename));
            }
        }
    	_server = null;
    	_listener = listener;
    }
    
    public int start() throws IOException {
    	if (_server == null) {
    		_server = SshServer.setUpDefaultServer();
    		_server.setPort(_port);
 
//    		_server.getProperties().put(SshServer.IDLE_TIMEOUT, "0");
    		_server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(_hostKeyFilename));
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
            userAuthFactories.add(new UserAuthNone.Factory());
            _server.setUserAuthFactories(userAuthFactories);
    	}
    	_server.start();
    	_port = _server.getPort();
    	if (_logger.isInfoEnabled()) {
    		_logger.info(String.format("%s - started listening on port %d", _name, _server.getPort()));
    	}
    	return _server.getPort();
    }

    public void stop() throws InterruptedException {
    	if (_server != null) {
        	_server.stop();
        	if (_logger.isInfoEnabled()) {
        		_logger.info(String.format("%s - stopped listening on port %d", _name, _port));
        	}
    	}
    }

    public int getPort() {
    	return _port;
    }
}
