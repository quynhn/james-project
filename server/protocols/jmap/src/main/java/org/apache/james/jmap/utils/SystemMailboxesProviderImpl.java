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

import org.apache.james.jmap.exceptions.MailboxRoleNotFoundException;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MailboxQuery;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.ThrowingFunction;
import com.google.common.annotations.VisibleForTesting;

public class SystemMailboxesProviderImpl implements SystemMailboxesProvider {

    private final MailboxManager mailboxManager;

    @Inject
    public SystemMailboxesProviderImpl(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    private boolean hasRole(Role aRole, MailboxPath mailBoxPath) {
        return Role.from(mailBoxPath.getName())
                .map(aRole::equals)
                .orElse(false);
    }

    public Stream<MessageManager> listMailboxes(Role aRole, MailboxSession session) throws MailboxException {
        ThrowingFunction<MailboxPath, MessageManager> loadMailbox = path -> mailboxManager.getMailbox(path, session);
        return mailboxManager.search(MailboxQuery.builder(session).privateUserMailboxes().build(), session)
            .stream()
            .map(MailboxMetaData::getPath)
            .filter(path -> hasRole(aRole, path))
            .map(Throwing.function(loadMailbox).sneakyThrow());
    }

    public MessageManager findMailbox(Role role, MailboxSession session) throws MailboxException {
        return listMailboxes(role, session).findAny()
            .orElseThrow(() -> new MailboxRoleNotFoundException(role));
    }
}
