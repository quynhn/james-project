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

package org.apache.james.mailbox.cassandra.mail;

import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.modules.CassandraAnnotationModule;
import org.apache.james.mailbox.cassandra.table.CassandraAnnotationTable;
import org.apache.james.mailbox.model.MailboxAnnotation;
import org.apache.james.mailbox.model.MailboxAnnotation.AnnotationValue;
import org.apache.james.mailbox.model.MailboxAnnotation.MailboxAnnotationEntryKey;
import org.apache.james.mailbox.model.MailboxAnnotation.MailboxAnnotationEntryValue;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.SimpleMailboxAnnotation;
import org.apache.james.mailbox.model.SimpleMailboxAnnotation.SimpleMailboxAnnotationCommand;
import org.apache.james.mailbox.model.SimpleMailboxAnnotation.SimpleMailboxAnnotationEntryKey;
import org.apache.james.mailbox.model.SimpleMailboxAnnotation.SimpleMailboxAnnotationEntryValue;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.datastax.driver.core.Session;

public class CassandraAnnotationMapperTest {
    private CassandraAnnotationMapper cassandraAnnotationMapper;
    private CassandraCluster cassandra;
    private SimpleMailbox<CassandraId> mailbox;
    private int uidValidity;
    private int maxRetry;
    private ExecutorService executor;

    @Before
    public void setUp() throws Exception {
        cassandra = CassandraCluster.create(new CassandraAnnotationModule());
        cassandra.ensureAllTables();
        uidValidity = 10;
        mailbox = new SimpleMailbox<>(new MailboxPath("#private", "benwa@linagora.com", "INBOX"), uidValidity);
        mailbox.setMailboxId(CassandraId.of(UUID.fromString("464765a0-e4e7-11e4-aba4-710c1de3782b")));
        maxRetry = 100;
        cassandraAnnotationMapper = new CassandraAnnotationMapper(mailbox, cassandra.getConf(), maxRetry);
        executor = Executors.newFixedThreadPool(2);
    }

    @After
    public void tearDown() throws Exception {
        cassandra.clearAllTables();
        executor.shutdownNow();
    }

    @Test(expected = IllegalArgumentException.class)
    public void creatingAnnotaionMapperWithNegativeMaxRetryShouldFail() {
        new CassandraAnnotationMapper(mailbox, cassandra.getConf(), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void creatingAnnotationMapperWithNullMaxRetryShouldFail() {
        new CassandraAnnotationMapper(mailbox, cassandra.getConf(), 0);
    }

    @Test
    public void retrieveEmptyAnnotation() throws Exception {
        assertThat(cassandraAnnotationMapper.getAnnotation()).isEqualTo(SimpleMailboxAnnotation.EMPTY);
    }

    @Test
    public void retrieveAnnotation() throws Exception {
        cassandra.getConf().execute(
                insertInto(CassandraAnnotationTable.TABLE_NAME)
                    .value(CassandraAnnotationTable.ID, mailbox.getMailboxId().asUuid())
                    .value(CassandraAnnotationTable.KEY, "/private/comment")
                    .value(CassandraAnnotationTable.VALUE, "My private comment")
            );

        Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> resultEntries = buildOneItem();
        MailboxAnnotation expectedResult = new SimpleMailboxAnnotation(resultEntries);

        assertThat(cassandraAnnotationMapper.getAnnotation()).isEqualTo(expectedResult);
    }

    @Test
    public void retrieveMultiAnnotations() throws Exception {
        Session currentSession = cassandra.getConf();
        currentSession.execute(
                insertInto(CassandraAnnotationTable.TABLE_NAME)
                    .value(CassandraAnnotationTable.ID, mailbox.getMailboxId().asUuid())
                    .value(CassandraAnnotationTable.KEY, "/private/comment")
                    .value(CassandraAnnotationTable.VALUE, "My private comment")
            );
        currentSession.execute(
                insertInto(CassandraAnnotationTable.TABLE_NAME)
                    .value(CassandraAnnotationTable.ID, mailbox.getMailboxId().asUuid())
                    .value(CassandraAnnotationTable.KEY, "/shared/comment")
                    .value(CassandraAnnotationTable.VALUE, "My shared comment")
            );

        Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> resultEntries = buildTwoItems();
        MailboxAnnotation expectedResult = new SimpleMailboxAnnotation(resultEntries);

        assertThat(cassandraAnnotationMapper.getAnnotation()).isEqualTo(expectedResult);
    }

    @Test
    public void addAnnotationWhenNoneStoredShouldReturnInsertAnnotation() throws Exception {
        SimpleMailboxAnnotationEntryKey key = new SimpleMailboxAnnotationEntryKey("/private/comment");
        SimpleMailboxAnnotationEntryValue value = new SimpleMailboxAnnotationEntryValue("My private comment", AnnotationValue.string);
        cassandraAnnotationMapper.updateAnnotation(new SimpleMailboxAnnotationCommand(key, value));
        assertThat(cassandraAnnotationMapper.getAnnotation()).isEqualTo(new SimpleMailboxAnnotation(buildOneItem()));
    }

    @Test
    public void addAnnotationWhenStoredShouldReturnUpdatedAnnotation() throws Exception {
        Session currentSession = cassandra.getConf();
        currentSession.execute(
                insertInto(CassandraAnnotationTable.TABLE_NAME)
                .value(CassandraAnnotationTable.ID, mailbox.getMailboxId().asUuid())
                .value(CassandraAnnotationTable.KEY, "/private/comment")
                .value(CassandraAnnotationTable.VALUE, "My stored private comment")
                );
        
        SimpleMailboxAnnotationEntryKey key = new SimpleMailboxAnnotationEntryKey("/private/comment");
        SimpleMailboxAnnotationEntryValue value = new SimpleMailboxAnnotationEntryValue("My private comment", AnnotationValue.string);
        cassandraAnnotationMapper.updateAnnotation(new SimpleMailboxAnnotationCommand(key, value));
        assertThat(cassandraAnnotationMapper.getAnnotation()).isEqualTo(new SimpleMailboxAnnotation(buildOneItem()));
    }

    @Test
    public void addNilAnnotationWhenNoneStoredShouldReturnEmpty() throws Exception {
        SimpleMailboxAnnotationEntryKey key = new SimpleMailboxAnnotationEntryKey("/private/comment");
        cassandraAnnotationMapper.updateAnnotation(new SimpleMailboxAnnotationCommand(key, SimpleMailboxAnnotationEntryValue.NIL));
        assertThat(cassandraAnnotationMapper.getAnnotation()).isEqualTo(SimpleMailboxAnnotation.EMPTY);
    }

    @Test
    public void addNilAnnotationWhenStoredShouldReturnEmpty() throws Exception {
        Session currentSession = cassandra.getConf();
        currentSession.execute(
                insertInto(CassandraAnnotationTable.TABLE_NAME)
                .value(CassandraAnnotationTable.ID, mailbox.getMailboxId().asUuid())
                .value(CassandraAnnotationTable.KEY, "/private/comment")
                .value(CassandraAnnotationTable.VALUE, "My stored private comment")
                );

        SimpleMailboxAnnotationEntryKey key = new SimpleMailboxAnnotationEntryKey("/private/comment");
        cassandraAnnotationMapper.updateAnnotation(new SimpleMailboxAnnotationCommand(key, SimpleMailboxAnnotationEntryValue.NIL));
        assertThat(cassandraAnnotationMapper.getAnnotation()).isEqualTo(SimpleMailboxAnnotation.EMPTY);
    }

    private Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> buildOneItem() {
        Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> resultEntries = 
                new HashMap<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue>();
        resultEntries.put(new SimpleMailboxAnnotationEntryKey("/private/comment"), 
                new SimpleMailboxAnnotationEntryValue("My private comment", AnnotationValue.string));
        return resultEntries;
    }

    private Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> buildTwoItems() {
        Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> resultEntries = buildOneItem();
        resultEntries.put(new SimpleMailboxAnnotationEntryKey("/shared/comment"), 
                new SimpleMailboxAnnotationEntryValue("My shared comment", AnnotationValue.string));
        return resultEntries;
    }
}
