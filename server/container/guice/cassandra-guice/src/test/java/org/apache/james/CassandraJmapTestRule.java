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

package org.apache.james;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.apache.james.http.jetty.ConfigurationException;
import org.apache.james.mailbox.extractor.TextExtractor;
import org.apache.james.mailbox.store.search.PDFTextExtractor;
import org.apache.james.modules.TestESMetricReporterModule;
import org.apache.james.modules.TestIMAPServerModule;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.modules.protocols.JMAPServerModule;
import org.apache.james.utils.ConfigurationProvider;
import org.apache.james.utils.FileConfigurationProvider;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.inject.Inject;
import com.google.inject.Module;


public class CassandraJmapTestRule implements TestRule {

    private static final int LIMIT_TO_3_MESSAGES = 3;

    public static CassandraJmapTestRule defaultTestRule() {
        return new CassandraJmapTestRule(new EmbeddedElasticSearchRule());
    }

    private GuiceModuleTestRule guiceModuleTestRule;

    public CassandraJmapTestRule(GuiceModuleTestRule... guiceModuleTestRule) {
        this.guiceModuleTestRule =
                AggregateGuiceModuleTestRule
                    .of(guiceModuleTestRule)
                    .aggregate(new TempFilesystemTestRule());
    }

    public GuiceJamesServer jmapServer(Module... additionals) {
        return new GuiceJamesServer()
            .combineWith(CassandraJamesServerMain.cassandraServerModule, CassandraJamesServerMain.protocols)
            .overrideWith(binder -> binder.bind(TextExtractor.class).to(PDFTextExtractor.class))
            .overrideWith(new TestJMAPServerModule(LIMIT_TO_3_MESSAGES))
            .overrideWith(new TestESMetricReporterModule())
            .overrideWith(guiceModuleTestRule.getModule())
            .overrideWith(additionals)
            .overrideWith(binder -> binder.bind(ConfigurationProvider.class).to(UnionConfigurationProvider.class));
    }

    private static class UnionConfigurationProvider implements ConfigurationProvider {
        private final FileConfigurationProvider fileConfigurationProvider;

        @Inject
        public UnionConfigurationProvider(FileConfigurationProvider fileConfigurationProvider) {
            this.fileConfigurationProvider = fileConfigurationProvider;
        }

        @Override
        public HierarchicalConfiguration getConfiguration(String component) throws org.apache.commons.configuration.ConfigurationException {
            if (component.equals("imapserver")) {
                return imapConfiguration();
            } else if (component.equals("lmtpserver")) {
                return lmtpConfiguration();
            } else if (component.equals("managesieveserver")) {
                return manageSieveConfiguration();
            } else if (component.equals("pop3server")) {
                return pop3Configuration();
            } else if (component.equals("smtpserver")) {
                return smtpConfiguration();
            }
            return fileConfigurationProvider.getConfiguration(component);
        }

        private HierarchicalConfiguration smtpConfiguration() {
            HierarchicalConfiguration configuration = commonProperties();
            configuration.addProperty("[@bind]", "0.0.0.0:1026");

            return configuration;
        }

        private HierarchicalConfiguration commonProperties() {
            PropertyListConfiguration configuration = new PropertyListConfiguration();
            configuration.addProperty("[@connectionBacklog]", "200");
            configuration.addProperty("[@keystore]", "file://conf/keystore");
            configuration.addProperty("[@secret]", "james72laBalle");
            configuration.addProperty("[@provider]", "org.bouncycastle.jce.provider.BouncyCastleProvider");
            return configuration;
        }

        private HierarchicalConfiguration pop3Configuration() {
            HierarchicalConfiguration configuration = commonProperties();
            configuration.addProperty("[@bind]", "0.0.0.0:1111");
            configuration.addProperty("[@]", "");
            return configuration;
        }

        private HierarchicalConfiguration manageSieveConfiguration() {
            HierarchicalConfiguration configuration = commonProperties();
            configuration.addProperty("[@bind]", "0.0.0.0:4150");
            configuration.addProperty("[@jmxName]", "managesieveserver");
            configuration.addProperty("[@algorithm]", "SunX509");
            return configuration;
        }

        private HierarchicalConfiguration imapConfiguration() throws ConfigurationException {
            PropertyListConfiguration configuration = new PropertyListConfiguration();
            configuration.addProperty("[@bind]", "0.0.0.0:1144");
            return configuration;
        }
        private HierarchicalConfiguration lmtpConfiguration() throws ConfigurationException {
            PropertyListConfiguration configuration = new PropertyListConfiguration();
            configuration.addProperty("[@bind]", "127.0.0.1:1025");
            return configuration;
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return guiceModuleTestRule.apply(base, description);
    }

    public void await() {
        guiceModuleTestRule.await();
    }
}
