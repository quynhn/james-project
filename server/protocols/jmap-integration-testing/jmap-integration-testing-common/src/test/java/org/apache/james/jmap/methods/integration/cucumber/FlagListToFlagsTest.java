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

package org.apache.james.jmap.methods.integration.cucumber;

import static org.assertj.core.api.Assertions.assertThat;

import javax.mail.Flags;

import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

public class FlagListToFlagsTest {
    @Test
    public void fromFlagListShouldConvertAnwseredFlag() throws Exception {
        assertThat(FlagListToFlags.fromFlagList(ImmutableList.of("$Answered")))
            .isEqualTo(new Flags(Flags.Flag.ANSWERED));
    }

    @Test
    public void fromFlagListShouldConvertSeenFlag() throws Exception {
        assertThat(FlagListToFlags.fromFlagList(ImmutableList.of("$Seen")))
            .isEqualTo(new Flags(Flags.Flag.SEEN));
    }

    @Test
    public void fromFlagListShouldConvertDraftFlag() throws Exception {
        assertThat(FlagListToFlags.fromFlagList(ImmutableList.of("$Draft")))
            .isEqualTo(new Flags(Flags.Flag.DRAFT));
    }

    @Test
    public void fromFlagListShouldConvertRecentFlag() throws Exception {
        assertThat(FlagListToFlags.fromFlagList(ImmutableList.of("$Recent")))
            .isEqualTo(new Flags(Flags.Flag.RECENT));
    }

    @Test
    public void fromFlagListShouldConvertDeletedFlag() throws Exception {
        assertThat(FlagListToFlags.fromFlagList(ImmutableList.of("$Deleted")))
            .isEqualTo(new Flags(Flags.Flag.DELETED));
    }

    @Test
    public void fromFlagListShouldConvertFlaggedFlag() throws Exception {
        assertThat(FlagListToFlags.fromFlagList(ImmutableList.of("$Flagged")))
            .isEqualTo(new Flags(Flags.Flag.FLAGGED));
    }

    @Test
    public void fromFlagListShouldConvertCustomUserFlag() throws Exception {
        assertThat(FlagListToFlags.fromFlagList(ImmutableList.of("op§")))
            .isEqualTo(new Flags("op§"));
    }

}