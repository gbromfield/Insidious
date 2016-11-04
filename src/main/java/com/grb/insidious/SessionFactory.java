package com.grb.insidious;

import com.grb.insidious.ssh.SSHServer;
import com.grb.insidious.ssh.SSHServerClient;
import com.grb.insidious.tl1.TL1Session;

/**
 * Created by gbromfie on 11/3/16.
 */
public class SessionFactory {
    static public Session createSession(Protocol protocol, String id, SSHServer sshServer, SSHServerClient client) {
        if (protocol == null) {
            return new TL1Session(id, sshServer, client);
        } else if (protocol == Protocol.TL1) {
            return new TL1Session(id, sshServer, client);
        }
        return null;
    }
}
