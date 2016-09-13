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

import java.io.ByteArrayInputStream;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Charsets;

public class ReplaceContentTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ReplaceContent mailet;
    private FakeMailetConfig mailetConfig;

    @Before
    public void setup() {
        mailet = new ReplaceContent();
        mailetConfig = new FakeMailetConfig("Test", FakeMailContext.defaultContext());
    }

    @Test
    public void getMailetInfoShouldReturnValue() {
        assertThat(mailet.getMailetInfo()).isEqualTo("ReplaceContent");
    }

    @Test
    public void serviceShouldReplaceSubjectWhenMatching() throws Exception {
        mailetConfig.setProperty("subjectPattern", "/test/TEST/i/,/o/a//,/s/s/i/");
        mailet.init(mailetConfig);

        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setSubject("one test");

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();
        mailet.service(mail);

        assertThat(mail.getMessage().getSubject()).isEqualTo("ane TEsT");
    }

    @Test
    public void serviceShouldReplaceBodyWhenMatching() throws Exception {
        mailetConfig.setProperty("bodyPattern", 
                "/test/TEST/i/," +
                "/o/a/r/," +
                "/S/s/r/,/\\u00E8/e'//," +
                "/test([^\\/]*?)bla/X$1Y/im/," +
                "/X(.\\n)Y/P$1Q//," +
                "/\\/\\/,//");
        mailet.init(mailetConfig);

        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setText("This is one simple test/ \u00E8 one simple test.\n"
                + "Blo blo blo blo.\n");

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();
        mailet.service(mail);

        assertThat(mail.getMessage().getContent()).isEqualTo("This is ane simple TEsT, e' ane simple P.\n"
                + "Q bla bla bla.\n");
    }

    @Test
    public void serviceShouldNotLoopWhenCaseInsensitiveAndRepeat() throws Exception {
        mailetConfig.setProperty("bodyPattern", "/a/a/ir/");
        mailet.init(mailetConfig);

        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setText("aaa");

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();
        mailet.service(mail);

        assertThat(mail.getMessage().getContent()).isEqualTo("aaa");
    }

    @Test
    public void serviceShouldReplaceSubjectWhenConfigurationFromFile() throws Exception {
        mailetConfig.setProperty("subjectPatternFile", "#/org/apache/james/mailet/standard/mailets/replaceSubject.patterns");
        mailet.init(mailetConfig);

        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setSubject("re: r:ri:one test");

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();
        mailet.service(mail);

        assertThat(mail.getMessage().getSubject()).isEqualTo("Re: Re: Re: one test");
    }

    @Test
    public void serviceShouldRemoveOrAddTextInBody() throws Exception {
        mailetConfig.setProperty("bodyPattern", "/--original message--/<quote>/i/,"
                + "/<quote>(.*)(\\r\\n)([^>]+)/<quote>$1$2>$3/imrs/,"
                + "/<quote>\\r\\n//im/");
        mailet.init(mailetConfig);

        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setText("Test.\r\n" + "\r\n" + "--original message--\r\n"
                + "part of\r\n" + "message\\ that\\0 must0 be\r\n"
                + "quoted. Let's see if\r\n" + "he can do it.");

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();
        mailet.service(mail);

        assertThat(mail.getMessage().getContent()).isEqualTo("Test.\r\n" + "\r\n" + ">part of\r\n"
                + ">message\\ that\\0 must0 be\r\n"
                + ">quoted. Let's see if\r\n" + ">he can do it.");
    }


    @Test
    public void serviceShouldReplaceBodyWhenMatchingASCIICharacter() throws Exception {
        mailetConfig.setProperty("bodyPattern", "/\\u2026/.../r/");
        mailet.init(mailetConfig);

        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setSubject("one test");
        message.setText("Replacement \u2026 one test \u2026");

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();
        mailet.service(mail);

        assertThat(mail.getMessage().getContent()).isEqualTo("Replacement ... one test ...");
    }

    @Test
    public void serviceShouldReplaceBodyWhenMatchingCharset() throws Exception {
        String messageSource = "Content-Type: text/plain; charset=\"iso-8859-1\"\r\n"
                + "Content-Transfer-Encoding: quoted-printable\r\n"
                + "\r\n"
                + "=93test=94 with th=92 apex";

        mailetConfig.setProperty("bodyPattern", "/[\\u2018\\u2019\\u201A]/'//,"
                + "/[\\u201C\\u201D\\u201E]/\"//," + "/[\\x91\\x92\\x82]/'//,"
                + "/[\\x93\\x94\\x84]/\"/r/," + "/\\x85/...//," + "/\\x8B/<//,"
                + "/\\x9B/>//," + "/\\x96/-//," + "/\\x97/--//,");
        mailet.init(mailetConfig);

        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()),
                new ByteArrayInputStream(messageSource.getBytes()));

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();
        mailet.service(mail);

        assertThat(mail.getMessage().getContent()).isEqualTo("\"test\" with th' apex");
    }

    @Test
    public void serviceShouldSetContenTypeWhenInitialized() throws Exception {
        mailetConfig.setProperty("subjectPattern", "/test/TEST/i/,/o/a//,/s/s/i/");
        mailetConfig.setProperty("charset", Charsets.UTF_8.name());
        mailet.init(mailetConfig);

        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setSubject("one test");
        message.setText("This is one simple test/ \u00E8 one simple test.\n"
                + "Blo blo blo blo.\n");

        Mail mail = FakeMail.builder()
                .mimeMessage(message)
                .build();
        mailet.service(mail);

        assertThat(mail.getMessage().getContentType()).isEqualTo("text/plain; charset=UTF-8");
    }
}
