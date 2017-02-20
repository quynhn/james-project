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

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static org.apache.james.mailbox.cassandra.table.CassandraApplicableFlagTable.FIELDS;
import static org.apache.james.mailbox.cassandra.table.CassandraApplicableFlagTable.MAILBOX_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraApplicableFlagTable.TABLE_NAME;
import static org.apache.james.mailbox.cassandra.table.Flag.ANSWERED;
import static org.apache.james.mailbox.cassandra.table.Flag.DELETED;
import static org.apache.james.mailbox.cassandra.table.Flag.DRAFT;
import static org.apache.james.mailbox.cassandra.table.Flag.FLAGGED;
import static org.apache.james.mailbox.cassandra.table.Flag.SEEN;
import static org.apache.james.mailbox.cassandra.table.Flag.USER_FLAGS;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.model.ApplicableFlag;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.collect.ImmutableSet;

public class CassandraApplicableFlagDAO {
    private final CassandraAsyncExecutor cassandraAsyncExecutor;
    private final PreparedStatement delete;
    private final PreparedStatement insert;
    private final PreparedStatement update;
    private final PreparedStatement select;

    @Inject
    public CassandraApplicableFlagDAO(Session session) {
        this.cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
        this.insert = prepareInsert(session);
        this.update = prepareUpdate(session);
        this.delete = prepareDelete(session);
        this.select = prepareSelect(session);
    }

    private PreparedStatement prepareDelete(Session session) {
        return session.prepare(QueryBuilder.delete()
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, bindMarker(MAILBOX_ID))));
    }

    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(TABLE_NAME)
            .value(MAILBOX_ID, bindMarker(MAILBOX_ID))
            .value(ANSWERED, bindMarker(ANSWERED))
            .value(DELETED, bindMarker(DELETED))
            .value(DRAFT, bindMarker(DRAFT))
            .value(FLAGGED, bindMarker(FLAGGED))
            .value(SEEN, bindMarker(SEEN))
            .value(USER_FLAGS, bindMarker(USER_FLAGS))
            .ifNotExists());
    }

    private PreparedStatement prepareUpdate(Session session) {
        return session.prepare(update(TABLE_NAME)
            .with(set(ANSWERED, bindMarker(ANSWERED)))
            .and(set(DELETED, bindMarker(DELETED)))
            .and(set(DRAFT, bindMarker(DRAFT)))
            .and(set(FLAGGED, bindMarker(FLAGGED)))
            .and(set(SEEN, bindMarker(SEEN)))
            .and(set(USER_FLAGS, bindMarker(USER_FLAGS)))
            .where(eq(MAILBOX_ID, bindMarker(MAILBOX_ID))));
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, bindMarker(MAILBOX_ID))));
    }

    public CompletableFuture<Optional<ApplicableFlag>> retrieveId(CassandraId mailboxId) {
        return cassandraAsyncExecutor.executeSingleRow(
            select.bind()
                .setUUID(MAILBOX_ID, mailboxId.asUuid()))
            .thenApply(rowOptional ->
                rowOptional.map(row -> new ApplicableFlag(
                    CassandraId.of(row.getUUID(MAILBOX_ID)),
                    new FlagsExtractor(row).getApplicableFlags())));
    }

    public CompletableFuture<Boolean> insert(ApplicableFlag applicableFlag) {
        CassandraId cassandraId = (CassandraId) applicableFlag.getMailboxId();
        Flags flags = applicableFlag.getFlags();

        return cassandraAsyncExecutor.executeReturnApplied(insert.bind()
            .setUUID(MAILBOX_ID, cassandraId.asUuid())
            .setBool(ANSWERED, flags.contains(Flag.ANSWERED))
            .setBool(DELETED, flags.contains(Flag.DELETED))
            .setBool(DRAFT, flags.contains(Flag.DRAFT))
            .setBool(FLAGGED, flags.contains(Flag.FLAGGED))
            .setBool(SEEN, flags.contains(Flag.SEEN))
            .setSet(USER_FLAGS, ImmutableSet.copyOf(flags.getUserFlags())));
    }

    public CompletableFuture<Void> updateApplicableFlags(ApplicableFlag applicableFlag) {
        CassandraId cassandraId = (CassandraId) applicableFlag.getMailboxId();
        Flags flags = applicableFlag.getFlags();

        return cassandraAsyncExecutor.executeVoid(update.bind()
            .setUUID(MAILBOX_ID, cassandraId.asUuid())
            .setBool(ANSWERED, flags.contains(Flag.ANSWERED))
            .setBool(DELETED, flags.contains(Flag.DELETED))
            .setBool(DRAFT, flags.contains(Flag.DRAFT))
            .setBool(FLAGGED, flags.contains(Flag.FLAGGED))
            .setBool(SEEN, flags.contains(Flag.SEEN))
            .setSet(USER_FLAGS, ImmutableSet.copyOf(flags.getUserFlags())));
    }

    public CompletableFuture<Void> delete(CassandraId cassandraId) {
        return cassandraAsyncExecutor.executeVoid(delete.bind()
            .setUUID(MAILBOX_ID, cassandraId.asUuid()));
    }

}
