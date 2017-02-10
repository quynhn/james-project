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

package org.apache.james.jmap.utils;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;

import com.google.common.annotations.VisibleForTesting;

public class SystemMailboxesProviderImpl implements SystemMailboxesProvider {

    private final MailboxManager mailboxManager;

    @Inject
    @VisibleForTesting
    public SystemMailboxesProviderImpl(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    public Stream<MessageManager> listMailboxes(Role aRole, MailboxSession session) throws MailboxException {
        MailboxPath mailboxPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, session.getUser().getUserName(), aRole.serialize());
        try {
            return Stream.of(mailboxManager.getMailbox(mailboxPath, session));
        } catch (MailboxNotFoundException e) {
            return Stream.of();
        }
    }
}
