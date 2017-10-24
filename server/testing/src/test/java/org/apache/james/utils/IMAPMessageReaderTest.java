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

package org.apache.james.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.net.imap.IMAPClient;
import org.junit.Test;

public class IMAPMessageReaderTest {
    IMAPClient imapClient = mock(IMAPClient.class);

    @Test
    public void userReceivedMessageWithFlagsInMailboxShouldReturnTrueWhenSingleFlag() throws Exception {
        when(imapClient.getReplyString()).thenReturn("* 1 FETCH (FLAGS (\\Flagged) )\n" +
            "AAAC OK FETCH completed.");
        IMAPMessageReader testee = new IMAPMessageReader(imapClient);

        assertThat(testee.userReceivedMessageWithFlagsInMailbox("user", "password", "mailbox", "\\Flagged"))
            .isTrue();
    }

    @Test
    public void userReceivedMessageWithFlagsInMailboxShouldReturnTrueWhenSeveralFlags() throws Exception {
        when(imapClient.getReplyString()).thenReturn("* 1 FETCH (FLAGS (\\Flagged \\Seen) )\n" +
            "AAAC OK FETCH completed.");
        IMAPMessageReader testee = new IMAPMessageReader(imapClient);

        assertThat(testee.userReceivedMessageWithFlagsInMailbox("user", "password", "mailbox", "\\Flagged \\Seen"))
            .isTrue();
    }

    @Test
    public void userReceivedMessageWithFlagsInMailboxShouldReturnTrueWhenSeveralFlagsInAnyOrder() throws Exception {
        when(imapClient.getReplyString()).thenReturn("* 1 FETCH (FLAGS (\\Flagged \\Seen) )\n" +
            "AAAC OK FETCH completed.");
        IMAPMessageReader testee = new IMAPMessageReader(imapClient);

        assertThat(testee.userReceivedMessageWithFlagsInMailbox("user", "password", "mailbox", "\\Seen \\Flagged"))
            .isTrue();
    }
}