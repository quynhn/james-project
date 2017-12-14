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

package org.apache.james.transport.mailets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.mail.MessagingException;

import org.apache.james.protocols.lib.PortUtil;
import org.apache.james.smtpserver.mock.util.MockSpamd;
import org.apache.james.util.scanner.SpamAssassinInvoker;
import org.apache.mailet.Mail;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.apache.mailet.base.test.MimeMessageBuilder;
import org.junit.Test;

public class SpamAssassinTest {
    private SpamAssassin mailet = new SpamAssassin();

    @Test
    public void initShouldNotThrowWhenNoSpamHost() throws Exception {
        assertThatThrownBy(() -> mailet.init()).isInstanceOf(NullPointerException.class) ;
    }

    @Test
    public void initShouldSetDefaultSpamdHostWhenDefault() throws Exception {
        mailet.init(FakeMailetConfig.builder()
            .mailetName("SpamAssassin")
            .build());

        assertThat(mailet.getSpamdHost()).isEqualTo(SpamAssassin.DEFAULT_HOST);
    }

    @Test
    public void initShouldSetSpamdHostWhenPresent() throws Exception {
        String spamdHost = "any.host";
        mailet.init(FakeMailetConfig.builder()
            .mailetName("SpamAssassin")
            .setProperty(SpamAssassin.SPAMD_HOST, spamdHost)
            .build());

        assertThat(mailet.getSpamdHost()).isEqualTo(spamdHost);
    }

    @Test
    public void initShouldSetDefaultSpamdPortWhenDefault() throws Exception {
        mailet.init(FakeMailetConfig.builder()
            .mailetName("SpamAssassin")
            .build());

        assertThat(mailet.getSpamdPort()).isEqualTo(SpamAssassin.DEFAULT_PORT);
    }

    @Test
    public void initShouldThrowWhenSpamdPortIsNotNumber() throws Exception {
        assertThatThrownBy(() -> mailet.init(FakeMailetConfig.builder()
            .mailetName("SpamAssassin")
            .setProperty(SpamAssassin.SPAMD_PORT, "noNumber")
            .build())).isInstanceOf(MessagingException.class);
    }

    @Test
    public void initShouldSetSpamPortWhenPresent() throws Exception {
        int spamPort = 1000;
        mailet.init(FakeMailetConfig.builder()
            .mailetName("SpamAssassin")
            .setProperty(SpamAssassin.SPAMD_PORT, "" + spamPort)
            .build());

        assertThat(mailet.getSpamdPort()).isEqualTo(spamPort);
    }

    @Test
    public void serviceShouldWriteSpamAttributeOnMail() throws Exception {
        int port = PortUtil.getNonPrivilegedPort();
        new Thread(new MockSpamd(port)).start();

        FakeMailetConfig mailetConfiguration = FakeMailetConfig.builder()
            .mailetName("SpamAssassin")
            .setProperty(SpamAssassin.SPAMD_HOST, "localhost")
            .setProperty(SpamAssassin.SPAMD_PORT, "" + port)
            .build();
        mailet.init(mailetConfiguration);

        String rawMessage = "From: sender@example.com\r\n"
            + "To: User 1 <user1@example.com>\r\n"
            + "Subject: testing\r\n"
            + "\r\n"
            + "Please!";

        Mail mail = FakeMail.builder()
            .mimeMessage(MimeMessageBuilder.mimeMessageFromBytes(rawMessage.getBytes()))
            .build();

        mailet.service(mail);

        assertThat(mail.getAttributeNames())
            .containsOnly(SpamAssassinInvoker.FLAG_MAIL_ATTRIBUTE_NAME, SpamAssassinInvoker.STATUS_MAIL_ATTRIBUTE_NAME);
    }

    @Test
    public void serviceShouldWriteMessageAsNotSpamWhenNotSpam() throws Exception {
        int port = PortUtil.getNonPrivilegedPort();
        new Thread(new MockSpamd(port)).start();

        FakeMailetConfig mailetConfiguration = FakeMailetConfig.builder()
            .mailetName("SpamAssassin")
            .setProperty(SpamAssassin.SPAMD_HOST, "localhost")
            .setProperty(SpamAssassin.SPAMD_PORT, "" + port)
            .build();
        mailet.init(mailetConfiguration);

        String rawMessage = "From: sender@example.com\r\n"
            + "To: User 1 <user1@example.com>\r\n"
            + "Subject: testing\r\n"
            + "\r\n"
            + "Please!";

        Mail mail = FakeMail.builder()
            .mimeMessage(MimeMessageBuilder.mimeMessageFromBytes(rawMessage.getBytes()))
            .build();

        mailet.service(mail);

        assertThat(mail.getAttribute(SpamAssassinInvoker.FLAG_MAIL_ATTRIBUTE_NAME)).isEqualTo("NO");
    }

    @Test
    public void serviceShouldWriteMessageAsSpamWhenSpam() throws Exception {
        int port = PortUtil.getNonPrivilegedPort();
        new Thread(new MockSpamd(port)).start();

        FakeMailetConfig mailetConfiguration = FakeMailetConfig.builder()
            .mailetName("SpamAssassin")
            .setProperty(SpamAssassin.SPAMD_HOST, "localhost")
            .setProperty(SpamAssassin.SPAMD_PORT, "" + port)
            .build();
        mailet.init(mailetConfiguration);

        String rawMessage = "From: sender@example.com\r\n"
            + "To: User 1 <user1@example.com>\r\n"
            + "Subject: -SPAM- testing\r\n"
            + "\r\n"
            + "Please!";

        Mail mail = FakeMail.builder()
            .mimeMessage(MimeMessageBuilder.mimeMessageFromBytes(rawMessage.getBytes()))
            .build();

        mailet.service(mail);

        assertThat(mail.getAttribute(SpamAssassinInvoker.FLAG_MAIL_ATTRIBUTE_NAME)).isEqualTo("YES");
    }

    @Test
    public void getMailetInfoShouldReturnSpamAssasinMailetInformation() throws Exception {
        assertThat(mailet.getMailetInfo()).isEqualTo("Checks message against SpamAssassin");
    }

}