/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/

package com.azoft.nusuth.distributor.connectionfactory;

import java.io.Serializable;

public class ContainerAddress
        implements Serializable {
    public String host;
    public int port;
    public int adminPort;

    public ContainerAddress(String host, int port, int adminPort) {
        this.host = host;
        this.port = port;
        this.adminPort = adminPort;
    }

    public String toString() {
        return '(' + host + ':' + port + ':' + adminPort + ')';
    }
}
