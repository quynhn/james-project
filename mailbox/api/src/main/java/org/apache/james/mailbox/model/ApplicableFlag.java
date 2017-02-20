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

package org.apache.james.mailbox.model;

import java.util.Set;

import javax.mail.Flags;

import com.google.common.base.Objects;

public class ApplicableFlag {
    private final MailboxId mailboxId;
    private final Flags flags;

    public ApplicableFlag(MailboxId mailboxId, Flags flags) {
        this.mailboxId = mailboxId;
        this.flags = flags;
    }

    public MailboxId getMailboxId() {
        return mailboxId;
    }

    public Flags getFlags() {
        return flags;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof ApplicableFlag) {
            ApplicableFlag that = (ApplicableFlag) o;

            return Objects.equal(this.mailboxId, that.mailboxId)
                && Objects.equal(this.flags, that.flags);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(mailboxId, flags);
    }

}
