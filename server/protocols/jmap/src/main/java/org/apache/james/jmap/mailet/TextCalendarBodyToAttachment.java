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

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;

import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;

/**
 * This mailet convert Content-Type of MimeMessage from text/calendar to mulitpart/mixed
 *
 * The BodyPart should be retrieved from content of text/calendar with
 * - The same content-type from original message
 * - The same content-transfer-encoding from original message
 *
 * <br />
 * It does not takes any parameter
 *
 * Sample configuration:
 * <p/>
 * <pre><code>
 * &lt;mailet match="All" class="TextCalendarBodyToAttachment"/&gt;
 * </code></pre>
 *
 */
public class TextCalendarBodyToAttachment extends GenericMailet {
    private static final String TEXT_CALENDAR_TYPE = "text/calendar";
    private static final String CONTENT_HEADER_PREFIX = "Content-";

    @Override
    public String getMailetInfo() {
        return "Moves body part of content type text/calendar to attachment";
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        MimeMessage mimeMessage = mail.getMessage();
        if (mimeMessage.isMimeType(TEXT_CALENDAR_TYPE)) {
            processTextBodyAsAttachment(mimeMessage);
        }
    }

    @VisibleForTesting
    void processTextBodyAsAttachment(MimeMessage mimeMessage) throws MessagingException {
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(createMimeBodyPartFromMimeMessage(mimeMessage));

        mimeMessage.setContent(multipart);
        mimeMessage.saveChanges();
    }

    private MimeBodyPart createMimeBodyPartFromMimeMessage(MimeMessage mimeMessage) throws MessagingException {
        MimeBodyPart fileBody = createMimeBodyPartAndMoveContentHeadersFromMimeMessageToMimePart(mimeMessage);

        fileBody.setDisposition(Part.ATTACHMENT);
        return fileBody;
    }

    private MimeBodyPart createMimeBodyPartAndMoveContentHeadersFromMimeMessageToMimePart(MimeMessage mimeMessage) throws MessagingException {
        MimeBodyPart fileBody = new MimeBodyPart(mimeMessage.getRawInputStream());
        List<Header> contentHeaders = Collections.list((Enumeration<Header>) mimeMessage.getAllHeaders())
            .stream()
            .filter(header -> header.getName().startsWith(CONTENT_HEADER_PREFIX))
            .collect(Guavate.toImmutableList());

        for (Header header : contentHeaders) {
            fileBody.setHeader(header.getName(), header.getValue());
            mimeMessage.removeHeader(header.getName());
        }

        return fileBody;
    }

}
