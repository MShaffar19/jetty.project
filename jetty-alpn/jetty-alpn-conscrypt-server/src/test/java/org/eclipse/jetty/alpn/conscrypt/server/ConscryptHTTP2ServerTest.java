//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.alpn.conscrypt.server;

import org.conscrypt.OpenSSLProvider;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test server that verifies that the Conscrypt ALPN mechanism works for both server and client side
 */
public class ConscryptHTTP2ServerTest
{

    Server server = new Server();

    static
    {
        Security.addProvider(new OpenSSLProvider());
    }

    public static void main(String[] args)
        throws Exception
    {
        new ConscryptHTTP2ServerTest().startServer();
    }

    private SslContextFactory newSslContextFactory()
    {
        Path path = Paths.get("src", "test", "resources");
        File keys = path.resolve("keystore").toFile();

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyManagerPassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
        sslContextFactory.setTrustStorePath(keys.getAbsolutePath());
        sslContextFactory.setKeyStorePath(keys.getAbsolutePath());
        sslContextFactory.setTrustStorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
        sslContextFactory.setProvider("Conscrypt");
        sslContextFactory.setEndpointIdentificationAlgorithm(null);
        return sslContextFactory;
    }

    @BeforeEach
    public void startServer()
        throws Exception
    {

        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme( "https" );

        httpsConfig.setSendXPoweredBy(true);
        httpsConfig.setSendServerVersion(true);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        HttpConnectionFactory http = new HttpConnectionFactory(httpsConfig);
        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpsConfig);
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(http.getProtocol());
        SslConnectionFactory ssl = new SslConnectionFactory(newSslContextFactory(), alpn.getProtocol());

        ServerConnector http2Connector = new ServerConnector(server,ssl,alpn,h2,http);
        http2Connector.setPort(0);
        server.addConnector(http2Connector);

        server.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String target,Request baseRequest,HttpServletRequest request,
                                HttpServletResponse response)
                throws IOException, ServletException
            {
                response.setStatus(200);
                baseRequest.setHandled(true);
            }
        } );

        server.start();

    }

    @AfterEach
    public void stopServer()
        throws Exception
    {
        if (server != null)
        {
            server.stop();
        }
    }


    @Test
    public void test_simple_query()
        throws Exception
    {

        HTTP2Client h2Client = new HTTP2Client();
        HttpClient client = new HttpClient(new HttpClientTransportOverHTTP2(h2Client),newSslContextFactory());
        client.start();
        try
        {
            int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
            ContentResponse contentResponse = client.GET("https://localhost:" + port);
            assertEquals(200, contentResponse.getStatus());
        }
        finally
        {
            client.stop();
        }

    }
}
