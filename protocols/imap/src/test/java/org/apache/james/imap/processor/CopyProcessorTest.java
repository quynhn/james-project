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

package org.apache.james.imap.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.message.request.CopyRequest;
import org.apache.james.imap.message.request.MoveRequest;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.MailboxMetaData;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class CopyProcessorTest {

    public static final String TAG = "TAG";
    private static final Logger LOGGER = LoggerFactory.getLogger(CopyProcessorTest.class);

    private CopyProcessor testee;
    private ImapProcessor mockNextProcessor;
    private MailboxManager mockMailboxManager;
    private StatusResponseFactory mockStatusResponseFactory;
    private ImapProcessor.Responder mockResponder;
    private ImapSession mockImapSession;
    private MailboxSession mockMailboxSession;

    @Before
    public void setUp() {
        mockNextProcessor = mock(ImapProcessor.class);
        mockMailboxManager = mock(MailboxManager.class);
        mockStatusResponseFactory = mock(StatusResponseFactory.class);
        mockResponder = mock(ImapProcessor.Responder.class);
        mockImapSession = mock(ImapSession.class);
        mockMailboxSession = mock(MailboxSession.class);

        testee = new CopyProcessor(mockNextProcessor, mockMailboxManager, mockStatusResponseFactory);
    }

    @Test
    public void processShouldWork() throws Exception {
        CopyRequest copyRequest = new CopyRequest(ImapCommand.anyStateCommand("Name"), new IdRange[] {new IdRange(4, 6)}, ImapConstants.INBOX_NAME, true, TAG);

        MailboxSession.User user = mock(MailboxSession.User.class);
        when(user.getUserName()).thenReturn("username");
        when(mockMailboxSession.getPersonalSpace()).thenReturn("");
        when(mockMailboxSession.getUser()).thenReturn(user);
        when(mockMailboxSession.getSessionId()).thenReturn(42L);
        when(mockImapSession.getState()).thenReturn(ImapSessionState.SELECTED);
        when(mockImapSession.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY)).thenReturn(mockMailboxSession);
        MailboxPath inbox = MailboxPath.inbox(mockMailboxSession);
        MailboxPath selected = new MailboxPath(inbox, "selected");
        SelectedMailbox selectedMailbox = mock(SelectedMailbox.class);
        when(selectedMailbox.getLastUid()).thenReturn(8L);
        when(selectedMailbox.existsCount()).thenReturn(8L);
        when(selectedMailbox.getPath()).thenReturn(selected);
        when(mockImapSession.getSelected()).thenReturn(selectedMailbox);
        when(mockMailboxManager.mailboxExists(inbox, mockMailboxSession)).thenReturn(true);
        MessageManager targetMessageManager = mock(MessageManager.class);
        when(mockMailboxManager.getMailbox(inbox, mockMailboxSession)).thenReturn(targetMessageManager);
        when(targetMessageManager.getMetaData(false, mockMailboxSession, MessageManager.MetaData.FetchGroup.NO_UNSEEN)).thenReturn(new MailboxMetaData(null, null, 58L, 18L, 8L, 8L, 8L, 8L, true, true, null));
        StatusResponse okResponse = mock(StatusResponse.class);
        when(mockStatusResponseFactory.taggedOk(any(String.class), any(ImapCommand.class), any(HumanReadableText.class), any(StatusResponse.ResponseCode.class))).thenReturn(okResponse);
        when(mockMailboxManager.moveMessages(MessageRange.range(4, 6), selected, inbox, mockMailboxSession)).thenReturn(Lists.<MessageRange>newArrayList(MessageRange.range(4, 6)));

        testee.process(copyRequest, mockResponder, mockImapSession);

        verify(mockMailboxManager).startProcessingRequest(mockMailboxSession);
        verify(mockMailboxManager).endProcessingRequest(mockMailboxSession);
        verify(mockMailboxManager).mailboxExists(inbox, mockMailboxSession);
        verify(mockMailboxManager).getMailbox(inbox, mockMailboxSession);
        verify(mockMailboxManager).copyMessages(MessageRange.range(4, 6), selected, inbox, mockMailboxSession);
        verify(targetMessageManager).getMetaData(false, mockMailboxSession, MessageManager.MetaData.FetchGroup.NO_UNSEEN);
        verify(mockResponder).respond(okResponse);
        verifyNoMoreInteractions(mockMailboxManager, targetMessageManager, mockResponder, mockNextProcessor);
    }


    @Test
    public void processShouldWorkWithMultipleRanges() throws Exception {
        CopyRequest copyRequest = new CopyRequest(ImapCommand.anyStateCommand("Name"), new IdRange[] {new IdRange(5, 6), new IdRange(1, 3)}, ImapConstants.INBOX_NAME, true, TAG);

        MailboxSession.User user = mock(MailboxSession.User.class);
        when(user.getUserName()).thenReturn("username");
        when(mockMailboxSession.getPersonalSpace()).thenReturn("");
        when(mockMailboxSession.getUser()).thenReturn(user);
        when(mockMailboxSession.getSessionId()).thenReturn(42L);
        when(mockImapSession.getState()).thenReturn(ImapSessionState.SELECTED);
        when(mockImapSession.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY)).thenReturn(mockMailboxSession);
        MailboxPath inbox = MailboxPath.inbox(mockMailboxSession);
        MailboxPath selected = new MailboxPath(inbox, "selected");
        SelectedMailbox selectedMailbox = mock(SelectedMailbox.class);
        when(selectedMailbox.getLastUid()).thenReturn(8L);
        when(selectedMailbox.existsCount()).thenReturn(8L);
        when(selectedMailbox.getPath()).thenReturn(selected);
        when(mockImapSession.getSelected()).thenReturn(selectedMailbox);
        when(mockMailboxManager.mailboxExists(inbox, mockMailboxSession)).thenReturn(true);
        MessageManager targetMessageManager = mock(MessageManager.class);
        when(mockMailboxManager.getMailbox(inbox, mockMailboxSession)).thenReturn(targetMessageManager);
        when(targetMessageManager.getMetaData(false, mockMailboxSession, MessageManager.MetaData.FetchGroup.NO_UNSEEN)).thenReturn(new MailboxMetaData(null, null, 58L, 18L, 8L, 8L, 8L, 8L, true, true, null));
        StatusResponse okResponse = mock(StatusResponse.class);
        when(mockStatusResponseFactory.taggedOk(any(String.class), any(ImapCommand.class), any(HumanReadableText.class), any(StatusResponse.ResponseCode.class))).thenReturn(okResponse);

        testee.process(copyRequest, mockResponder, mockImapSession);

        verify(mockMailboxManager).startProcessingRequest(mockMailboxSession);
        verify(mockMailboxManager).endProcessingRequest(mockMailboxSession);
        verify(mockMailboxManager).mailboxExists(inbox, mockMailboxSession);
        verify(mockMailboxManager).getMailbox(inbox, mockMailboxSession);
        verify(mockMailboxManager).copyMessages(MessageRange.range(5, 6), selected, inbox, mockMailboxSession);
        verify(mockMailboxManager).copyMessages(MessageRange.range(1, 3), selected, inbox, mockMailboxSession);
        verify(targetMessageManager).getMetaData(false, mockMailboxSession, MessageManager.MetaData.FetchGroup.NO_UNSEEN);
        verify(mockResponder).respond(okResponse);
        verifyNoMoreInteractions(mockMailboxManager, targetMessageManager, mockResponder, mockNextProcessor);
    }

    @Test
    public void processShouldRespondNoOnUnExistingTargetMailbox() throws Exception {
        CopyRequest copyRequest = new CopyRequest(ImapCommand.anyStateCommand("Name"), new IdRange[] {new IdRange(4, 6)}, ImapConstants.INBOX_NAME, true, TAG);

        MailboxSession.User user = mock(MailboxSession.User.class);
        when(user.getUserName()).thenReturn("username");
        when(mockMailboxSession.getPersonalSpace()).thenReturn("");
        when(mockMailboxSession.getUser()).thenReturn(user);
        when(mockMailboxSession.getSessionId()).thenReturn(42L);
        when(mockImapSession.getState()).thenReturn(ImapSessionState.SELECTED);
        when(mockImapSession.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY)).thenReturn(mockMailboxSession);
        MailboxPath inbox = MailboxPath.inbox(mockMailboxSession);
        MailboxPath selected = new MailboxPath(inbox, "selected");
        SelectedMailbox selectedMailbox = mock(SelectedMailbox.class);
        when(selectedMailbox.getLastUid()).thenReturn(8L);
        when(selectedMailbox.existsCount()).thenReturn(8L);
        when(selectedMailbox.getPath()).thenReturn(selected);
        when(mockImapSession.getSelected()).thenReturn(selectedMailbox);
        when(mockMailboxManager.mailboxExists(inbox, mockMailboxSession)).thenReturn(false);

        StatusResponse noResponse = mock(StatusResponse.class);
        when(mockStatusResponseFactory.taggedNo(any(String.class), any(ImapCommand.class), any(HumanReadableText.class), any(StatusResponse.ResponseCode.class))).thenReturn(noResponse);

        testee.process(copyRequest, mockResponder, mockImapSession);

        verify(mockMailboxManager).startProcessingRequest(mockMailboxSession);
        verify(mockMailboxManager).endProcessingRequest(mockMailboxSession);
        verify(mockMailboxManager).mailboxExists(inbox, mockMailboxSession);
        verify(mockResponder).respond(noResponse);
        verifyNoMoreInteractions(mockMailboxManager, mockResponder, mockNextProcessor);
    }

    @Test
    public void processShouldRespondNoOnMailboxException() throws Exception {
        CopyRequest copyRequest = new CopyRequest(ImapCommand.anyStateCommand("Name"), new IdRange[] {new IdRange(4, 6)}, ImapConstants.INBOX_NAME, true, TAG);

        MailboxSession.User user = mock(MailboxSession.User.class);
        when(user.getUserName()).thenReturn("username");
        when(mockMailboxSession.getPersonalSpace()).thenReturn("");
        when(mockMailboxSession.getUser()).thenReturn(user);
        when(mockImapSession.getLog()).thenReturn(LOGGER);
        when(mockMailboxSession.getSessionId()).thenReturn(42L);
        when(mockImapSession.getState()).thenReturn(ImapSessionState.SELECTED);
        when(mockImapSession.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY)).thenReturn(mockMailboxSession);
        MailboxPath inbox = MailboxPath.inbox(mockMailboxSession);
        MailboxPath selected = new MailboxPath(inbox, "selected");
        SelectedMailbox selectedMailbox = mock(SelectedMailbox.class);
        when(selectedMailbox.getLastUid()).thenReturn(8L);
        when(selectedMailbox.existsCount()).thenReturn(8L);
        when(selectedMailbox.getPath()).thenReturn(selected);
        when(mockImapSession.getSelected()).thenReturn(selectedMailbox);
        when(mockMailboxManager.mailboxExists(inbox, mockMailboxSession)).thenThrow(new MailboxException());

        StatusResponse noResponse = mock(StatusResponse.class);
        when(mockStatusResponseFactory.taggedNo(any(String.class), any(ImapCommand.class), any(HumanReadableText.class))).thenReturn(noResponse);

        testee.process(copyRequest, mockResponder, mockImapSession);

        verify(mockMailboxManager).startProcessingRequest(mockMailboxSession);
        verify(mockMailboxManager).endProcessingRequest(mockMailboxSession);
        verify(mockMailboxManager).mailboxExists(inbox, mockMailboxSession);
        verify(mockResponder).respond(noResponse);
        verifyNoMoreInteractions(mockMailboxManager, mockResponder, mockNextProcessor);
    }

    @Test
    public void processShouldNotHandleMoveRequests() throws Exception {
        MoveRequest moveRequest = new MoveRequest(ImapCommand.anyStateCommand("Name"), new IdRange[] {new IdRange(4, 6)}, ImapConstants.INBOX_NAME, true, TAG);

        testee.process(moveRequest, mockResponder, mockImapSession);

        verify(mockNextProcessor).process(moveRequest, mockResponder, mockImapSession);
        verifyNoMoreInteractions(mockMailboxManager, mockResponder, mockNextProcessor);
    }

}
