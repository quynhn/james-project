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

package org.apache.james.mailbox.indexer.registrations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.store.event.EventFactory;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.Before;
import org.junit.Test;

public class GlobalRegistrationTest {
    public static final MailboxPath INBOX = MailboxPath.forUser("btellier@apache.org", "INBOX");
    public static final MailboxPath NEW_PATH = MailboxPath.forUser("btellier@apache.org", "INBOX.new");
    public static final int UID_VALIDITY = 45;
    public static final MailboxId MAILBOX_ID = TestId.of(1);
    public static final SimpleMailbox MAILBOX = new SimpleMailbox(INBOX, UID_VALIDITY, MAILBOX_ID);
    public static final SimpleMailbox NEW_MAILBOX = new SimpleMailbox(NEW_PATH, UID_VALIDITY, MAILBOX_ID);

    private GlobalRegistration globalRegistration;
    private EventFactory eventFactory;
    private MockMailboxSession session;

    @Before
    public void setUp() {
        eventFactory = new EventFactory();
        session = new MockMailboxSession("test");
        globalRegistration = new GlobalRegistration();
    }

    @Test
    public void pathToIndexShouldNotBeChangedByDefault() {
        assertThat(globalRegistration.getPathToIndex(MAILBOX_ID)).isNotPresent();
    }

    @Test
    public void pathToIndexShouldNotBeChangedByRenamedEvents() {
        MailboxListener.Event event = eventFactory.mailboxRenamed(session, INBOX, new SimpleMailbox(NEW_PATH, 42, MAILBOX_ID));
        globalRegistration.event(event);
        assertThat(globalRegistration.getPathToIndex(MAILBOX_ID).get()).isEqualTo(NEW_PATH);
    }

    @Test
    public void pathToIndexShouldBeNullifiedByDeletedEvents() {
        MailboxListener.Event event = eventFactory.mailboxDeleted(session, MAILBOX);
        globalRegistration.event(event);
        assertThat(globalRegistration.getPathToIndex(MAILBOX_ID)).isEqualTo(Optional.empty());
    }

    @Test
    public void pathToIndexShouldBeModifiedByRenamedEvents() {
        MailboxListener.Event event = eventFactory.mailboxRenamed(session, INBOX, NEW_MAILBOX);
        globalRegistration.event(event);
        assertThat(globalRegistration.getPathToIndex(MAILBOX_ID).get()).isEqualTo(NEW_PATH);
    }


}
