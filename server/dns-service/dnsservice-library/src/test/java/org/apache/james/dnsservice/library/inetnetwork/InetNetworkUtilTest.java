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
package org.apache.james.dnsservice.library.inetnetwork;

import static org.apache.james.dnsservice.api.mock.DNSFixture.DNS_SERVER_MOCK;
import static org.apache.james.dnsservice.api.mock.DNSFixture.HOST_IP_V4;
import static org.apache.james.dnsservice.api.mock.DNSFixture.HOST_IP_V6;
import static org.apache.james.dnsservice.api.mock.DNSFixture.LOCALHOST_IP_V4_ADDRESS_0;
import static org.apache.james.dnsservice.api.mock.DNSFixture.LOCALHOST_IP_V4_ADDRESS_2;
import static org.apache.james.dnsservice.api.mock.DNSFixture.LOCALHOST_IP_V6_ADDRESS_0;
import static org.apache.james.dnsservice.api.mock.DNSFixture.LOCALHOST_IP_V6_ADDRESS_2;
import static org.apache.james.dnsservice.api.mock.DNSFixture.MASK_IP_V4;
import static org.assertj.core.api.Assertions.assertThat;
import org.apache.james.dnsservice.library.inetnetwork.model.Inet4Network;
import org.apache.james.dnsservice.library.inetnetwork.model.Inet6Network;
import org.apache.james.dnsservice.library.inetnetwork.model.InetNetwork;

import java.net.UnknownHostException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class InetNetworkUtilTest {

    private static final String IP_V4 = "127.0.0.0";
    private InetNetworkUtil inetNetworkUtil;
    private InetNetwork inetNetwork;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void getFromStringShouldCalculateMarkAndReturnIpV4() throws UnknownHostException {
        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);

        inetNetwork = inetNetworkUtil.getFromString(LOCALHOST_IP_V4_ADDRESS_0);
        assertThat(inetNetwork).isEqualTo(new Inet4Network(DNS_SERVER_MOCK.getByName(IP_V4), DNS_SERVER_MOCK.getByName("255.255.0.0")));
    }

    @Test
    public void getFromStringShouldThrownWhenMarkLengthIsNotNumber() throws UnknownHostException {
        expectedException.expect(UnknownHostException.class);

        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);
        inetNetwork = inetNetworkUtil.getFromString(IP_V4 + "/invalid");
    }

    @Test
    public void getFromStringShouldThrownWhenMarkLengthIsEmpty() throws UnknownHostException {
        expectedException.expect(UnknownHostException.class);

        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);
        inetNetwork = inetNetworkUtil.getFromString(IP_V4 + "/");

        assertThat(inetNetwork).isEqualTo(new Inet4Network(DNS_SERVER_MOCK.getByName(IP_V4), DNS_SERVER_MOCK.getByName("255.255.0.0")));
    }

    @Test
    public void getFromStringShouldReturnRootIpV4WhenZeroMarkLength() throws UnknownHostException {
        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);
        inetNetwork = inetNetworkUtil.getFromString(IP_V4 + "/0");

        assertThat(inetNetwork).isEqualTo(new Inet4Network(DNS_SERVER_MOCK.getByName("0.0.0.0"), DNS_SERVER_MOCK.getByName("0.0.0.0")));
    }

    @Test
    public void getFromStringShouldGetMaxMarkWhenNoMark() throws UnknownHostException {
        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);

        inetNetwork = inetNetworkUtil.getFromString(IP_V4);
        assertThat(inetNetwork).isEqualTo(new Inet4Network(DNS_SERVER_MOCK.getByName(IP_V4), DNS_SERVER_MOCK.getByName("255.255.255.255")));
    }

    @Test
    public void getFromStringShouldReturnIpV4() throws UnknownHostException {
        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);

        inetNetwork = inetNetworkUtil.getFromString(LOCALHOST_IP_V4_ADDRESS_2);
        assertThat(inetNetwork).isEqualTo(new Inet4Network(DNS_SERVER_MOCK.getByName(HOST_IP_V4), DNS_SERVER_MOCK.getByName(MASK_IP_V4)));
    }

    @Test
    public void getFromStringShouldReturnIpV6() throws UnknownHostException {
        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);

        inetNetwork = inetNetworkUtil.getFromString(LOCALHOST_IP_V6_ADDRESS_0);
        assertThat(inetNetwork).isEqualTo(new Inet6Network(DNS_SERVER_MOCK.getByName(LOCALHOST_IP_V6_ADDRESS_0), 32768));
    }

    @Test
    public void getFromStringShouldReturnIpV6WhenPercentageCharacterComparator() throws UnknownHostException {
        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);

        inetNetwork = inetNetworkUtil.getFromString(LOCALHOST_IP_V6_ADDRESS_2);
        assertThat(inetNetwork).isEqualTo(new Inet6Network(DNS_SERVER_MOCK.getByName(HOST_IP_V6), 48));
    }

    @Test
    public void isV6ShouldBeTrueWhenInetV6() throws UnknownHostException {
        assertThat(InetNetworkUtil.isV6(LOCALHOST_IP_V6_ADDRESS_0)).isTrue();
    }

    @Test
    public void isV6ShouldBeFalseWhenInetV4() throws UnknownHostException {
        assertThat(InetNetworkUtil.isV6(IP_V4)).isFalse();
    }

    @Test
    public void getFromStringShouldThrowWhenWrongIPV6() throws Exception {
        expectedException.expect(UnknownHostException.class);

        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);
        inetNetworkUtil.getFromString("00:00:00:00:00:00:00:00:1");
    }

    @Test
    public void getFromStringShouldThrowWhenIpV4WithWildcardAndTooMuchSignatureString() throws Exception {
        expectedException.expect(UnknownHostException.class);

        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);
        inetNetwork = inetNetworkUtil.getFromString("0.0.0.0.0.0/16*");
    }

    @Test
    public void getFromStringShouldThrowWhenInvalidIpV4WithWildcard() throws Exception {
        expectedException.expect(UnknownHostException.class);

        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);
        inetNetwork = inetNetworkUtil.getFromString("0.0.0.0/16*");
    }

    @Test
    public void getFromStringShouldReturnWhenIpV4WithWildcard() throws Exception {
        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);
        inetNetwork = inetNetworkUtil.getFromString("182.168.*");

        assertThat(inetNetwork).isEqualTo(new Inet4Network(DNS_SERVER_MOCK.getByName("182.168.0.0"), DNS_SERVER_MOCK.getByName("255.255.0.0")));
    }

    @Test
    public void getFromStringShouldStandardHostIpAndReturnWhenIpV4WithWildcard() throws Exception {
        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);
        inetNetwork = inetNetworkUtil.getFromString("182.168*");

        assertThat(inetNetwork).isEqualTo(new Inet4Network(DNS_SERVER_MOCK.getByName("182.168.0.0"), DNS_SERVER_MOCK.getByName("255.255.0.0")));
    }

    @Test
    public void getFromStringShouldThrowWhenWrongIPV4() throws Exception {
        expectedException.expect(UnknownHostException.class);

        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);
        inetNetworkUtil.getFromString("0.0.0.0.0");
    }


    @Test
    public void getFromStringShouldThrowWhenIpV6WithWildcard() throws Exception {
        expectedException.expect(UnsupportedOperationException.class);

        inetNetworkUtil = new InetNetworkUtil(DNS_SERVER_MOCK);
        inetNetwork = inetNetworkUtil.getFromString("0:0:0:0:0:0/16*");
    }
}
