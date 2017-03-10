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

package org.apache.james.webadmin.service;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.Mapping;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.model.User;
import org.apache.james.util.streams.Iterators;
import org.apache.james.webadmin.dto.DomainMappingResponse;
import org.apache.james.webadmin.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DomainMappingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainMappingService.class);
    public static final String DOMAIN_KEY_PREFIX = "*@";
    public static final String DOMAIN_VALUE_PREFIX = "domain:";

    private final RecipientRewriteTable recipientRewriteTable;

    @Inject
    public DomainMappingService(RecipientRewriteTable recipientRewriteTable) {
        this.recipientRewriteTable = recipientRewriteTable;
    }

    public Map<String, Collection<String>> getMappings() throws RecipientRewriteTableException {
        return Optional.ofNullable(recipientRewriteTable.getAllMappings())
            .orElse(ImmutableMap.of())
            .entrySet()
            .stream()
            .map(entry -> Pair.of(entry.getKey(), entry.getValue().select(Mapping.Type.Domain)))
            .filter(entry -> entry.getValue().size() == 1)
            .map(entry -> Pair.of(entry.getKey(), Iterables.getFirst(entry.getValue().asStrings(), "")))
            .filter(entry -> entry.getKey().startsWith(DOMAIN_KEY_PREFIX)
                && entry.getValue().startsWith(DOMAIN_VALUE_PREFIX))
            .map(entry -> Pair.of(entry.getKey().substring(DOMAIN_KEY_PREFIX.length()),
                entry.getValue().substring(DOMAIN_VALUE_PREFIX.length())))
            .collect(Guavate.toImmutableListMultimap(Pair::getValue, Pair::getKey))
            .asMap();
    }

}
