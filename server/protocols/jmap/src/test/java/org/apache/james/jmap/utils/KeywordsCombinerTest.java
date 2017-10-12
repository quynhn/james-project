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

package org.apache.james.jmap.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.jmap.model.Keyword;
import org.apache.james.jmap.model.Keywords;
import org.apache.james.util.CommutativityChecker;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class KeywordsCombinerTest {

    public static final Keywords.KeywordsFactory FACTORY = Keywords.factory();

    @Test
    public void keywordsCombinerShouldBeCommutative() {
        assertThat(new CommutativityChecker<>(
            ImmutableSet.of(
                FACTORY.from(Keyword.ANSWERED),
                FACTORY.from(Keyword.DELETED),
                FACTORY.from(Keyword.DRAFT),
                FACTORY.from(Keyword.FLAGGED),
                FACTORY.from(Keyword.SEEN),
                FACTORY.from(),
                FACTORY.from(new Keyword("$Forwarded")),
                FACTORY.from(new Keyword("$Any")),
                FACTORY.from(Keyword.ANSWERED, Keyword.DELETED, Keyword.DRAFT, Keyword.FLAGGED, Keyword.SEEN, new Keyword("$Forwarded"), new Keyword("$Any"))),
            new KeywordsCombiner())
            .findNonCommutativeInput())
            .isEmpty();
    }

    // TODO more unit tests

}