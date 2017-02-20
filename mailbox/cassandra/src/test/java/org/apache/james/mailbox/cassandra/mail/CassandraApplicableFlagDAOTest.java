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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.cassandra.modules.CassandraApplicableFlagModule;
import org.apache.james.mailbox.model.ApplicableFlag;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class CassandraApplicableFlagDAOTest {
    private CassandraCluster cassandra;

    private CassandraApplicableFlagDAO testee;

    @Before
    public void setUp() throws Exception {
        cassandra = CassandraCluster.create(new CassandraApplicableFlagModule());
        cassandra.ensureAllTables();

        testee = new CassandraApplicableFlagDAO(cassandra.getConf());
    }

    @After
    public void tearDown() throws Exception {
        cassandra.clearAllTables();
    }

    @Test
    public void deleteShouldNotThrowWhenRowDoesntExist() throws Exception {
        testee.delete(CassandraId.timeBased())
            .join();
    }

    @Test
    public void deleteShouldDeleteWhenRowExists() {
        CassandraId mailboxId = CassandraId.timeBased();
        ApplicableFlag applicableFlag = new ApplicableFlag(mailboxId, new Flags());
        testee.insert(applicableFlag)
            .join();

        testee.delete(mailboxId).join();

        Optional<ApplicableFlag> applicableFlagResult = testee.retrieveId(mailboxId).join();
        assertThat(applicableFlagResult.isPresent()).isFalse();
    }

    @Test
    public void deleteShouldDeleteOnlyConcernedRowWhenMultipleRowExists() {
        CassandraId mailboxId1 = CassandraId.timeBased();
        CassandraId mailboxId2 = CassandraId.timeBased();

        ApplicableFlag applicableFlag1 = new ApplicableFlag(mailboxId1, new Flags());
        ApplicableFlag applicableFlag2 = new ApplicableFlag(mailboxId2, new Flags());

        CompletableFuture.allOf(testee.insert(applicableFlag1),
            testee.insert(applicableFlag2))
            .join();

        testee.delete(mailboxId1).join();

        Optional<ApplicableFlag> deletedApplicableFlag = testee.retrieveId(mailboxId1).join();
        assertThat(deletedApplicableFlag.isPresent()).isFalse();
        Optional<ApplicableFlag> notDeletedApplicableFlag = testee.retrieveId(mailboxId2).join();
        assertThat(notDeletedApplicableFlag.isPresent()).isTrue();
    }

    @Test
    public void insertShouldStoreApplicableFlag() {
        CassandraId mailboxId = CassandraId.timeBased();

        ApplicableFlag applicableFlag = new ApplicableFlag(mailboxId, new Flags());
        testee.insert(applicableFlag)
            .join();

        Optional<ApplicableFlag> storedApplicableFlag = testee.retrieveId(mailboxId).join();
        assertThat(storedApplicableFlag.get()).isEqualTo(applicableFlag);
    }

    @Test
    public void updateShouldUpdateAnsweredFlag() {
        CassandraId mailboxId = CassandraId.timeBased();

        ApplicableFlag insertApplicableFlag = new ApplicableFlag(mailboxId, new Flags());
        testee.insert(insertApplicableFlag)
            .join();

        ApplicableFlag updateApplicableFlag = new ApplicableFlag(mailboxId, new Flags(Flag.ANSWERED));
        testee.updateApplicableFlags(updateApplicableFlag).join();

        ApplicableFlag applicableFlagResult = testee.retrieveId(mailboxId).join().get();
        assertThat(applicableFlagResult).isEqualTo(updateApplicableFlag);
        assertThat(applicableFlagResult.getFlags().contains(Flag.ANSWERED)).isTrue();
    }

    @Test
    public void updateShouldUpdateDeletedFlag() {
        CassandraId mailboxId = CassandraId.timeBased();

        ApplicableFlag insertApplicableFlag = new ApplicableFlag(mailboxId, new Flags());
        testee.insert(insertApplicableFlag)
            .join();

        ApplicableFlag updateApplicableFlag = new ApplicableFlag(mailboxId, new Flags(Flag.DELETED));
        testee.updateApplicableFlags(updateApplicableFlag).join();

        ApplicableFlag applicableFlagResult = testee.retrieveId(mailboxId).join().get();
        assertThat(applicableFlagResult).isEqualTo(updateApplicableFlag);
        assertThat(applicableFlagResult.getFlags().contains(Flag.DELETED)).isTrue();
    }

    @Test
    public void updateShouldUpdateDraftFlag() {
        CassandraId mailboxId = CassandraId.timeBased();

        ApplicableFlag insertApplicableFlag = new ApplicableFlag(mailboxId, new Flags());
        testee.insert(insertApplicableFlag)
            .join();

        ApplicableFlag updateApplicableFlag = new ApplicableFlag(mailboxId, new Flags(Flag.DRAFT));
        testee.updateApplicableFlags(updateApplicableFlag).join();

        ApplicableFlag applicableFlagResult = testee.retrieveId(mailboxId).join().get();
        assertThat(applicableFlagResult).isEqualTo(updateApplicableFlag);
        assertThat(applicableFlagResult.getFlags().contains(Flag.DRAFT)).isTrue();
    }

    @Test
    public void updateShouldUpdateFlaggedFlag() {
        CassandraId mailboxId = CassandraId.timeBased();

        ApplicableFlag insertApplicableFlag = new ApplicableFlag(mailboxId, new Flags());
        testee.insert(insertApplicableFlag)
            .join();

        ApplicableFlag updateApplicableFlag = new ApplicableFlag(mailboxId, new Flags(Flag.FLAGGED));
        testee.updateApplicableFlags(updateApplicableFlag).join();

        ApplicableFlag applicableFlagResult = testee.retrieveId(mailboxId).join().get();
        assertThat(applicableFlagResult).isEqualTo(updateApplicableFlag);
        assertThat(applicableFlagResult.getFlags().contains(Flag.FLAGGED)).isTrue();
    }

    @Test
    public void updateShouldUpdateSeenFlag() {
        CassandraId mailboxId = CassandraId.timeBased();

        ApplicableFlag insertApplicableFlag = new ApplicableFlag(mailboxId, new Flags());
        testee.insert(insertApplicableFlag)
            .join();

        ApplicableFlag updateApplicableFlag = new ApplicableFlag(mailboxId, new Flags(Flag.SEEN));
        testee.updateApplicableFlags(updateApplicableFlag).join();

        ApplicableFlag applicableFlagResult = testee.retrieveId(mailboxId).join().get();
        assertThat(applicableFlagResult).isEqualTo(updateApplicableFlag);
        assertThat(applicableFlagResult.getFlags().contains(Flag.SEEN)).isTrue();
    }

    @Test
    public void updateShouldUpdateUserFlags() {
        CassandraId mailboxId = CassandraId.timeBased();

        ApplicableFlag insertApplicableFlag = new ApplicableFlag(mailboxId, new Flags());
        testee.insert(insertApplicableFlag)
            .join();

        Flags newFlags = new Flags();
        newFlags.add("Any user flag");
        ApplicableFlag updateApplicableFlag = new ApplicableFlag(mailboxId, newFlags);
        testee.updateApplicableFlags(updateApplicableFlag).join();

        ApplicableFlag applicableFlagResult = testee.retrieveId(mailboxId).join().get();
        assertThat(applicableFlagResult).isEqualTo(updateApplicableFlag);
        assertThat(applicableFlagResult.getFlags().getUserFlags()).containsOnly("Any user flag");
    }

    @Test
    public void retrieveIdShouldRetrieveWhenKeyMatches() {
        CassandraId mailboxId = CassandraId.timeBased();

        ApplicableFlag applicableFlag = new ApplicableFlag(mailboxId, new Flags());
        testee.insert(applicableFlag)
            .join();

        Optional<ApplicableFlag> applicableFlagResult = testee.retrieveId(mailboxId).join();

        assertThat(applicableFlagResult.get()).isEqualTo(applicableFlag);
    }
}