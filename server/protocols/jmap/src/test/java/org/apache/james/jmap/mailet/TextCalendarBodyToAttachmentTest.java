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

package org.apache.james.jmap.mailet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.apache.mailet.base.test.MimeMessageBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.steveash.guavate.Guavate;

public class TextCalendarBodyToAttachmentTest {
    private static final String MESSAGE_CONTENT = "Return-Path: <local@localhost.com>\n" +
        "MIME-Version: 1.0\n" +
        "X-Classification-Guess: {\"mailboxId\":\"ef6890c0-243a-11e7-b008-ddd22b16a7b9\",\"mailboxName\":\"Localhost\",\"confidence\":94.36473846435547}\n" +
        "Delivered-To: to@localhost.com\n" +
        "Received: from smtp.localhost.dc1 (EHLO smtp.localhost.com)\n" +
        "          by james.localhost.com (JAMES SMTP Server ) with ESMTP ID 1407088133\n" +
        "          for <user@james.localhost.com>;\n" +
        "          Wed, 26 Jul 2017 08:33:46 +0000 (UTC)\n" +
        "Received: from [10.11.0.85] (unknown [88.208.91.78])\n" +
        "\t(using TLSv1 with cipher DHE-RSA-AES128-SHA (128/128 bits))\n" +
        "\t(No client certificate requested)\n" +
        "\tby smtp.localhost.com (Postfix) with ESMTPSA id 9034565A\n" +
        "\tfor <user@loalhost.com>; Wed, 26 Jul 2017 10:34:02 +0200 (CEST)\n" +
        "From: User From <fromUser@localhost.com>\n" +
        "Message-ID: <2d0fa16c-4c37-58de-1acc-50b5f6a23a58@linagora.com>\n" +
        "To: User To <toUser@linagora.com>\n" +
        "Date: Wed, 26 Jul 2017 10:34:02 +0200\n" +
        "Subject: =?UTF-8?B?UsOpcG9uc2Ugw6AgbA==?=\n" +
        " =?UTF-8?B?4oCZaW52aXRhdGlvbiAoQWNjZXB0w6llKcKgOiBbQSBDT05GSVJNRVJd?=\n" +
        " =?UTF-8?Q?_Call_OVH?=\n" +
        "Content-class: urn:content-classes:calendarmessage\n" +
        "Content-Type: text/calendar; method=REPLY; charset=UTF-8\n" +
        "Content-transfer-encoding: 8BIT\n" +
        "\n" +
        "BEGIN:VCALENDAR\n" +
        "PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN\n" +
        "VERSION:2.0\n" +
        "METHOD:REPLY\n" +
        "BEGIN:VEVENT\n" +
        "CREATED:20170726T081145Z\n" +
        "LAST-MODIFIED:20170726T083402Z\n" +
        "DTSTAMP:20170726T083402Z\n" +
        "UID:f1514f44bf39311568d6407249cb76c48103fcd1fb398c3c501cb72b2d293f36e047fe\n" +
        " b2aab16e43439a608f28671ab7c10e754cbc85ddee4a07fa8cf8f4f7af05bfb502b8f69a1e\n" +
        "\n" +
        "SUMMARY:[A CONFIRMER] Call OVH\n" +
        "PRIORITY:5\n" +
        "ORGANIZER;CN=User;X-OBM-ID=486:mailto:userTo@localhost.com\n" +
        "ATTENDEE;CN=Yann PROVOST;PARTSTAT=ACCEPTED;CUTYPE=INDIVIDUAL;X-OBM-ID=810:\n" +
        " mailto:user1To@linagora.com\n" +
        "DTSTART:20170727T130000Z\n" +
        "DURATION:PT1H\n" +
        "TRANSP:OPAQUE\n" +
        "SEQUENCE:2\n" +
        "X-LIC-ERROR;X-LIC-ERRORTYPE=VALUE-PARSE-ERROR:No value for X property. Rem\n" +
        " oving entire property:\n" +
        "CLASS:PUBLIC\n" +
        "X-OBM-DOMAIN:linagora.com\n" +
        "X-OBM-DOMAIN-UUID:02874f7c-d10e-102f-acda-0015176f7922\n" +
        "LOCATION:01.78.14.42.61 / 1586\n" +
        "X-OBM-ALERT;X-OBM-ID=486:600\n" +
        "END:VEVENT\n" +
        "END:VCALENDAR\n" +
        "\n";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private TextCalendarBodyToAttachment mailet = new TextCalendarBodyToAttachment();

    @Before
    public void setUp() throws Exception {
        mailet.init(FakeMailetConfig.builder()
            .mailetName("TextCalendarBodyToAttachment")
            .build());
    }

    @Test
    public void getMailetInformationShouldReturnInformation() throws Exception {
        assertThat(mailet.getMailetInfo()).isEqualTo("Moves body part of content type text/calendar to attachment");
    }

    @Test
    public void serviceShouldThrowWhenCanNotGetMessageFromMail() throws Exception {
        expectedException.expect(MailetException.class);
        Mail mail = mock(Mail.class);
        when(mail.getMessage())
            .thenThrow(MessagingException.class);

        mailet.service(mail);
    }

    @Test
    public void serviceShouldThrowWhenMessageCanNotGetContentType() throws Exception {
        expectedException.expect(MailetException.class);

        MimeMessage message = mock(MimeMessage.class);
        Mail mail = FakeMail.from(message);

        when(message.isMimeType(anyString())).thenThrow(MessagingException.class);

        mailet.service(mail);
    }

    @Test
    public void serviceShouldKeepMessageAsItIsWhenMessageIsNotTextCalendar() throws Exception {
        String messageContent = "Content-type: text/html; method=REPLY; charset=UTF-8\n" +
            "Content-transfer-encoding: 8BIT\n" +
            "\n" +
            "BEGIN:VCALENDAR";
        MimeMessage message = MimeMessageBuilder.mimeMessageFromStream(new ByteArrayInputStream(messageContent.getBytes()));

        Mail mail = FakeMail.builder()
            .mimeMessage(message)
            .build();

        mailet.service(mail);

        assertThat(mail.getMessage().isMimeType("text/*")).isTrue();
        assertThat(mail.getMessage().getDisposition()).isNull();
    }

    @Test
    public void serviceShouldChangeMessageContentTypeToMultipart() throws Exception {
        String messageContent = "Content-type: text/calendar; method=REPLY; charset=UTF-8\n" +
            "Content-transfer-encoding: 8BIT\n" +
            "\n" +
            "BEGIN:VCALENDAR";
        MimeMessage message = MimeMessageBuilder.mimeMessageFromStream(new ByteArrayInputStream(messageContent.getBytes()));

        Mail mail = FakeMail.builder()
            .mimeMessage(message)
            .build();

        mailet.service(mail);

        assertThat(mail.getMessage().isMimeType("multipart/*")).isTrue();
    }

    @Test
    public void serviceShouldConvertTextBodyOfMessageToAttachmentWhenTextCalendar() throws Exception {
        String messageContent = "Content-type: text/calendar; method=REPLY; charset=UTF-8\n" +
            "Content-transfer-encoding: 8BIT\n" +
            "\n" +
            "BEGIN:VCALENDAR\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR";
        MimeMessage message = MimeMessageBuilder.mimeMessageFromStream(new ByteArrayInputStream(messageContent.getBytes()));

        Mail mail = FakeMail.builder()
            .mimeMessage(message)
            .build();

        mailet.service(mail);

        MimeMessage actual = mail.getMessage();
        Multipart multipart = (Multipart)actual.getContent();

        assertThat(multipart.getCount()).isEqualTo(1);
        assertThat(multipart.getBodyPart(0).getDisposition()).isEqualTo("attachment");
    }

    @Test
    public void serviceShouldKeepAllMessageHeaderWhenConverting() throws Exception {
        MimeMessage message = MimeMessageBuilder.mimeMessageFromStream(new ByteArrayInputStream(MESSAGE_CONTENT.getBytes()));
        List<Header> allHeaders = Collections.list(message.getAllHeaders());
        List<String> allStringHeaders = allHeaders.stream()
            .map(Header::getName)
            .collect(Guavate.toImmutableList());

        Mail mail = FakeMail.builder()
            .mimeMessage(message)
            .build();

        mailet.service(mail);

        List<Header> actualHeaders = Collections.list(mail.getMessage().getAllHeaders());
        List<String> actualStringHeaders = actualHeaders
            .stream()
            .map(Header::getName)
            .collect(Guavate.toImmutableList());

        assertThat(actualStringHeaders).containsAll(allStringHeaders);
    }

    @Test
    public void contentTypeOfAttachmentShouldGetFromOriginalMessage() throws Exception {
        MimeMessage message = MimeMessageBuilder.mimeMessageFromStream(new ByteArrayInputStream(MESSAGE_CONTENT.getBytes()));

        Mail mail = FakeMail.builder()
            .mimeMessage(message)
            .build();

        mailet.service(mail);

        Multipart multipart = (Multipart)mail.getMessage().getContent();

        int firstBodyPart = 0;
        assertThat(multipart.getBodyPart(firstBodyPart).getContentType()).startsWith("text/calendar");
    }
}