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

import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import org.apache.mailet.base.GenericMailet;

import com.google.common.annotations.VisibleForTesting;

public class ICALBodyTextToAttachment extends GenericMailet {
    private static final String TEXT_MIME_TYPE = "text/calendar";

    @Override
    public String getMailetInfo() {
        return "Calendar save the bodyText as attachment";
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        MimeMessage message = getMessageFromMail(mail);
        if (isTextCalendar(message)) {
            processTextBodyAsAttachment(message);
        }
    }

    private boolean isTextCalendar(MimeMessage mimeMessage) throws MailetException {
        try {
            return mimeMessage.isMimeType(TEXT_MIME_TYPE);
        } catch (MessagingException e) {
            throw new MailetException("Could not retrieve contenttype of MimePart.", e);
        }
    }

    private MimeMessage getMessageFromMail(Mail mail) throws MailetException {
        try {
            return mail.getMessage();
        } catch (MessagingException e) {
            throw new MailetException("Could not retrieve message from Mail object", e);
        }
    }

    @VisibleForTesting
    void processTextBodyAsAttachment(MimeMessage mimeMessage) throws MailetException {
        if (!isTextCalendar(mimeMessage)) {
            return;
        }
        try {
            Multipart multipart = new MimeMultipart();
            SharedByteArrayInputStream content = (SharedByteArrayInputStream)mimeMessage.getContent();

            MimeBodyPart fileBody = new MimeBodyPart(content.newStream(0, mimeMessage.getSize()));
            fileBody.setDisposition(Part.ATTACHMENT);
            multipart.addBodyPart(fileBody);

            mimeMessage.setContent(multipart);
            mimeMessage.saveChanges();
        } catch (MessagingException | IOException e) {
            throw new MailetException("Could not retrieve message from Mail object", e);
        }
    }
}
