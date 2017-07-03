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
import java.io.IOException;
import java.util.UUID;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.mailbox.cassandra.modules.CassandraBlobModule;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.Strings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CassandraBlobsDAOTest {
    private CassandraCluster cassandra;
    private CassandraBlobsDAO testee;

    @Before
    public void setUp() throws Exception {
        cassandra = CassandraCluster.create(new CassandraBlobModule());
        cassandra.ensureAllTables();

        testee = new CassandraBlobsDAO(cassandra.getConf());
    }

    @After
    public void tearDown() throws Exception {
        cassandra.clearAllTables();
        cassandra.close();
    }

    @Test
    public void readShouldBeEmptyWhenNoExisting() throws IOException {
        byte[] bytes = IOUtils.toByteArray(testee.read(UUIDs.timeBased()));

        assertThat(bytes).isEmpty();
    }

    @Test
    public void readShouldReturnSavedData() throws IOException {
        UUID uuid = testee.save("toto".getBytes(Charsets.UTF_8)).join();

        byte[] bytes = IOUtils.toByteArray(testee.read(uuid));

        assertThat(new String(bytes, Charsets.UTF_8)).isEqualTo("toto");
    }

    @Test
    public void readShouldReturnLongSavedData() throws IOException {
        String longString = Strings.repeat("0123456789\n", 1000);
        UUID uuid = testee.save(longString.getBytes(Charsets.UTF_8)).join();

        byte[] bytes = IOUtils.toByteArray(testee.read(uuid));

        assertThat(new String(bytes, Charsets.UTF_8)).isEqualTo(longString);
    }

    @Test
    public void readShouldReturnSplitSavedDataByChunk() throws IOException {
        String longString = Strings.repeat("0123456789\n", CassandraBlobsDAO.CHUNK_SIZE);
        UUID uuid = testee.save(longString.getBytes(Charsets.UTF_8)).join();

        byte[] bytes = IOUtils.toByteArray(testee.read(uuid));

        assertThat(new String(bytes, Charsets.UTF_8)).isEqualTo(longString);
    }

}