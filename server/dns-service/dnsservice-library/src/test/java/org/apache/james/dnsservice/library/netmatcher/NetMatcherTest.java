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

import static org.assertj.core.api.Assertions.assertThat;
import static org.apache.james.dnsservice.api.mock.DNSFixture.LOCALHOST_IP_V4_ADDRESSES_DUPLICATE;
import static org.apache.james.dnsservice.api.mock.DNSFixture.LOCALHOST_IP_V6_ADDRESSES_DUPLICATE;
import static org.apache.james.dnsservice.api.mock.DNSFixture.LOCALHOST_IP_V4_ADDRESSES;
import static org.apache.james.dnsservice.api.mock.DNSFixture.LOCALHOST_IP_V6_ADDRESSES;
import static org.apache.james.dnsservice.api.mock.DNSFixture.DNS_SERVER_IPV4_MOCK;
import static org.apache.james.dnsservice.api.mock.DNSFixture.DNS_SERVER_IPV6_MOCK;
import static org.apache.james.dnsservice.api.mock.DNSFixture.HOST_CANNOT_LOOKUP_BY_NAME;

import org.apache.james.dnsservice.library.inetnetwork.InetNetworkUtil;

import java.net.UnknownHostException;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class NetMatcherTest {

    private static final String IP_V4_1 = "172.16.0.0/255.255.0.0";
    private static final String IP_V4_2 = "192.168.1.0/255.255.255.0";
    private static final String IP_V6_1 = "0:0:0:0:0:0:0:1/32768";
    private static final String IP_V6_2 = "2781:db8:1234:0:0:0:0:0/48";
    private static final String IP_V4_IPs = IP_V4_1 + ", " + IP_V4_2;
    private static final InetNetworkUtil inetNetworkV4Builder = new InetNetworkUtil(DNS_SERVER_IPV4_MOCK);
    private static final InetNetworkUtil inetNetworkV6Builder = new InetNetworkUtil(DNS_SERVER_IPV6_MOCK);

    private NetMatcher netMatcher;

    @Test
    public void matchInetNetworkShouldNotMatchWhenUnknownHostException() throws Exception {
        netMatcher = new NetMatcher(LOCALHOST_IP_V4_ADDRESSES, DNS_SERVER_IPV4_MOCK);

        assertThat(netMatcher.matchInetNetwork(HOST_CANNOT_LOOKUP_BY_NAME)).isFalse();
    }

    @Test
    public void matchInetNetworkShouldMatchWhenIpV4() throws UnknownHostException {
        netMatcher = new NetMatcher(LOCALHOST_IP_V4_ADDRESSES, DNS_SERVER_IPV4_MOCK);

        assertThat(netMatcher.matchInetNetwork("127.0.0.1")).isTrue();
        assertThat(netMatcher.matchInetNetwork("localhost")).isTrue();
        assertThat(netMatcher.matchInetNetwork("172.16.15.254")).isTrue();
        assertThat(netMatcher.matchInetNetwork("192.168.1.254")).isTrue();
    }

    @Test
    public void matchInetNetworkShouldNotMatchWhenIpV4OutOfTheRange() throws UnknownHostException {
        netMatcher = new NetMatcher(LOCALHOST_IP_V4_ADDRESSES, DNS_SERVER_IPV4_MOCK);

        assertThat(netMatcher.matchInetNetwork("192.169.1.254")).isFalse();
    }

    @Test
    public void matchInetNetworkShouldNotMatchIpV6WhenIpV4() throws UnknownHostException {
        netMatcher = new NetMatcher(LOCALHOST_IP_V4_ADDRESSES, DNS_SERVER_IPV4_MOCK);

        assertThat(netMatcher.matchInetNetwork("0:0:0:0:0:0:0:1%0")).isFalse();
    }

    @Test
    public void matchInetNetworkShouldMatchWhenIpV6() throws UnknownHostException {
        netMatcher = new NetMatcher(LOCALHOST_IP_V6_ADDRESSES, DNS_SERVER_IPV6_MOCK);

        assertThat(netMatcher.matchInetNetwork("0:0:0:0:0:0:0:1%0")).isTrue();
        assertThat(netMatcher.matchInetNetwork("00:00:00:00:00:00:00:1")).isTrue();
        assertThat(netMatcher.matchInetNetwork("2781:0db8:1234:8612:45ee:ffff:fffe:0001")).isTrue();
    }

    @Test
    public void matchInetNetworkShouldNotMatchWhenIpV6OutOfTheRange() throws UnknownHostException {
        netMatcher = new NetMatcher(LOCALHOST_IP_V6_ADDRESSES, DNS_SERVER_IPV6_MOCK);

        assertThat(netMatcher.matchInetNetwork("00:00:00:00:00:00:00:2")).isFalse();
        assertThat(netMatcher.matchInetNetwork("2781:0db8:1235:8612:45ee:ffff:fffe:0001")).isFalse();
    }

    @Test
    public void matchInetNetworkShouldNotMatchIpV4WhenIpV6() throws UnknownHostException {
        netMatcher = new NetMatcher(LOCALHOST_IP_V6_ADDRESSES, DNS_SERVER_IPV6_MOCK);

        assertThat(netMatcher.matchInetNetwork("172.16.15.254")).isFalse();
    }

    @Test
    public void getNetworksShouldNotContainInetNetworkWhenUnknownHostException() {
        netMatcher = new NetMatcher(ImmutableList.<String>of(HOST_CANNOT_LOOKUP_BY_NAME), DNS_SERVER_IPV6_MOCK);

        assertThat(netMatcher.getNetworks()).isEmpty();
    }
    @Test
    public void getNetworksShouldInitByString() throws Exception {
        netMatcher = new NetMatcher(IP_V4_IPs, DNS_SERVER_IPV4_MOCK);

        assertThat(netMatcher.getNetworks())
                .hasSize(2)
                .containsSequence(inetNetworkV4Builder.getFromString(IP_V4_1), inetNetworkV4Builder.getFromString(IP_V4_2));
    }

    @Test
    public void getNetworksShouldRemoveDuplicateWhenIp4() throws Exception {
        netMatcher = new NetMatcher(LOCALHOST_IP_V4_ADDRESSES_DUPLICATE, DNS_SERVER_IPV4_MOCK);

        assertThat(netMatcher.getNetworks())
                .hasSize(2)
                .containsSequence(inetNetworkV4Builder.getFromString(IP_V4_1), inetNetworkV4Builder.getFromString(IP_V4_2));
    }

    @Test
    public void getNetworksShouldRemoveDuplicateWhenIp6() throws Exception {
        netMatcher = new NetMatcher(LOCALHOST_IP_V6_ADDRESSES_DUPLICATE, DNS_SERVER_IPV6_MOCK);

        assertThat(netMatcher.getNetworks())
                .hasSize(2)
                .containsSequence(inetNetworkV6Builder.getFromString(IP_V6_1), inetNetworkV6Builder.getFromString(IP_V6_2));
    }
}
