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

import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.dnsservice.library.inetnetwork.InetNetworkBuilder;
import org.apache.james.dnsservice.library.inetnetwork.model.InetNetwork;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetMatcher {
    private static final String NETS_SEPARATOR = ", ";
    private static final Logger LOG = LoggerFactory.getLogger(NetMatcher.class);
    private static final Comparator<InetNetwork> INET_NETWORK_COMPARATOR = new Comparator<InetNetwork>() {
        public int compare(InetNetwork in1, InetNetwork in2) {
            return in1.toString().compareTo(in2.toString());
        }
    };
    private static final Predicate<Optional<InetNetwork>> IS_PRESENT_INET_NETWORK = new Predicate<Optional<InetNetwork>>() {
        @Override
        public boolean apply(Optional<InetNetwork> inetNetworkOptional) {
            return inetNetworkOptional.isPresent();
        }
    };
    private static final Function<Optional<InetNetwork>, InetNetwork> GET_INET_NETWORK = new Function<Optional<InetNetwork>, InetNetwork>() {
        @Override
        public InetNetwork apply(Optional<InetNetwork> inetNetworkOptional) {
            return inetNetworkOptional.get();
        }
    };

    private final DNSService dnsServer;
    private final SortedSet<InetNetwork> networks;
    private final InetNetworkBuilder inetNetwork;
    private final Collection<String> nets;

    public NetMatcher(Collection<String> nets, DNSService dnsServer) {
        this.nets = nets;
        this.dnsServer = dnsServer;
        this.inetNetwork = new InetNetworkBuilder(this.dnsServer);
        this.networks = initInetNetworks();
    }

    public NetMatcher(String commaSeparatedNets, DNSService dnsServer) {
        this(Splitter.on(NETS_SEPARATOR).splitToList(commaSeparatedNets), dnsServer);
    }

    public boolean matchInetNetwork(String hostIP) {
        try {
            return FluentIterable.from(networks)
                    .anyMatch(inetContainIp(dnsServer.getByName(hostIP)));
        } catch (UnknownHostException uhe) {
            LOG.error("Cannot resolve address for IP '{}'", hostIP, uhe);
            return false;
        }
    }

    private SortedSet<InetNetwork> initInetNetworks() {
        return FluentIterable.from(nets)
                .transform(getInetFromString())
                .filter(IS_PRESENT_INET_NETWORK)
                .transform(GET_INET_NETWORK)
                .toSortedSet(INET_NETWORK_COMPARATOR);
    }

    private Predicate<InetNetwork> inetContainIp(final InetAddress ip) {
        return new Predicate<InetNetwork>() {
            @Override
            public boolean apply(InetNetwork inetNetwork) {
                return inetNetwork.contains(ip);
            }
        };
    }

    private Function<String, Optional<InetNetwork>> getInetFromString() {
        return new Function<String, Optional<InetNetwork>>() {
            @Override
            public Optional<InetNetwork> apply(String net) {
                try {
                    return Optional.of(inetNetwork.getFromString(net));
                } catch (UnknownHostException uhe) {
                    LOG.error("Cannot resolve address", uhe);
                    return Optional.absent();
                }
            }
        };
    }

    @VisibleForTesting
    SortedSet<InetNetwork> getNetworks() {
        return networks;
    }
}
