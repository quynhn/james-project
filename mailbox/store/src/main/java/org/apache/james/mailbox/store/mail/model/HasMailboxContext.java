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
package org.apache.james.mailbox.store.mail.model;

import javax.mail.Flags;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MailboxId;

public interface HasMailboxContext extends Comparable<HasMailboxContext> {

    /**
     * Return the mailbox id of the linked mailbox
     * 
     * @return mailboxId
     */
    MailboxId getMailboxId();

    /**
     * Return the uid
     */
    MessageUid getUid();

    /**
     * Return the mod-sequence for the message
     * 
     * @return message
     */
    long getModSeq();

    /**
     * Return if it was marked as answered
     * 
     * @return answered
     */
    boolean isAnswered();

    /**
     * Return if it was mark as deleted
     * 
     * @return deleted
     */
    boolean isDeleted();

    /**
     * Return if it was mark as draft
     * 
     * @return draft
     */
    boolean isDraft();

    /**
     * Return if it was flagged
     * 
     * @return flagged
     */
    boolean isFlagged();

    /**
     * Return if it was marked as recent
     * 
     * @return recent
     */
    boolean isRecent();

    /**
     * Return if it was marked as seen
     * 
     * @return seen
     */
    boolean isSeen();

    /**
     * Creates a new flags instance populated
     * with the current flag data.
     * 
     * @return new instance, not null
     */
    Flags createFlags();
}