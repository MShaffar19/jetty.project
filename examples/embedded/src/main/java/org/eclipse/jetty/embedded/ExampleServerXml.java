//
//  ========================================================================
//  Copyright (c) 1995-2021 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.embedded;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;

/**
 * Configures and Starts a Jetty server from an XML declaration.
 */
public class ExampleServerXml
{
    public static Server createServer(int port) throws Exception
    {
        // Find Jetty XML (in classpath) that configures and starts Server.
        // See src/main/resources/exampleserver.xml
        Resource serverXml = Resource.newSystemResource("exampleserver.xml");
        XmlConfiguration xml = new XmlConfiguration(serverXml);
        xml.getProperties().put("http.port", Integer.toString(port));
        Server server = (Server)xml.configure();
        return server;
    }

    public static void main(String[] args) throws Exception
    {
        int port = ExampleUtil.getPort(args, "jetty.http.port", 8080);
        Server server = createServer(port);
        server.start();
        server.join();
    }
}
