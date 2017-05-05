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

import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.dnsservice.library.inetnetwork.model.Inet4Network;
import org.apache.james.dnsservice.library.inetnetwork.model.Inet6Network;
import org.apache.james.dnsservice.library.inetnetwork.model.InetNetwork;
import org.apache.commons.lang.StringUtils;

import java.net.UnknownHostException;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

/**
 * <p>
 * Builds a InetNetwork (Inet4Network or Inet6Network) in function on the
 * provided string pattern that represents a subnet.
 * </p>
 * 
 * <p>
 * Inet4Network is constructed based on the IPv4 subnet expressed in one of
 * several formats:
 * 
 * <pre>
 *     IPv4 Format                     Example
 *     Explicit address                127.0.0.1
 *     Address with a wildcard         127.0.0.*
 *     Domain name                     myHost.com
 *     Domain name + prefix-length     myHost.com/24
 *     Domain name + mask              myHost.com/255.255.255.0
 *     IP address + prefix-length      127.0.0.0/8
 *     IP + mask                       127.0.0.0/255.0.0.0
 * </pre>
 * 
 * For more information on IP V4, see RFC 1518 and RFC 1519.
 * </p>
 * 
 * <p>
 * Inet6Network is constructed based on the IPv4 subnet expressed in one of
 * several formats:
 * 
 * <pre>
 *     IPv6 Format                     Example
 *     Explicit address                0000:0000:0000:0000:0000:0000:0000:0001
 *     IP address + subnet mask (/)   0000:0000:0000:0000:0000:0000:0000:0001/64
 *     IP address + subnet mask (%)   0000:0000:0000:0000:0000:0000:0000:0001%64
 *     The following V6 formats will be supported later:
 *     Domain name                     myHost.com
 *     Domain name + mask (/)          myHost.com/48
 *     Domain name + mask (%)          myHost.com%48
 *     Explicit shorted address        ::1
 * </pre>
 * 
 * For more information on IP V6, see RFC 2460. (See also <a
 * href="http://en.wikipedia.org/wiki/IPv6_address"
 * >http://en.wikipedia.org/wiki/IPv6_address</a>)
 * </p>
 */
public class InetNetworkUtil {
    private static final String WILDCARD = "*";
    private static final String IP_V4_SIGNATURE = ".";
    private static final String IP_V6_SIGNATURE = ":";
    private static final String IP_AND_NETMARK_SEPARATOR = "/";
    private static final String IP_AND_NETMARK_SEPARATOR_ANOTHER_ON_V6 = "%";
    private static final List<String> FULL_RANGE_IP4 = ImmutableList.of("0.0.0.0/0.0.0.0", "0.0.0/255.0.0.0", "0.0/255.255.0.0", "0/255.255.255.0");
    private static final String MAX_NET_MASK_V4 = "/255.255.255.255";
    private static final String MAX_NET_MASK_V6 = "/32768";
    private static final List<Integer> MAST_PARTS = ImmutableList.of(24, 16, 8, 1);
    private static final int DECIMAL_RADIX = 10;

    private final DNSService dnsService;

    public InetNetworkUtil(DNSService dnsServer) {
        this.dnsService = dnsServer;
    }

    public InetNetwork getFromString(String netspec) throws UnknownHostException {
        if (isV6(netspec)) {
           return getV6FromString(netspec);
        }

        return getV4FromString(netspec);
    }

    public static boolean isV6(String netspec) {
        return netspec.contains(IP_V6_SIGNATURE);
    }

    private InetNetwork getV4FromString(String netspec) throws UnknownHostException {
        if (StringUtils.endsWith(netspec, WILDCARD)) {
            netspec = normalizeV4FromAsterisk(netspec);
        } else {
            netspec = standardIpWithNetmarkSeparator(netspec, MAX_NET_MASK_V4);
            String host = StringUtils.substringBefore(netspec, IP_AND_NETMARK_SEPARATOR);
            String mask = StringUtils.substringAfter(netspec, IP_AND_NETMARK_SEPARATOR);
            if (StringUtils.containsNone(mask, IP_V4_SIGNATURE)) {
                netspec = normalizeV4FromCIDR(host, mask);
            }
        }

        return getInetNetwork(netspec, false);
    }

    private InetNetwork getV6FromString(String netspec) throws UnknownHostException {
        if (StringUtils.endsWith(netspec, WILDCARD)) {
            throw new UnsupportedOperationException("Wildcard for IPv6 not supported");
        }

        netspec = StringUtils.replace(netspec, IP_AND_NETMARK_SEPARATOR_ANOTHER_ON_V6, IP_AND_NETMARK_SEPARATOR);

        return getInetNetwork(standardIpWithNetmarkSeparator(netspec, MAX_NET_MASK_V6), true);
    }

    private InetNetwork getInetNetwork(String netspec, boolean isV6) throws UnknownHostException {
        String[] hostAndMask = StringUtils.split(netspec, IP_AND_NETMARK_SEPARATOR);
        if (isV6) {
            return new Inet6Network(dnsService.getByName(hostAndMask[0]), Integer.parseInt(hostAndMask[1]));
        }
        return new Inet4Network(dnsService.getByName(hostAndMask[0]), dnsService.getByName(hostAndMask[1]));
    }

    private String standardIpWithNetmarkSeparator(String netspec, String maxNetMask) {
        if (StringUtils.containsNone(netspec, IP_AND_NETMARK_SEPARATOR)) {
            return netspec + maxNetMask;
        }
        return netspec;
    }

    private String normalizeV4FromAsterisk(String netspec) throws UnknownHostException {
        netspec = standardIpWhenWildcard(netspec);

        int octets = StringUtils.countMatches(netspec, IP_V4_SIGNATURE);

        if (octets >= FULL_RANGE_IP4.size()) {
            throw new UnknownHostException();
        }
        return netspec + FULL_RANGE_IP4.get(octets);
    }

    private String standardIpWhenWildcard(String netspec) {
        netspec = StringUtils.substringBefore(netspec, WILDCARD);
        if (!StringUtils.endsWith(netspec, IP_V4_SIGNATURE)) {
            netspec += IP_V4_SIGNATURE;
        }
        return netspec;
    }

    private String normalizeV4FromCIDR(String host, String markLength) throws UnknownHostException {
        return host + IP_AND_NETMARK_SEPARATOR + getNetMask(getMask(markLength));
    }

    private static int getMask(String markLength) throws UnknownHostException {
        int bits = 0;
        try {
            bits = 32 - Integer.parseInt(markLength);
        } catch (NumberFormatException e) {
            throw new UnknownHostException(markLength);
        }
        if (bits == 32) {
            return 0;
        }

        return 0xFFFFFFFF - ((1 << bits) - 1);
    }

    private String getNetMask(int mask) {
        return FluentIterable.from(MAST_PARTS)
                .transform(getNetMarkPart(mask))
                .join(Joiner.on(IP_V4_SIGNATURE));
    }

    private Function<Integer, String> getNetMarkPart(final int mask) {
        return new Function<Integer, String>() {
            @Override
            public String apply(Integer part) {
                return Integer.toString(mask >> part & 0xFF, DECIMAL_RADIX);
            }
        };
    }
}
