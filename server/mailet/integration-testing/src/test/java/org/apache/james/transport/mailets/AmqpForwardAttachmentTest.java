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

package org.apache.james.transport.mailets;

import static org.assertj.core.api.Assertions.assertThat;

import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.jmap.mailet.VacationMailet;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailets.TemporaryJamesServer;
import org.apache.james.mailets.configuration.CommonProcessors;
import org.apache.james.mailets.configuration.MailetConfiguration;
import org.apache.james.mailets.configuration.MailetContainer;
import org.apache.james.mailets.configuration.ProcessorConfiguration;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.probe.DataProbe;
import org.apache.james.transport.mailets.amqp.AmqpRule;
import org.apache.james.transport.matchers.All;
import org.apache.james.transport.matchers.RecipientIsLocal;
import org.apache.james.util.docker.Images;
import org.apache.james.util.docker.SwarmGenericContainer;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.SMTPMessageSender;
import org.apache.mailet.Mail;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.MimeMessageBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;

public class AmqpForwardAttachmentTest {

    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final int IMAP_PORT = 1143;
    private static final int SMTP_PORT = 1025;
    private static final String PASSWORD = "secret";

    private static final String JAMES_APACHE_ORG = "james.org";

    private static final String FROM = "fromUser@" + JAMES_APACHE_ORG;
    private static final String RECIPIENT = "touser@" + JAMES_APACHE_ORG;
    
    private static final String MAIL_ATTRIBUTE = "my.attribute";
    private static final String EXCHANGE_NAME = "myExchange";
    private static final String ROUTING_KEY = "myRoutingKey";
    
    private static final byte[] TEST_ATTACHMENT_CONTENT = "Test attachment content".getBytes(Charsets.UTF_8);

    public SwarmGenericContainer rabbitMqContainer = new SwarmGenericContainer(Images.RABBITMQ)
            .withAffinityToContainer();

    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    public AmqpRule amqpRule = new AmqpRule(rabbitMqContainer, EXCHANGE_NAME, ROUTING_KEY);

    @Rule
    public final RuleChain chain = RuleChain.outerRule(temporaryFolder).around(rabbitMqContainer).around(amqpRule);
    
    private TemporaryJamesServer jamesServer;
    private ConditionFactory calmlyAwait;

    @Before
    public void setup() throws Exception {
        MailetContainer mailetContainer = MailetContainer.builder()
            .postmaster("postmaster@" + JAMES_APACHE_ORG)
            .threads(5)
            .addProcessor(CommonProcessors.root())
            .addProcessor(CommonProcessors.error())
            .addProcessor(ProcessorConfiguration.builder()
                    .state("transport")
                    .enableJmx(true)
                    .addMailet(MailetConfiguration.builder()
                            .matcher(All.class)
                            .mailet(RemoveMimeHeader.class)
                            .addProperty("name", "bcc")
                            .build())
                    .addMailet(MailetConfiguration.builder()
                            .matcher(All.class)
                            .mailet(StripAttachment.class)
                            .addProperty(StripAttachment.ATTRIBUTE_PARAMETER_NAME, MAIL_ATTRIBUTE)
                            .addProperty(StripAttachment.PATTERN_PARAMETER_NAME, ".*\\.txt")
                            .build())
                    .addMailet(MailetConfiguration.builder()
                            .matcher(All.class)
                            .mailet(MimeDecodingMailet.class)
                            .addProperty(MimeDecodingMailet.ATTRIBUTE_PARAMETER_NAME, MAIL_ATTRIBUTE)
                            .build())
                    .addMailet(MailetConfiguration.builder()
                            .matcher(All.class)
                            .mailet(AmqpForwardAttribute.class)
                            .addProperty(AmqpForwardAttribute.URI_PARAMETER_NAME, amqpRule.getAmqpUri())
                            .addProperty(AmqpForwardAttribute.EXCHANGE_PARAMETER_NAME, EXCHANGE_NAME)
                            .addProperty(AmqpForwardAttribute.ATTRIBUTE_PARAMETER_NAME, MAIL_ATTRIBUTE)
                            .addProperty(AmqpForwardAttribute.ROUTING_KEY_PARAMETER_NAME, ROUTING_KEY)
                            .build())
                    .addMailet(MailetConfiguration.builder()
                            .matcher(RecipientIsLocal.class)
                            .mailet(VacationMailet.class)
                            .build())
                    .addMailet(MailetConfiguration.builder()
                            .matcher(RecipientIsLocal.class)
                            .mailet(LocalDelivery.class)
                            .build())
                    .build())
            .build();

        jamesServer = TemporaryJamesServer.builder().build(temporaryFolder, mailetContainer);
        Duration slowPacedPollInterval = Duration.FIVE_HUNDRED_MILLISECONDS;
        calmlyAwait = Awaitility.with()
            .pollInterval(slowPacedPollInterval)
            .and()
            .with()
            .pollDelay(slowPacedPollInterval)
            .await();

        DataProbe dataprobe = jamesServer.getProbe(DataProbeImpl.class);
        dataprobe.addDomain(JAMES_APACHE_ORG);
        dataprobe.addUser(FROM, PASSWORD);
        dataprobe.addUser(RECIPIENT, PASSWORD);
        jamesServer.getProbe(MailboxProbeImpl.class).createMailbox(MailboxConstants.USER_NAMESPACE, RECIPIENT, "INBOX");
    }

    @After
    public void tearDown() throws Exception {
        jamesServer.shutdown();
    }

    @Test
    public void stripAttachmentShouldPutAttachmentsInMailAttributeWhenConfiguredForIt() throws Exception {
        MimeMessage message = MimeMessageBuilder.mimeMessageBuilder()
            .setMultipartWithBodyParts(
                MimeMessageBuilder.bodyPartBuilder()
                    .data("simple text")
                    .build(),
                MimeMessageBuilder.bodyPartBuilder()
                    .data(TEST_ATTACHMENT_CONTENT)
                    .disposition("attachment")
                    .filename("test.txt")
                    .build())
            .setSubject("test")
            .build();

        Mail mail = FakeMail.builder()
              .mimeMessage(message)
              .sender(new MailAddress(FROM))
              .recipient(new MailAddress(RECIPIENT))
              .build();

        try (SMTPMessageSender messageSender = SMTPMessageSender.noAuthentication(LOCALHOST_IP, SMTP_PORT, JAMES_APACHE_ORG);
                IMAPMessageReader imapMessageReader = new IMAPMessageReader(LOCALHOST_IP, IMAP_PORT)) {
            messageSender.sendMessage(mail);
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(messageSender::messageHasBeenSent);
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> imapMessageReader.userReceivedMessage(RECIPIENT, PASSWORD));
        }

        assertThat(amqpRule.readContentAsBytes()).contains(TEST_ATTACHMENT_CONTENT);
    }

}
