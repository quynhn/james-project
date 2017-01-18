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
package org.apache.james.jmap;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.jmap.mailet.VacationMailet;
import org.apache.james.jmap.methods.RequestHandler;
import org.apache.james.jmap.send.PostDequeueDecoratorFactory;
import org.apache.james.jmap.utils.HtmlTextExtractor;
import org.apache.james.jmap.utils.MailboxBasedHtmlTextExtractor;
import org.apache.james.jmap.utils.SystemMailboxesProvider;
import org.apache.james.jmap.utils.SystemMailboxesProviderImpl;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxManager.SearchCapabilities;
import org.apache.james.mailetcontainer.impl.MatcherMailetPair;
import org.apache.james.modules.server.CamelMailetContainerModule;
import org.apache.james.queue.api.MailQueueItemDecoratorFactory;
import org.apache.james.transport.matchers.RecipientIsLocal;
import org.apache.james.utils.ConfigurationPerformer;
import org.apache.james.utils.PropertiesProvider;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class JMAPModule extends AbstractModule {
    private static final int DEFAULT_JMAP_PORT = 80;

    @Override
    protected void configure() {
        install(new JMAPCommonModule());
        install(new MethodsModule());
        bind(JMAPServer.class).in(Scopes.SINGLETON);
        bind(RequestHandler.class).in(Scopes.SINGLETON);
        bind(UploadHandler.class).in(Scopes.SINGLETON);
        bind(MailboxBasedHtmlTextExtractor.class).in(Scopes.SINGLETON);
        bind(SystemMailboxesProviderImpl.class).in(Scopes.SINGLETON);

        bind(HtmlTextExtractor.class).to(MailboxBasedHtmlTextExtractor.class);
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(RequiredCapabilitiesPrecondition.class);

        Multibinder<CamelMailetContainerModule.TransportProcessorCheck> transportProcessorChecks = Multibinder.newSetBinder(binder(), CamelMailetContainerModule.TransportProcessorCheck.class);
        transportProcessorChecks.addBinding().to(VacationMailetCheck.class);
        
        bind(SystemMailboxesProvider.class).to(SystemMailboxesProviderImpl.class);
        bind(MailQueueItemDecoratorFactory.class).to(PostDequeueDecoratorFactory.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    JMAPConfiguration provideConfiguration(PropertiesProvider propertiesProvider, FileSystem fileSystem) throws ConfigurationException, IOException{
        PropertiesConfiguration configuration = propertiesProvider.getConfiguration("jmap");
        return JMAPConfiguration.builder()
                .keystore(configuration.getString("tls.keystoreURL"))
                .secret(configuration.getString("tls.secret"))
                .jwtPublicKeyPem(loadPublicKey(fileSystem, Optional.ofNullable(configuration.getString("jwt.publickeypem.url"))))
                .port(configuration.getInt("jmap.port", DEFAULT_JMAP_PORT))
                .build();
    }

    private Optional<String> loadPublicKey(FileSystem fileSystem, Optional<String> jwtPublickeyPemUrl) {
        return jwtPublickeyPemUrl.map(Throwing.function(url -> FileUtils.readFileToString(fileSystem.getFile(url))));
    }

    @Singleton
    public static class RequiredCapabilitiesPrecondition implements ConfigurationPerformer {

        private final MailboxManager mailboxManager;

        @Inject
        public RequiredCapabilitiesPrecondition(MailboxManager mailboxManager) {
            this.mailboxManager = mailboxManager;
        }

        @Override
        public void initModule() {
            Preconditions.checkArgument(mailboxManager.hasCapability(MailboxManager.MailboxCapabilities.Move),
                    "MOVE support in MailboxManager is required by JMAP Module");

            EnumSet<MailboxManager.MessageCapabilities> messageCapabilities = mailboxManager.getSupportedMessageCapabilities();
            Preconditions.checkArgument(messageCapabilities.contains(MailboxManager.MessageCapabilities.Attachment),
                    "Attachment support in MailboxManager is required by JMAP Module");
            Preconditions.checkArgument(messageCapabilities.contains(MailboxManager.MessageCapabilities.UniqueID),
                    "MessageIdManager is not defined by this Mailbox implementation");

            EnumSet<SearchCapabilities> searchCapabilities = mailboxManager.getSupportedSearchCapabilities();
            Preconditions.checkArgument(searchCapabilities.contains(MailboxManager.SearchCapabilities.MultimailboxSearch),
                    "Multimailbox search in MailboxManager is required by JMAP Module");
            Preconditions.checkArgument(searchCapabilities.contains(MailboxManager.SearchCapabilities.Text),
                    "Text support in MailboxManager is required by JMAP Module");
        }

        @Override
        public List<Class<? extends Configurable>> forClasses() {
            return ImmutableList.of();
        }
    }

    public static class VacationMailetCheck implements CamelMailetContainerModule.TransportProcessorCheck {
        @Override
        public void check(List<MatcherMailetPair> pairs) throws ConfigurationException {
            Preconditions.checkNotNull(pairs);
            pairs.stream()
                .filter(pair -> pair.getMailet().getClass().equals(VacationMailet.class))
                .filter(pair -> pair.getMatcher().getClass().equals(RecipientIsLocal.class))
                .findAny()
                .orElseThrow(() -> new ConfigurationException("Missing " + VacationMailet.class.getName() + " in mailets configuration (mailetcontainer -> processors -> transport)"));
        }
    }

}
