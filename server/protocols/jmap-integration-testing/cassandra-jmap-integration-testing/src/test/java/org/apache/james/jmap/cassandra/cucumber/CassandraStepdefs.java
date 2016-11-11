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

package org.apache.james.jmap.cassandra.cucumber;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.james.CassandraJamesServer;
import org.apache.james.CassandraJamesServerMain;
import org.apache.james.backends.cassandra.EmbeddedCassandra;
import org.apache.james.jmap.methods.integration.cucumber.MainStepdefs;
import org.apache.james.mailbox.elasticsearch.EmbeddedElasticSearch;
import org.apache.james.modules.CassandraJmapServerModule;
import org.junit.rules.TemporaryFolder;

import com.github.fge.lambdas.runnable.ThrowingRunnable;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.runtime.java.guice.ScenarioScoped;

@ScenarioScoped
public class CassandraStepdefs {

    private final MainStepdefs mainStepdefs;
    private TemporaryFolder temporaryFolder = new TemporaryFolder();
    private EmbeddedElasticSearch embeddedElasticSearch = new EmbeddedElasticSearch(temporaryFolder);
    private EmbeddedCassandra cassandra = EmbeddedCassandra.createStartServer();

    @Inject
    private CassandraStepdefs(MainStepdefs mainStepdefs) {
        this.mainStepdefs = mainStepdefs;
    }

    @Before
    public void init() throws Exception {
        temporaryFolder.create();
        embeddedElasticSearch.before();
        mainStepdefs.jmapServer = new CassandraJamesServer()
                .combineWith(CassandraJamesServerMain.cassandraServerModule)
                .overrideWith(new CassandraJmapServerModule(temporaryFolder, embeddedElasticSearch, cassandra));
        mainStepdefs.awaitMethod = () -> embeddedElasticSearch.awaitForElasticSearch();
        mainStepdefs.init();
    }

    @After
    public void tearDown() {
        ignoreFailures(() -> mainStepdefs.tearDown(),
                () -> embeddedElasticSearch.after(),
                () -> temporaryFolder.delete());
    }

    private void ignoreFailures(ThrowingRunnable... cleaners) {
        Arrays.stream(cleaners)
            .forEach(this::runSwallowingException);
    }
    
    private void runSwallowingException(Runnable run) {
        try {
            run.run();
        } catch (Exception e) {
            // ignore
        }
    }
}
