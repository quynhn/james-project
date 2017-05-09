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

package org.apache.james.dnsservice.library.netmatcher;

import static org.apache.james.dnsservice.api.mock.DNSFixture.DNS_SERVER_MOCK;
import static org.apache.james.dnsservice.api.mock.DNSFixture.HOST_CANNOT_LOOKUP_BY_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.james.dnsservice.library.inetnetwork.model.Inet4Network;

import java.net.UnknownHostException;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NetMatcherFactoryTest {
    private Inet4Network inet4Network1;
    private Inet4Network inet4Network2;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        inet4Network1 = new Inet4Network(DNS_SERVER_MOCK.getByName("172.16.0.0"), DNS_SERVER_MOCK.getByName("255.255.0.0"));
        inet4Network2 = new Inet4Network(DNS_SERVER_MOCK.getByName("192.168.1.0"), DNS_SERVER_MOCK.getByName("255.255.255.0"));
    }

    @Test
    public void getNetMatcherShouldRemoveFailNetworkWhenCannotCreateInetNetworkFromString() {
        NetMatcher netMatcher = NetMatcherFactory.getNetMatcher(HOST_CANNOT_LOOKUP_BY_NAME, DNS_SERVER_MOCK);

        assertThat(netMatcher.getNetworks()).isEmpty();
    }

    @Test
    public void getNetMatcherShouldAcceptStringCommaSeparatedNets() throws Exception {
        NetMatcher netMatcher = NetMatcherFactory.getNetMatcher("172.16.0.0/255.255.0.0, 192.168.1.0/255.255.255.0", DNS_SERVER_MOCK);

        assertThat(netMatcher.getNetworks())
                .hasSize(2)
                .containsSequence(inet4Network1, inet4Network2);
    }

    @Test
    public void getNetMatcherShouldAcceptListOfNets() throws Exception {
        NetMatcher netMatcher = NetMatcherFactory.getNetMatcher(ImmutableList.of("172.16.0.0/255.255.0.0", "192.168.1.0/255.255.255.0"), DNS_SERVER_MOCK);

        assertThat(netMatcher.getNetworks())
                .hasSize(2)
                .containsSequence(inet4Network1, inet4Network2);
    }

}