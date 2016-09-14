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
package org.apache.james.transport.matchers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import javax.mail.MessagingException;

import org.apache.james.dnsservice.api.mock.MockDNSService;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMatcherConfig;
import org.junit.Before;
import org.junit.Test;

public class RemoteAddrInNetworkTest {
    private RemoteAddrInNetwork matcher;
    private FakeMail fakeMail;
    private MailAddress testRecipient;

    @Before
    public void setup() throws MessagingException {
        MockDNSService dnsServer = new MockDNSService() {
            @Override
            public InetAddress getByName(String host) throws UnknownHostException {
                return InetAddress.getByName(host);
            }
        };
        FakeMatcherConfig matcherConfig = new FakeMatcherConfig("AllowedNetworkIs=192.168.200.0/24", FakeMailContext.defaultContext());
        matcher = new RemoteAddrInNetwork();
        matcher.setDNSService(dnsServer);
        matcher.init(matcherConfig);
        testRecipient = new MailAddress("test@james.apache.org");
    }

    @Test
    public void shouldMatchWhenOnSameNetwork() throws MessagingException {
        fakeMail = FakeMail.builder()
                .recipient(testRecipient)
                .remoteAddr("192.168.200.1")
                .build();

        Collection<MailAddress> actual = matcher.match(fakeMail);

        assertThat(actual).containsOnly(testRecipient);
    }

    @Test
    public void shouldNotMatchWhenOnDifferentNetwork() throws MessagingException {
        fakeMail = FakeMail.builder()
                .recipient(testRecipient)
                .remoteAddr("192.168.1.1")
                .build();

        Collection<MailAddress> actual = matcher.match(fakeMail);

        assertThat(actual).isNull();
    }

    @Test
    public void shouldNotMatchWhenNoCondition() throws MessagingException {
        FakeMatcherConfig matcherConfig = new FakeMatcherConfig("", FakeMailContext.defaultContext());
        RemoteAddrInNetwork testee = new RemoteAddrInNetwork();
        testee.init(matcherConfig);

        fakeMail = FakeMail.builder()
                .recipient(testRecipient)
                .build();

        Collection<MailAddress> actual = testee.match(fakeMail);

        assertThat(actual).isNull();
    }

    @Test
    public void shouldNotMatchWhenInvalidAddress() throws MessagingException {
        fakeMail = FakeMail.builder()
                .recipient(testRecipient)
                .remoteAddr("invalid")
                .build();

        Collection<MailAddress> actual = matcher.match(fakeMail);

        assertThat(actual).isNull();
    }
}
