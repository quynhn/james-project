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

import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

public class MessageResults {
    private final long total;
    private final List<MessageId> messageIds;

    public static MessageResults DEFAULT = new MessageResults(0, ImmutableList.of());

    public MessageResults(long total, List<MessageId> messageIds) {
        this.total = total;
        this.messageIds = messageIds;
    }

    public long getTotal() {
        return total;
    }

    public List<MessageId> getMessageIds() {
        return messageIds;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof MessageResults) {
            MessageResults that = (MessageResults) o;

            return Objects.equals(this.total, that.total)
                && Objects.equals(this.messageIds, that.messageIds);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(total, messageIds);
    }
}
