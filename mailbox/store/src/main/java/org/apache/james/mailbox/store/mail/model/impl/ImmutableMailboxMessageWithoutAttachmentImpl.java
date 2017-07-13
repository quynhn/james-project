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

package org.apache.james.mailbox.store.mail.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.HasMailboxContext;
import org.apache.james.mailbox.store.mail.model.ImmutableMailboxContext;
import org.apache.james.mailbox.store.mail.model.ImmutableMailboxMessageWithoutAttachment;
import org.apache.james.mailbox.store.mail.model.MessageWithoutAttachment;
import org.apache.james.mailbox.store.mail.model.Property;

class ImmutableMailboxMessageWithoutAttachmentImpl implements ImmutableMailboxMessageWithoutAttachment {
    private final ImmutableMailboxContext mailboxContext;
    private final MessageWithoutAttachment message;

    ImmutableMailboxMessageWithoutAttachmentImpl(
        MessageWithoutAttachment message,
        ImmutableMailboxContext mailboxContext
    ) {
        this.message = message;
        this.mailboxContext = mailboxContext;
    }

    @Override
    public MessageId getMessageId() {
        return message.getMessageId();
    }

    @Override
    public Date getInternalDate() {
        return message.getInternalDate();
    }

    @Override
    public InputStream getBodyContent() throws IOException {
        return message.getBodyContent();
    }

    @Override
    public String getMediaType() {
        return message.getMediaType();
    }

    @Override
    public String getSubType() {
        return message.getSubType();
    }

    @Override
    public long getBodyOctets() {
        return message.getBodyOctets();
    }

    @Override
    public long getFullContentOctets() {
        return message.getFullContentOctets();
    }

    @Override
    public long getHeaderOctets() {
        return message.getHeaderOctets();
    }

    @Override
    public Long getTextualLineCount() {
        return message.getTextualLineCount();
    }

    @Override
    public InputStream getHeaderContent() throws IOException {
        return message.getHeaderContent();
    }

    @Override
    public InputStream getFullContent() throws IOException {
        return message.getFullContent();
    }

    @Override
    public List<Property> getProperties() {
        return message.getProperties();
    }

    @Override
    public MailboxId getMailboxId() {
        return mailboxContext.getMailboxId();
    }

    @Override
    public MessageUid getUid() {
        return mailboxContext.getUid();
    }

    @Override
    public long getModSeq() {
        return mailboxContext.getModSeq();
    }

    @Override
    public boolean isAnswered() {
        return mailboxContext.isAnswered();
    }

    @Override
    public boolean isDeleted() {
        return mailboxContext.isDeleted();
    }

    @Override
    public boolean isDraft() {
        return mailboxContext.isDraft();
    }

    @Override
    public boolean isFlagged() {
        return mailboxContext.isFlagged();
    }

    @Override
    public boolean isRecent() {
        return mailboxContext.isRecent();
    }

    @Override
    public boolean isSeen() {
        return mailboxContext.isSeen();
    }

    @Override
    public Flags createFlags() {
        return mailboxContext.createFlags();
    }

    @Override
    public int compareTo(HasMailboxContext o) {
        return mailboxContext.compareTo(o);
    }
}