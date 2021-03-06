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

package org.apache.james.mailbox.store.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.StoreMailboxPath;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxId;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class EventFactory<Id extends MailboxId> {

    public interface MailboxAware<LocalId extends MailboxId> {
        Mailbox<LocalId> getMailbox();
    }

    public final class AddedImpl<LocalId extends MailboxId> extends MailboxListener.Added implements MailboxAware<LocalId> {
        private final Map<Long, MessageMetaData> added;
        private final Mailbox<LocalId> mailbox;

        public AddedImpl(MailboxSession session, Mailbox<LocalId> mailbox, SortedMap<Long, MessageMetaData> added) {
            super(session, new StoreMailboxPath<LocalId>(mailbox));
            this.added = ImmutableMap.copyOf(added);
            this.mailbox = mailbox;
        }

        public List<Long> getUids() {
            return ImmutableList.copyOf(added.keySet());
        }

        public MessageMetaData getMetaData(long uid) {
            return added.get(uid);
        }

        public Mailbox<LocalId> getMailbox() {
            return mailbox;
        }
    }

    public final class ExpungedImpl<LocalId extends MailboxId> extends MailboxListener.Expunged implements MailboxAware<LocalId> {
        private final Map<Long, MessageMetaData> uids;
        private final Mailbox<LocalId> mailbox;

        public ExpungedImpl(MailboxSession session, Mailbox<LocalId> mailbox,  Map<Long, MessageMetaData> uids) {
            super(session,  new StoreMailboxPath<LocalId>(mailbox));
            this.uids = ImmutableMap.copyOf(uids);
            this.mailbox = mailbox;
        }

        public List<Long> getUids() {
            return ImmutableList.copyOf(uids.keySet());
        }

        public MessageMetaData getMetaData(long uid) {
            return uids.get(uid);
        }

        public Mailbox<LocalId> getMailbox() {
            return mailbox;
        }
    }

    public final class FlagsUpdatedImpl<LocalId extends MailboxId> extends MailboxListener.FlagsUpdated implements MailboxAware<LocalId> {
        private final List<Long> uids;

        private final Mailbox<LocalId> mailbox;

        private final List<UpdatedFlags> uFlags;

        public FlagsUpdatedImpl(MailboxSession session, Mailbox<LocalId> mailbox, List<Long> uids, List<UpdatedFlags> uFlags) {
            super(session, new StoreMailboxPath<LocalId>(mailbox));
            this.uids = ImmutableList.copyOf(uids);
            this.uFlags = ImmutableList.copyOf(uFlags);
            this.mailbox = mailbox;
        }

        public List<Long> getUids() {
            return uids;
        }

        public List<UpdatedFlags> getUpdatedFlags() {
            return uFlags;
        }

        public Mailbox<LocalId> getMailbox() {
            return mailbox;
        }

    }

    public final class MailboxDeletionImpl<LocalId extends MailboxId> extends MailboxListener.MailboxDeletion implements MailboxAware<LocalId> {
        private final Mailbox<LocalId> mailbox;

        public MailboxDeletionImpl(MailboxSession session, Mailbox<LocalId> mailbox) {
            super(session, new StoreMailboxPath<LocalId>(mailbox));
            this.mailbox = mailbox;
        }


        public Mailbox<LocalId> getMailbox() {
            return mailbox;
        }

    }

    public final class MailboxAddedImpl<LocalId extends MailboxId> extends MailboxListener.MailboxAdded implements MailboxAware<LocalId> {

        private final Mailbox<LocalId> mailbox;

        public MailboxAddedImpl(MailboxSession session, Mailbox<LocalId> mailbox) {
            super(session,  new StoreMailboxPath<LocalId>(mailbox));
            this.mailbox = mailbox;
        }


        public Mailbox<LocalId> getMailbox() {
            return mailbox;
        }

    }

    public final class MailboxRenamedEventImpl<LocalId extends MailboxId> extends MailboxListener.MailboxRenamed implements MailboxAware<LocalId> {

        private final MailboxPath newPath;
        private final Mailbox<LocalId> newMailbox;

        public MailboxRenamedEventImpl(MailboxSession session, MailboxPath oldPath, Mailbox<LocalId> newMailbox) {
            super(session, oldPath);
            this.newPath = new StoreMailboxPath<LocalId>(newMailbox);
            this.newMailbox = newMailbox;
        }

        public MailboxPath getNewPath() {
            return newPath;
        }

        @Override
        public Mailbox<LocalId> getMailbox() {
            return newMailbox;
        }
    }

    public MailboxListener.Added added(MailboxSession session, SortedMap<Long, MessageMetaData> uids, Mailbox<Id> mailbox) {
        return new AddedImpl<Id>(session, mailbox, uids);
    }

    public MailboxListener.Expunged expunged(MailboxSession session,  Map<Long, MessageMetaData> uids, Mailbox<Id> mailbox) {
        return new ExpungedImpl<Id>(session, mailbox, uids);
    }

    public MailboxListener.FlagsUpdated flagsUpdated(MailboxSession session, List<Long> uids, Mailbox<Id> mailbox, List<UpdatedFlags> uflags) {
        return new FlagsUpdatedImpl<Id>(session, mailbox, uids, uflags);
    }

    public MailboxListener.MailboxRenamed mailboxRenamed(MailboxSession session, MailboxPath from, Mailbox<Id> to) {
        return new MailboxRenamedEventImpl<Id>(session, from, to);
    }

    public MailboxListener.MailboxDeletion mailboxDeleted(MailboxSession session, Mailbox<Id> mailbox) {
        return new MailboxDeletionImpl<Id>(session, mailbox);
    }

    public MailboxListener.MailboxAdded mailboxAdded(MailboxSession session, Mailbox<Id> mailbox) {
        return new MailboxAddedImpl<Id>(session, mailbox);
    }

}
