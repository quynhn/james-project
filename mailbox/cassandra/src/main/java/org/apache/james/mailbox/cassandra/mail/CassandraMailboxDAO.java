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
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxTable.FIELDS;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxTable.ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxTable.MAILBOX_BASE;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxTable.NAME;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxTable.PATH;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxTable.TABLE_NAME;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxTable.UIDVALIDITY;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.table.CassandraMailboxTable;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.apache.james.util.CompletableFutureUtil;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.querybuilder.QueryBuilder;

public class CassandraMailboxDAO {

    private final CassandraAsyncExecutor executor;
    private final CassandraTypesProvider typesProvider;
    private final Session session;
    private final int maxAclRetry;
    private final PreparedStatement readStatement;
    private final PreparedStatement listStatement;
    private final PreparedStatement deleteStatement;
    private final PreparedStatement insertStatement;
    private final PreparedStatement updateStatement;

    public CassandraMailboxDAO(Session session, CassandraTypesProvider typesProvider, int maxAclRetry) {
        this.executor = new CassandraAsyncExecutor(session);
        this.typesProvider = typesProvider;
        this.session = session;
        this.insertStatement = prepareInsert(session);
        this.updateStatement = prepareUpdate(session);
        this.deleteStatement = prepareDelete(session);
        this.listStatement = prepareList(session);
        this.readStatement = prepareRead(session);
        this.maxAclRetry = maxAclRetry;
    }

    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(TABLE_NAME)
            .value(ID, bindMarker(ID))
            .value(NAME, bindMarker(NAME))
            .value(UIDVALIDITY, bindMarker(UIDVALIDITY))
            .value(MAILBOX_BASE, bindMarker(MAILBOX_BASE))
            .value(PATH, bindMarker(PATH)));
    }

    private PreparedStatement prepareUpdate(Session session) {
        return session.prepare(update(TABLE_NAME)
            .with(set(MAILBOX_BASE, bindMarker(MAILBOX_BASE)))
            .and(set(NAME, bindMarker(NAME)))
            .and(set(PATH, bindMarker(PATH)))
            .where(eq(ID, bindMarker(ID))));
    }

    private PreparedStatement prepareDelete(Session session) {
        return session.prepare(QueryBuilder.delete()
            .from(TABLE_NAME)
            .where(eq(ID, bindMarker(ID))));
    }

    private PreparedStatement prepareList(Session session) {
        return session.prepare(select(FIELDS).from(TABLE_NAME));
    }

    private PreparedStatement prepareRead(Session session) {
        return session.prepare(select(FIELDS).from(TABLE_NAME)
            .where(eq(ID, bindMarker(ID))));
    }

    public CompletableFuture<Void> save(Mailbox mailbox) {
        CassandraId cassandraId = (CassandraId) mailbox.getMailboxId();
        return executor.executeVoid(insertStatement.bind()
            .setUUID(ID, cassandraId.asUuid())
            .setString(NAME, mailbox.getName())
            .setLong(UIDVALIDITY, mailbox.getUidValidity())
            .setUDTValue(MAILBOX_BASE, createMailboxBaseUDT(mailbox.getNamespace(), mailbox.getUser()))
            .setString(PATH, path(mailbox).asString()));
    }

    public CompletableFuture<Void> updatePath(CassandraId mailboxId, MailboxPath mailboxPath) {
        return executor.executeVoid(updateStatement.bind()
            .setUUID(ID, mailboxId.asUuid())
            .setString(PATH, mailboxPath.asString())
            .setString(NAME, mailboxPath.getName())
            .setUDTValue(MAILBOX_BASE, createMailboxBaseUDT(mailboxPath.getNamespace(), mailboxPath.getUser())));
    }

    private UDTValue createMailboxBaseUDT(String namespace, String user) {
        return typesProvider.getDefinedUserType(CassandraMailboxTable.MAILBOX_BASE)
            .newValue()
            .setString(CassandraMailboxTable.MailboxBase.NAMESPACE, namespace)
            .setString(CassandraMailboxTable.MailboxBase.USER, user);
    }

    public CompletableFuture<Void> delete(CassandraId mailboxId) {
        return executor.executeVoid(deleteStatement.bind()
            .setUUID(ID, mailboxId.asUuid()));
    }

    public CompletableFuture<Optional<SimpleMailbox>> retrieveMailbox(CassandraId mailboxId) {
        return mailbox(mailboxId,
            executor.executeSingleRow(readStatement.bind()
                .setUUID(ID, mailboxId.asUuid())));
    }

    private CompletableFuture<Optional<SimpleMailbox>> mailbox(CassandraId cassandraId, CompletableFuture<Optional<Row>> rowFuture) {
        CompletableFuture<MailboxACL> aclCompletableFuture = new CassandraACLMapper(cassandraId, session, maxAclRetry).getACL();
        return rowFuture.thenApply(rowOptional -> rowOptional.map(this::mailboxFromRow))
            .thenApply(mailboxOptional -> {
                mailboxOptional.ifPresent(mailbox -> mailbox.setMailboxId(cassandraId));
                return mailboxOptional;
            })
            .thenCompose(mailboxOptional -> aclCompletableFuture.thenApply(acl -> {
                mailboxOptional.ifPresent(mailbox -> mailbox.setACL(acl));
                return mailboxOptional;
            }));
    }

    private SimpleMailbox mailboxFromRow(Row row) {
        return new SimpleMailbox(
            new MailboxPath(
                row.getUDTValue(MAILBOX_BASE).getString(CassandraMailboxTable.MailboxBase.NAMESPACE),
                row.getUDTValue(MAILBOX_BASE).getString(CassandraMailboxTable.MailboxBase.USER),
                row.getString(NAME)),
            row.getLong(UIDVALIDITY));
    }

    private MailboxPath path(Mailbox mailbox) {
        return new MailboxPath(mailbox.getNamespace(), mailbox.getUser(), mailbox.getName());
    }

    public CompletableFuture<Stream<SimpleMailbox>> retrieveAllMailboxes() {
        return executor.execute(listStatement.bind())
            .thenApply(CassandraUtils::convertToStream)
            .thenApply(stream -> stream.map(this::toMailboxWithId))
            .thenCompose(stream -> CompletableFutureUtil.allOf(stream.map(this::toMailboxWithAclFuture)));
    }

    private SimpleMailbox toMailboxWithId(Row row) {
        SimpleMailbox mailbox = mailboxFromRow(row);
        mailbox.setMailboxId(CassandraId.of(row.getUUID(ID)));
        return mailbox;
    }

    private CompletableFuture<SimpleMailbox> toMailboxWithAclFuture(SimpleMailbox mailbox) {
        CassandraId cassandraId = (CassandraId) mailbox.getMailboxId();
        return new CassandraACLMapper(cassandraId, session, maxAclRetry).getACL()
            .thenApply(acl -> {
                mailbox.setACL(acl);
                return mailbox;
            });
    }

}
