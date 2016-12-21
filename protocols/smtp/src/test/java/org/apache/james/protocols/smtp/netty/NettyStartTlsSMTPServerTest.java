/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.protocols.smtp.netty;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.smtp.SMTPSClient;
import org.apache.james.protocols.api.Encryption;
import org.apache.james.protocols.api.Protocol;
import org.apache.james.protocols.api.ProtocolServer;
import org.apache.james.protocols.api.handler.ProtocolHandler;
import org.apache.james.protocols.api.handler.WiringException;
import org.apache.james.protocols.api.utils.BogusSSLSocketFactory;
import org.apache.james.protocols.api.utils.BogusSslContextFactory;
import org.apache.james.protocols.api.utils.BogusTrustManagerFactory;
import org.apache.james.protocols.api.utils.MockLogger;
import org.apache.james.protocols.api.utils.ProtocolServerUtils;
import org.apache.james.protocols.netty.AbstractChannelPipelineFactory;
import org.apache.james.protocols.netty.NettyServer;
import org.apache.james.protocols.smtp.AllButStartTlsDelimiterChannelHandler;
import org.apache.james.protocols.smtp.SMTPConfigurationImpl;
import org.apache.james.protocols.smtp.SMTPProtocol;
import org.apache.james.protocols.smtp.SMTPProtocolHandlerChain;
import org.apache.james.protocols.smtp.utils.TestMessageHook;
import org.assertj.core.api.AssertDelegateTarget;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.junit.After;
import org.junit.Test;

import com.google.common.base.Optional;
import com.sun.mail.smtp.SMTPTransport;

public class NettyStartTlsSMTPServerTest {

    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final int RANDOM_PORT = 0;

    private ProtocolServer server;

    @After
    public void tearDown() {
        if (server != null) {
            server.unbind();
        }
    }

    private ProtocolServer createServer(Protocol protocol, Encryption enc) {
        NettyServer server = NettyServer.builder()
                .protocol(protocol)
                .secure(enc)
                .frameHandler(new AllButStartTlsDelimiterChannelHandler(AbstractChannelPipelineFactory.MAX_LINE_LENGTH, false, Delimiters.lineDelimiter()))
                .build();
        server.setListenAddresses(new InetSocketAddress(LOCALHOST_IP, RANDOM_PORT));
        return server;
    }

    private SMTPSClient createClient() {
        SMTPSClient client = new SMTPSClient(false, BogusSslContextFactory.getClientContext());
        client.setTrustManager(BogusTrustManagerFactory.getTrustManagers()[0]);
        return client;
    }

    private Protocol createProtocol(Optional<ProtocolHandler> handler) throws WiringException {
        SMTPProtocolHandlerChain chain = new SMTPProtocolHandlerChain();
        if (handler.isPresent()) {
            chain.add(handler.get());
        }
        chain.wireExtensibleHandlers();
        return new SMTPProtocol(chain, new SMTPConfigurationImpl(), new MockLogger());
    }

    @Test
    public void connectShouldReturnTrueWhenConnecting() throws Exception {
        ProtocolServer server = createServer(createProtocol(Optional.<ProtocolHandler> absent()), Encryption.createStartTls(BogusSslContextFactory.getServerContext()));  
        server.bind();

        SMTPSClient client = createClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(server).retrieveBindedAddress();
        client.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        assertThat(SMTPReply.isPositiveCompletion(client.getReplyCode())).isTrue();

        client.quit();
        client.disconnect();
    }

    @Test
    public void ehloShouldReturnTrueWhenSendingTheCommand() throws Exception {
        ProtocolServer server = createServer(createProtocol(Optional.<ProtocolHandler> absent()), Encryption.createStartTls(BogusSslContextFactory.getServerContext()));  
        server.bind();

        SMTPSClient client = createClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(server).retrieveBindedAddress();
        client.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());

        client.sendCommand("EHLO localhost");
        assertThat(SMTPReply.isPositiveCompletion(client.getReplyCode())).isTrue();
        
        client.quit();
        client.disconnect();
    }

    @Test
    public void startTlsShouldBeAnnouncedWhenServerSupportsIt() throws Exception {
        ProtocolServer server = createServer(createProtocol(Optional.<ProtocolHandler> absent()), Encryption.createStartTls(BogusSslContextFactory.getServerContext()));  
        server.bind();

        SMTPSClient client = createClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(server).retrieveBindedAddress();
        client.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        client.sendCommand("EHLO localhost");

        assertThat(new StartTLSAssert(client)).isStartTLSAnnounced();

        client.quit();
        client.disconnect();
    }

    private static class StartTLSAssert implements AssertDelegateTarget {

        private final SMTPSClient client;

        public StartTLSAssert(SMTPSClient client) {
            this.client = client;
            
        }

        public boolean isStartTLSAnnounced() {
            for (String reply: client.getReplyStrings()) {
                if (reply.toUpperCase(Locale.UK).endsWith("STARTTLS")) {
                    return true;
                }
            }
            return false;
        }
    }

    @Test
    public void startTlsShouldReturnTrueWhenServerSupportsIt() throws Exception {
        ProtocolServer server = createServer(createProtocol(Optional.<ProtocolHandler> absent()), Encryption.createStartTls(BogusSslContextFactory.getServerContext()));  
        server.bind();

        SMTPSClient client = createClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(server).retrieveBindedAddress();
        client.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        client.sendCommand("EHLO localhost");

        boolean execTLS = client.execTLS();
        assertThat(execTLS).isTrue();

        client.quit();
        client.disconnect();
    }

    @Test
    public void startTlsShouldFailWhenFollowedByInjectedCommand() throws Exception {
        ProtocolServer server = createServer(createProtocol(Optional.<ProtocolHandler> absent()), Encryption.createStartTls(BogusSslContextFactory.getServerContext()));  
        server.bind();

        SMTPSClient client = createClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(server).retrieveBindedAddress();
        client.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        client.sendCommand("EHLO localhost");

        client.sendCommand("STARTTLS\r\nRSET\r\n");
        assertThat(SMTPReply.isPositiveCompletion(client.getReplyCode())).isFalse();
    }

    @Test
    public void startTlsShouldWorkWhenUsingJavamail() throws Exception {
        TestMessageHook hook = new TestMessageHook();
        ProtocolServer server = createServer(createProtocol(Optional.<ProtocolHandler> of(hook)) , Encryption.createStartTls(BogusSslContextFactory.getServerContext()));  
        server.bind();

        InetSocketAddress bindedAddress = new ProtocolServerUtils(server).retrieveBindedAddress();

        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.from", "test@localhost");
        mailProps.put("mail.smtp.host", bindedAddress.getHostName());
        mailProps.put("mail.smtp.port", bindedAddress.getPort());
        mailProps.put("mail.smtp.socketFactory.class", BogusSSLSocketFactory.class.getName());
        mailProps.put("mail.smtp.socketFactory.fallback", "false");
        mailProps.put("mail.smtp.starttls.enable", "true");

        Session mailSession = Session.getDefaultInstance(mailProps);

        InternetAddress[] rcpts = new InternetAddress[] { new InternetAddress("valid@localhost") };
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress("test@localhost"));
        message.setRecipients(Message.RecipientType.TO, rcpts);
        message.setSubject("Testmail", "UTF-8");
        message.setText("Test.....");

        SMTPTransport transport = (SMTPTransport) mailSession.getTransport("smtps");

        transport.connect(new Socket(bindedAddress.getHostName(), bindedAddress.getPort()));
        transport.sendMessage(message, rcpts);

        assertThat(hook.getQueued()).hasSize(1);
    }
}
