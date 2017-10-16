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

package org.apache.james.mailbox.store;

import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.TestMessageId;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.mailbox.store.quota.DefaultQuotaRootResolver;
import org.junit.Before;

public class StoreMessageIdManagerSideEffectTest extends AbstractMessageIdManagerSideEffectTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected MessageIdManagerTestSystem createTestSystem(QuotaManager quotaManager, MailboxEventDispatcher dispatcher) throws Exception {

        TestMailboxSessionMapperFactory testMailboxSessionMapperFactory = new TestMailboxSessionMapperFactory();
        MessageId.Factory messageIdFactory = new TestMessageId.Factory();
        MailboxACLResolver aclResolver = null;
        GroupMembershipResolver groupMembershipResolver = null;
        MessageIdManager messageIdManager = new StoreMessageIdManager(testMailboxSessionMapperFactory, dispatcher, messageIdFactory,
            quotaManager, new DefaultQuotaRootResolver(testMailboxSessionMapperFactory),
            aclResolver,
            groupMembershipResolver);

        return new StoreMessageIdManagerTestSystem(messageIdManager,
            messageIdFactory,
            testMailboxSessionMapperFactory);
    }

}
