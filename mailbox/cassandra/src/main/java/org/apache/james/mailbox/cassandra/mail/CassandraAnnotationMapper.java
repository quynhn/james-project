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

import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.james.backends.cassandra.utils.CassandraConstants;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.backends.cassandra.utils.FunctionRunnerWithRetry;
import org.apache.james.backends.cassandra.utils.LightweightTransactionException;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.table.CassandraAnnotationTable;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxAnnotation;
import org.apache.james.mailbox.model.MailboxAnnotation.AnnotationValue;
import org.apache.james.mailbox.model.MailboxAnnotation.MailboxAnnotationCommand;
import org.apache.james.mailbox.model.MailboxAnnotation.MailboxAnnotationEntryKey;
import org.apache.james.mailbox.model.MailboxAnnotation.MailboxAnnotationEntryValue;
import org.apache.james.mailbox.model.SimpleMailboxAnnotation;
import org.apache.james.mailbox.model.SimpleMailboxAnnotation.SimpleMailboxAnnotationEntryKey;
import org.apache.james.mailbox.model.SimpleMailboxAnnotation.SimpleMailboxAnnotationEntryValue;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Preconditions;

public class CassandraAnnotationMapper {

    @FunctionalInterface
    public interface CodeInjector {
        void inject();
    }

    private final Mailbox<CassandraId> mailbox;
    private final Session session;
    private final int maxRetry;
    private final CodeInjector codeInjector;

    private static final Logger LOG = LoggerFactory.getLogger(CassandraAnnotationMapper.class);

    public CassandraAnnotationMapper(Mailbox<CassandraId> mailbox, Session session, int maxRetry) {
        this(mailbox, session, maxRetry, () -> {
        });
    }

    public CassandraAnnotationMapper(Mailbox<CassandraId> mailbox, Session session, int maxRetry,
            CodeInjector codeInjector) {
        Preconditions.checkArgument(maxRetry > 0);
        this.mailbox = mailbox;
        this.session = session;
        this.maxRetry = maxRetry;
        this.codeInjector = codeInjector;
    }

    public MailboxAnnotation getAnnotation() {
        ResultSet resultSet = getStoredAnnotationRow();
        if (resultSet.isExhausted()) {
            return SimpleMailboxAnnotation.EMPTY;
        }
        Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> annos = CassandraUtils.convertToStream(resultSet)
                .map(row -> new AbstractMap.SimpleEntry<>(buildKey(row.getString(CassandraAnnotationTable.KEY)),
                        buildValue(row.getString(CassandraAnnotationTable.VALUE))))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        return new SimpleMailboxAnnotation(annos);
    }

    public void updateAnnotation(MailboxAnnotationCommand command) throws MailboxException {
        try {
            new FunctionRunnerWithRetry(maxRetry).execute(() -> {codeInjector.inject();
                if (SimpleMailboxAnnotationEntryValue.NIL.equals(command.getEntryValue())) {
                    deleteStoredAnnotation(command);
                    return true;
                } else {
                    ResultSet resultSet = getStoredAnnotationRowByKey(command.getEntryKey())
                            .map((row) -> updateStoredAnnotation(command, row))
                            .orElseGet(() -> insertAnnotation(command));
                    return resultSet.one().getBool(CassandraConstants.LIGHTWEIGHT_TRANSACTION_APPLIED);
                }
            });
        } catch (LightweightTransactionException e) {
            LOG.error("Exception during lightweight transaction: " + e.getMessage());
            throw new MailboxException("Exception during lightweight transaction", e);
        }
    }

    private ResultSet deleteStoredAnnotation(MailboxAnnotationCommand command) {
        return session.execute(delete().from(CassandraAnnotationTable.TABLE_NAME)
                .where(eq(CassandraAnnotationTable.ID, mailbox.getMailboxId().asUuid()))
                .and(eq(CassandraAnnotationTable.KEY, command.getEntryKey().getName())).ifExists());
    }

    private ResultSet updateStoredAnnotation(MailboxAnnotationCommand command, Row row) {
        return session.execute(update(CassandraAnnotationTable.TABLE_NAME)
                .with(set(CassandraAnnotationTable.VALUE, command.getEntryValue().getValue()))
                .where(eq(CassandraAnnotationTable.ID, mailbox.getMailboxId().asUuid()))
                .and(eq(CassandraAnnotationTable.KEY, row.getString(CassandraAnnotationTable.KEY))).ifExists());
    }

    private ResultSet insertAnnotation(MailboxAnnotationCommand command) {
        return session.execute(insertInto(CassandraAnnotationTable.TABLE_NAME)
                .value(CassandraAnnotationTable.ID, mailbox.getMailboxId().asUuid())
                .value(CassandraAnnotationTable.KEY, command.getEntryKey().getName())
                .value(CassandraAnnotationTable.VALUE, command.getEntryValue().getValue()).ifNotExists());
    }

    private MailboxAnnotationEntryKey buildKey(String value) {
        return new SimpleMailboxAnnotationEntryKey(value);
    }

    private MailboxAnnotationEntryValue buildValue(String value) {
        return new SimpleMailboxAnnotationEntryValue("" + value, AnnotationValue.string);
    }

    private Optional<Row> getStoredAnnotationRowByKey(MailboxAnnotationEntryKey key) {
        return Optional.ofNullable(session.execute(select(CassandraAnnotationTable.KEY, CassandraAnnotationTable.VALUE)
                .from(CassandraAnnotationTable.TABLE_NAME)
                .where(eq(CassandraAnnotationTable.ID, mailbox.getMailboxId().asUuid()))
                .and(eq(CassandraAnnotationTable.KEY, key.getName()))).one());
    }

    private ResultSet getStoredAnnotationRow() {
        return session.execute(select(CassandraAnnotationTable.KEY, CassandraAnnotationTable.VALUE)
                .from(CassandraAnnotationTable.TABLE_NAME)
                .where(eq(CassandraAnnotationTable.ID, mailbox.getMailboxId().asUuid())));
    }

}
