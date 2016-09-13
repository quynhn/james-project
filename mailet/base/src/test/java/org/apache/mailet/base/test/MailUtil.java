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

package org.apache.mailet.base.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * some utilities for James unit testing
 */
public class MailUtil {

    private static final Session NO_SESSION = null;
    
    private static int m_counter = 0;

    public static String newId() {
        m_counter++;
        return "MockMailUtil-ID-" + m_counter;
    }

    public static FakeMail createMockMail2Recipients() throws MessagingException {
        return FakeMail.builder()
                .name(newId())
                .recipients(new MailAddress("test@james.apache.org"), new MailAddress("test2@james.apache.org"))
                .build();
    }

    public static FakeMail createMockMail2Recipients(MimeMessage message) throws MessagingException {
        return FakeMail.builder()
                .name(newId())
                .mimeMessage(message)
                .recipients(new MailAddress("test@james.apache.org"), new MailAddress("test2@james.apache.org"))
                .build();
    }

    public static MimeMessage createMimeMessage() throws MessagingException {
        return createMimeMessage(null, null);
    }
    
    public static MimeMessage createMimeMessageWithSubject(String subject) throws MessagingException {
        return createMimeMessage(null, null, subject);
    }

    public static MimeMessage createMimeMessage(String headerName, String headerValue) throws MessagingException {
        return createMimeMessage(headerName, headerValue, "testmail");
    }
    
    private static MimeMessage createMimeMessage(String headerName, String headerValue, String subject) throws MessagingException {
        String sender = "test@james.apache.org";
        String rcpt = "test2@james.apache.org";

        MimeMessage mimeMessage = new MimeMessage(NO_SESSION);
        mimeMessage.setFrom(new InternetAddress(sender));
        mimeMessage.setRecipients(MimeMessage.RecipientType.TO, rcpt);
        if (headerName != null) mimeMessage.setHeader(headerName, headerValue);
        if (subject != null) mimeMessage.setSubject(subject);
        mimeMessage.setText("testtext");
        mimeMessage.saveChanges();
        return mimeMessage;
    }
    
    public static String toString(Mail mail, String charset) throws IOException, MessagingException {
        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mail.getMessage().writeTo(
                rawMessage,
                new String[] { "Bcc", "Content-Length", "Message-ID" });
        return rawMessage.toString(charset);
    }

}
