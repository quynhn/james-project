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

package org.apache.james.mailbox.cassandra.mail;

import java.util.Date;
import java.util.List;
import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;

public class MessageWithoutAttachment {
    private final RawMessageWithoutAttachment message;
    private final Flags flags;
    private final MailboxId mailboxId;
    private final MessageUid messageUid;
    private final long modSeq;

    public MessageWithoutAttachment(RawMessageWithoutAttachment message, Flags flags,
                                    MailboxId mailboxId, MessageUid messageUid, long modSeq) {
        this.message = message;
        this.flags = flags;
        this.mailboxId = mailboxId;
        this.messageUid = messageUid;
        this.modSeq = modSeq;
    }

    public RawMessageWithoutAttachment toRawMessageWithoutAttachment() {
        return message;
    }

    public SimpleMailboxMessage toMailboxMessage(List<MessageAttachment> attachments) {
        return SimpleMailboxMessage.builder()
            .messageId(message.getMessageId())
            .mailboxId(mailboxId)
            .uid(messageUid)
            .modseq(modSeq)
            .internalDate(message.getInternalDate())
            .bodyStartOctet(message.getBodySize())
            .size(message.getSize())
            .content(message.getContent())
            .flags(flags)
            .propertyBuilder(message.getPropertyBuilder())
            .addAttachments(attachments)
            .build();
    }

    public MailboxId getMailboxId() {
        return mailboxId;
    }

    public MessageId getMessageId() {
        return message.getMessageId();
    }

    public ComposedMessageIdWithMetaData getMetadata() {
        return new ComposedMessageIdWithMetaData(new ComposedMessageId(mailboxId, message.getMessageId(), messageUid), flags, modSeq);
    }

    public SharedByteArrayInputStream getContent() {
        return message.getContent();
    }

    public PropertyBuilder getPropertyBuilder() {
        return message.getPropertyBuilder();
    }
}
